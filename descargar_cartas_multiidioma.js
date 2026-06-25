const https = require('https');
const fs = require('fs');
const fsPromises = require('fs').promises;
const path = require('path');

const LANGUAGES = ['es', 'ja', 'pt'];
const TOTAL_CARDS = 146;
const BASE_DEST_DIR = path.join(__dirname, 'frontend', 'public', 'images', 'cards');

async function downloadImage(url, dest) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if (res.statusCode !== 200) {
        reject(new Error(`Status code: ${res.statusCode}`));
        return;
      }
      const file = fs.createWriteStream(dest);
      res.pipe(file);
      file.on('finish', () => {
        file.close(resolve);
      });
      file.on('error', (err) => {
        fsPromises.unlink(dest).catch(() => {});
        reject(err);
      });
    }).on('error', reject);
  });
}

async function main() {
  console.log('🚀 Iniciando descarga de imágenes multiidioma desde TCGdex...');
  
  for (const lang of LANGUAGES) {
    const langDir = path.join(BASE_DEST_DIR, lang);
    await fsPromises.mkdir(langDir, { recursive: true });
    console.log(`\n📁 Procesando idioma: [${lang.toUpperCase()}] en ${langDir}...`);
    
    let downloadedCount = 0;
    let skippedCount = 0;
    
    for (let i = 1; i <= TOTAL_CARDS; i++) {
      const cardId = `xy1-${i}`;
      const url = `https://assets.tcgdex.net/${lang}/xy/xy1/${i}/low.png`;
      const destPath = path.join(langDir, `${cardId}.png`);
      
      try {
        // Verificar si ya existe
        try {
          await fsPromises.access(destPath);
          skippedCount++;
          continue;
        } catch {
          // No existe
        }
        
        console.log(`📥 Descargando ${cardId} (${lang})...`);
        await downloadImage(url, destPath);
        downloadedCount++;
        // Delay de 50ms para ser respetuosos con el servidor
        await new Promise(r => setTimeout(r, 50));
      } catch (err) {
        console.log(`⚠️  Saltando ${cardId} (${lang}): no disponible o error (${err.message})`);
        skippedCount++;
      }
    }
    console.log(`✨ Resultados [${lang.toUpperCase()}]: Descargados: ${downloadedCount}, Saltados/Existentes: ${skippedCount}`);
  }
  
  console.log('\n✅ ¡Descarga de cartas completada con éxito!');
}

main();
