const fs = require('fs');
const path = require('path');

const modelsDir = path.join(__dirname, '..', 'frontend', 'public', 'models-optimized', 'characters');
const files = [
  'adaman_regular_00.glb'
];

function run() {
  for (const filename of files) {
    const glbPath = path.join(modelsDir, filename);
    if (!fs.existsSync(glbPath)) {
      console.log(`File not found: ${filename}`);
      continue;
    }
    
    console.log(`\n========================================`);
    console.log(`Inspecting: ${filename}`);
    
    const fd = fs.openSync(glbPath, 'r');
    const header = Buffer.alloc(20);
    fs.readSync(fd, header, 0, 20, 0);
    
    const chunkLength = header.readUInt32LE(12);
    const jsonBuffer = Buffer.alloc(chunkLength);
    fs.readSync(fd, jsonBuffer, 0, chunkLength, 20);
    fs.closeSync(fd);
    
    const gltfJson = JSON.parse(jsonBuffer.toString('utf8'));
    
    // 1. Get all nodes (joints)
    const bones = [];
    const allNodeNames = [];
    if (gltfJson.nodes) {
      for (const node of gltfJson.nodes) {
        if (!node.name) continue;
        allNodeNames.push(node.name);
        const lowerName = node.name.toLowerCase();
        if (lowerName.includes('arm') || lowerName.includes('hand') || lowerName.includes('forearm')) {
          bones.push(node.name);
        }
      }
    }
    
    console.log(`Bones (matching arm/hand/forearm):`, bones.slice(0, 10), bones.length > 10 ? `...and ${bones.length - 10} more` : '');
    
    // 2. Get all animation names
    const animNames = [];
    if (gltfJson.animations) {
      for (const anim of gltfJson.animations) {
        animNames.push(anim.name);
      }
    }
    console.log(`Animations:`, animNames);
  }
}

run();
