const fs = require('fs');
const path = require('path');

const glbPath = path.join(__dirname, '..', 'frontend', 'public', 'models-optimized', 'characters', 'lillie__anniversary_50.glb');

function run() {
  if (!fs.existsSync(glbPath)) {
    console.error('File not found:', glbPath);
    return;
  }
  
  const fd = fs.openSync(glbPath, 'r');
  const header = Buffer.alloc(20);
  fs.readSync(fd, header, 0, 20, 0);
  
  const chunkLength = header.readUInt32LE(12);
  const chunkType = header.readUInt32LE(16);
  
  console.log('Chunk length:', chunkLength);
  console.log('Chunk type:', chunkType.toString(16), '(expected 4e4f534a)');
  
  const jsonBuffer = Buffer.alloc(chunkLength);
  fs.readSync(fd, jsonBuffer, 0, chunkLength, 20);
  fs.closeSync(fd);
  
  const gltfJson = JSON.parse(jsonBuffer.toString('utf8'));
  console.log('Total nodes:', gltfJson.nodes.length);
  
  const bones = [];
  const allNodeNames = [];
  for (const node of gltfJson.nodes) {
    allNodeNames.push(node.name);
    const lowerName = (node.name || '').toLowerCase();
    if (lowerName.includes('arm') || lowerName.includes('hand') || lowerName.includes('shoulder') || lowerName.includes('forearm')) {
      bones.push(node.name);
    }
  }
  
  console.log('\nNodes matching arm/hand/shoulder/forearm:');
  console.log(bones);
  
  console.log('\nFirst 50 node names in the file:');
  console.log(allNodeNames.slice(0, 50));
}

run();
