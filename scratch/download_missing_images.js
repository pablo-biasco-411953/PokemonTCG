const fs = require('fs');
const https = require('https');
const path = require('path');

const cardsJsonPath = 'C:\\Users\\pabli\\PokemonTCG\\backend\\src\\main\\resources\\cards.json';
const imagesDir = 'C:\\Users\\pabli\\PokemonTCG\\frontend\\public\\images\\cards';

function downloadImage(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, (response) => {
      if (response.statusCode === 200) {
        response.pipe(file);
        file.on('finish', () => {
          file.close(resolve);
        });
      } else {
        file.close();
        fs.unlink(dest, () => {});
        reject(new Error(`Server responded with ${response.statusCode}: ${response.statusMessage}`));
      }
    }).on('error', (err) => {
      file.close();
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
}

async function run() {
  const cards = JSON.parse(fs.readFileSync(cardsJsonPath, 'utf8'));
  let count = 0;
  
  for (const card of cards) {
    const imgPath = path.join(imagesDir, `${card.id}.png`);
    if (!fs.existsSync(imgPath)) {
      const parts = card.id.split('-');
      if (parts.length === 2) {
        const setId = parts[0];
        const number = parts[1].replace(/[a-zA-Z]/g, ''); // in case of like 55a
        // Use the pokemon tcg API image url format
        // Wait, standard images are at https://images.pokemontcg.io/${setId}/${number}.png
        const url = `https://images.pokemontcg.io/${setId}/${number}.png`;
        
        try {
          console.log(`Downloading missing image for ${card.nombre} (${card.id}) from ${url}`);
          await downloadImage(url, imgPath);
          count++;
          await new Promise(r => setTimeout(r, 100)); // small delay to not spam
        } catch (e) {
          console.error(`Failed to download ${card.id}: ${e.message}`);
        }
      }
    }
  }
  
  console.log(`Downloaded ${count} missing images!`);
}

run();
