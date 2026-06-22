const fs = require('fs');
const path = require('path');

const ASSETS_DIR = path.join(__dirname, 'src', 'assets');
const OUTPUT_FILE = path.join(ASSETS_DIR, 'playlist.json');

const AUDIO_EXTENSIONS = ['.mp3', '.wav', '.ogg', '.m4a', '.aac', '.flac'];
const IGNORED_DIRS = ['models']; // Skip heavy 3D assets to speed up scan

// Format seconds into m:ss format
function formatDuration(secs) {
  if (isNaN(secs) || secs <= 0) return '3:00';
  const m = Math.floor(secs / 60);
  const s = Math.floor(secs % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

// Lightweight MP3 duration reader
function getMp3Duration(filePath) {
  try {
    const fd = fs.openSync(filePath, 'r');
    const stat = fs.fstatSync(fd);
    const fileSize = stat.size;
    
    // Read first 128KB to find ID3v2 and first audio frame
    const bufferSize = Math.min(128 * 1024, fileSize);
    const buffer = Buffer.alloc(bufferSize);
    fs.readSync(fd, buffer, 0, bufferSize, 0);
    fs.closeSync(fd);
    
    let offset = 0;
    
    // Check ID3v2 tag
    if (buffer.length >= 10 && buffer.toString('utf8', 0, 3) === 'ID3') {
      // ID3v2 header is 10 bytes. Size is at offset 6 (4 bytes synchsafe integer)
      const id3Size = ((buffer[6] & 0x7F) << 21) |
                      ((buffer[7] & 0x7F) << 14) |
                      ((buffer[8] & 0x7F) << 7) |
                      (buffer[9] & 0x7F);
      offset = 10 + id3Size;
    }
    
    // Search for first MPEG audio frame sync (11 bits of 1s: 0xFF and top 3 bits of next byte)
    let frameHeaderOffset = -1;
    for (let i = offset; i < buffer.length - 4; i++) {
      if (buffer[i] === 0xFF && (buffer[i + 1] & 0xE0) === 0xE0) {
        frameHeaderOffset = i;
        break;
      }
    }
    
    if (frameHeaderOffset === -1) {
      // Fallback: estimate at 192kbps
      const durationSeconds = Math.round((fileSize - offset) / (192 * 1000 / 8));
      return formatDuration(durationSeconds);
    }
    
    // Decode MPEG header
    const byte1 = buffer[frameHeaderOffset + 1];
    const byte2 = buffer[frameHeaderOffset + 2];
    
    const mpegVersion = (byte1 & 0x18) >> 3; // 3 = v1, 2 = v2, 0 = v2.5
    const layer = (byte1 & 0x06) >> 1; // 1 = Layer III, 2 = Layer II, 3 = Layer I
    const bitrateIndex = (byte2 & 0xF0) >> 4;
    
    // Bitrate table for MP3 (MPEG 1 Layer III)
    let bitrates = [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0];
    if (mpegVersion !== 3) {
      // MPEG 2 or 2.5 Layer III
      bitrates = [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0];
    }
    const bitrate = bitrates[bitrateIndex] || 192; // Default to 192 if unknown
    
    const audioSize = fileSize - offset;
    const durationSeconds = Math.round(audioSize / (bitrate * 1000 / 8));
    return formatDuration(durationSeconds);
  } catch (e) {
    return '3:00';
  }
}

// Lightweight WAV duration reader
function getWavDuration(filePath) {
  try {
    const fd = fs.openSync(filePath, 'r');
    const buffer = Buffer.alloc(44);
    fs.readSync(fd, buffer, 0, 44, 0);
    fs.closeSync(fd);
    
    const isWav = buffer.toString('utf8', 0, 4) === 'RIFF' && buffer.toString('utf8', 8, 12) === 'WAVE';
    if (!isWav) return '3:00';
    
    const byteRate = buffer.readUInt32LE(28);
    const dataSize = buffer.readUInt32LE(40);
    
    if (byteRate > 0 && dataSize > 0) {
      return formatDuration(Math.round(dataSize / byteRate));
    }
    return '3:00';
  } catch (e) {
    return '3:00';
  }
}

function getAudioDuration(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  if (ext === '.mp3') return getMp3Duration(filePath);
  if (ext === '.wav') return getWavDuration(filePath);
  return '3:00';
}

function formatName(name) {
  return name
    .replace(/[_-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, c => c.toUpperCase());
}

function parseSongInfo(filename) {
  const nameWithoutExt = path.parse(filename).name;
  
  // Try split by ' - ' first, then by '-'
  let parts = nameWithoutExt.split(' - ');
  if (parts.length < 2) {
    parts = nameWithoutExt.split('-');
  }
  
  if (parts.length >= 2) {
    const artist = formatName(parts[0]);
    const title = formatName(parts.slice(1).join(' '));
    return { title, artist };
  }
  
  return {
    title: formatName(nameWithoutExt),
    artist: 'PokeUTN'
  };
}

function scanDir(dir, fileList = []) {
  if (!fs.existsSync(dir)) return fileList;
  const files = fs.readdirSync(dir);
  
  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      if (!IGNORED_DIRS.includes(file)) {
        scanDir(filePath, fileList);
      }
    } else {
      const ext = path.extname(file).toLowerCase();
      if (AUDIO_EXTENSIONS.includes(ext)) {
        fileList.push(filePath);
      }
    }
  }
  return fileList;
}

function run() {
  console.log('Escaneando carpeta de música para generar playlist...');
  
  if (!fs.existsSync(ASSETS_DIR)) {
    console.error(`Error: Directorio de assets no existe en ${ASSETS_DIR}`);
    process.exit(1);
  }
  
  const files = scanDir(ASSETS_DIR);
  const playlist = [];
  
  for (const filePath of files) {
    const relativePath = path.relative(ASSETS_DIR, filePath).replace(/\\/g, '/');
    const { title, artist } = parseSongInfo(path.basename(filePath));
    const duration = getAudioDuration(filePath);
    const url = `/assets/${relativePath}`;
    
    playlist.push({
      title,
      artist,
      duration,
      url
    });
    console.log(`- Mapeado: "${artist} - ${title}" [${duration}] en ${url}`);
  }
  
  fs.writeFileSync(OUTPUT_FILE, JSON.stringify(playlist, null, 2), 'utf8');
  console.log(`Playlist de música autogenerada con éxito en: ${OUTPUT_FILE}`);
}

run();
