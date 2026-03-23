const https = require('https');
const fs = require('fs').promises;
const path = require('path');

const API_URL = 'https://api.pokemontcg.io/v2/cards?pageSize=200';
const IMG_DIR = path.join(__dirname, 'frontend', 'src', 'assets', 'images', 'cards');
const JSON_PATH = path.join(__dirname, 'backend', 'src', 'main', 'resources', 'cards.json');

async function fetchJSON(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

async function downloadImage(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, (res) => {
      res.pipe(file);
      file.on('finish', () => { file.close(resolve); });
    }).on('error', err => {
      fs.unlink(dest).catch(() => {});
      reject(err);
    });
  });
}

async function main() {
  try {
    console.log('Generando datos de cartas...');
    const data = await fetchJSON(API_URL);
    const cards = data.data;

    await fs.mkdir(IMG_DIR, { recursive: true });

    const cleanedCards = [];

    for (const card of cards) {
      const id = card.id;
      const nombre = card.name;
      const tipo = card.types && card.types.length > 0 ? card.types[0] : 'Desconocido';
      const hp = card.hp || '0';
      const ataques = (card.attacks || []).map(a => ({ nombre: a.name, costo: a.cost }));

      let rutaImagenLocal = '';
      if (card.images && card.images.small) {
        const imgPath = path.join(IMG_DIR, `${id}.png`);
        try { await fs.access(imgPath); }
        catch {
          try { await downloadImage(card.images.small, imgPath); } catch (e) { console.warn(`Error al descargar imagen para ${id}:`, e.message); }
        }
        rutaImagenLocal = `assets/images/cards/${id}.png`;
      }

      cleanedCards.push({ id, nombre, tipo, hp, ataques, imagen: rutaImagenLocal });
    }

    await fs.writeFile(JSON_PATH, JSON.stringify(cleanedCards, null, 2), 'utf-8');
    console.log('Datos y rutas de imágenes guardados con éxito.');
  } catch (err) {
    console.error('Error:', err);
  }
}

main();