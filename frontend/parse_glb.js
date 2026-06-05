const fs = require('fs');

function parseGlb(filePath) {
    const buffer = fs.readFileSync(filePath);
    const magic = buffer.readUInt32LE(0);
    if (magic !== 0x46546C67) {
        console.error('Not a valid GLB file');
        return;
    }
    const version = buffer.readUInt32LE(4);
    const length = buffer.readUInt32LE(8);

    let offset = 12;
    while (offset < length) {
        const chunkLength = buffer.readUInt32LE(offset);
        const chunkType = buffer.readUInt32LE(offset + 4);
        offset += 8;
        
        if (chunkType === 0x4E4F534A) { // JSON
            const jsonChunk = buffer.toString('utf8', offset, offset + chunkLength);
            const json = JSON.parse(jsonChunk);
            console.log("Meshes:");
            json.meshes?.forEach((m, i) => console.log(`  [${i}] ${m.name}`));
            console.log("Materials:");
            json.materials?.forEach((m, i) => console.log(`  [${i}] ${m.name}`));
            console.log("Nodes:");
            json.nodes?.forEach((n, i) => {
                if (n.mesh !== undefined) {
                    console.log(`  [${i}] ${n.name} (mesh: ${n.mesh})`);
                }
            });
            break;
        }
        offset += chunkLength;
    }
}

parseGlb('C:/Users/pabli/PokemonTCG/frontend/public/models-optimized/characters/santoro.glb');
