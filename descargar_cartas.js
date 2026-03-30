const https = require('https');
const fs = require('fs');
const fsPromises = require('fs').promises;
const path = require('path');

const API_URL = 'https://api.pokemontcg.io/v2/cards?pageSize=200';
const IMG_DIR = path.join(__dirname, 'frontend', 'src', 'assets', 'images', 'cards');
const JSON_PATH = path.join(__dirname, 'backend', 'src', 'main', 'resources', 'cards.json');

// 🚨 LAS 6 ENERGÍAS BÁSICAS CLÁSICAS GARANTIZADAS
const ENERGIAS_BASICAS = [
  { id: 'energy-grass', name: 'Energía Planta', imgUrl: 'https://images.pokemontcg.io/base1/99.png' },
  { id: 'energy-fire', name: 'Energía Fuego', imgUrl: 'https://images.pokemontcg.io/base1/98.png' },
  { id: 'energy-water', name: 'Energía Agua', imgUrl: 'https://images.pokemontcg.io/base1/96.png' },
  { id: 'energy-lightning', name: 'Energía Eléctrica', imgUrl: 'https://images.pokemontcg.io/base1/100.png' },
  { id: 'energy-psychic', name: 'Energía Psíquica', imgUrl: 'https://images.pokemontcg.io/base1/101.png' },
  { id: 'energy-fighting', name: 'Energía Lucha', imgUrl: 'https://images.pokemontcg.io/base1/97.png' }
];

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
      fsPromises.unlink(dest).catch(() => {});
      reject(err);
    });
  });
}

async function main() {
  try {
    console.log('Generando datos de cartas...');
    const data = await fetchJSON(API_URL);
    const cards = data.data;

    await fsPromises.mkdir(IMG_DIR, { recursive: true });
    const cleanedCards = [];

    // 1. PROCESAR CARTAS DE LA API
    for (const card of cards) {
      const id = card.id;
      const nombre = card.name;
      const tipo = card.supertype === 'Energy' ? 'Energy' : (card.types && card.types.length > 0 ? card.types[0] : 'Desconocido');
      const hp = card.hp || '0';
      
      // ✅ ¡AQUÍ ESTÁ LA MAGIA! Ahora capturamos el daño también
      const ataques = (card.attacks || []).map(a => ({ 
        nombre: a.name, 
        costo: a.cost,
        dano: a.damage || "0" // Si el ataque no hace daño (ej: curar), le pone "0"
      }));

      let rutaImagenLocal = '';
      if (card.images && card.images.small) {
        const imgPath = path.join(IMG_DIR, `${id}.png`);
        try { await fsPromises.access(imgPath); }
        catch {
          try { await downloadImage(card.images.small, imgPath); } 
          catch (e) { console.warn(`Error al descargar imagen para ${id}:`, e.message); }
        }
        rutaImagenLocal = `/images/cards/${id}.png`;
      }

      cleanedCards.push({ id, nombre, tipo, hp, ataques, imagen: rutaImagenLocal });
    }

    // 2. INYECTAR LAS ENERGÍAS BÁSICAS
    console.log('Inyectando energías básicas...');
    for (const energia of ENERGIAS_BASICAS) {
      const imgPath = path.join(IMG_DIR, `${energia.id}.png`);
      try { await fsPromises.access(imgPath); }
      catch {
        try { await downloadImage(energia.imgUrl, imgPath); } 
        catch (e) { console.warn(`Error al descargar imagen para ${energia.id}:`, e.message); }
      }
      
      cleanedCards.push({
        id: energia.id,
        nombre: energia.name,
        tipo: 'Energy',
        hp: '0',
        ataques: [],
        imagen: `/images/cards/${energia.id}.png`
      });
    }

    await fsPromises.writeFile(JSON_PATH, JSON.stringify(cleanedCards, null, 2), 'utf-8');
    console.log('✅ Datos y rutas de imágenes guardados con éxito.');
  } catch (err) {
    console.error('❌ Error:', err);
  }
}

main();