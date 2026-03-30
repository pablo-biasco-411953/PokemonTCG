const https = require('https');
const fs = require('fs');
const fsPromises = require('fs').promises;
const path = require('path');

// Configuración de rutas
const API_URL = 'https://api.pokemontcg.io/v2/cards?pageSize=250';
const IMG_DIR = path.join(__dirname, 'frontend', 'src', 'assets', 'images', 'cards');
const JSON_PATH = path.join(__dirname, 'backend', 'src', 'main', 'resources', 'cards.json');

const ENERGIAS_BASICAS = [
  { id: 'energy-grass', name: 'Energía Planta', imgUrl: 'https://images.pokemontcg.io/base1/99.png', tipo: 'Grass' },
  { id: 'energy-fire', name: 'Energía Fuego', imgUrl: 'https://images.pokemontcg.io/base1/98.png', tipo: 'Fire' },
  { id: 'energy-water', name: 'Energía Agua', imgUrl: 'https://images.pokemontcg.io/base1/96.png', tipo: 'Water' },
  { id: 'energy-lightning', name: 'Energía Eléctrica', imgUrl: 'https://images.pokemontcg.io/base1/100.png', tipo: 'Lightning' },
  { id: 'energy-psychic', name: 'Energía Psíquica', imgUrl: 'https://images.pokemontcg.io/base1/101.png', tipo: 'Psychic' },
  { id: 'energy-fighting', name: 'Energía Lucha', imgUrl: 'https://images.pokemontcg.io/base1/97.png', tipo: 'Fighting' }
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
    console.log('🚀 Iniciando generación de datos con Debilidades...');
    const data = await fetchJSON(API_URL);
    const cards = data.data;

    await fsPromises.mkdir(IMG_DIR, { recursive: true });
    const cleanedCards = [];

    for (const card of cards) {
      if (card.supertype !== 'Pokémon') continue;

      const id = card.id;
      const nombre = card.name;
      const tipo = (card.types && card.types.length > 0) ? card.types[0] : 'Colorless';
      const hp = card.hp || '60';
      
      // 1. Mapeo de Ataques
      const ataques = (card.attacks || []).map(a => {
        let damageValue = 0;
        if (a.damage) {
          damageValue = parseInt(a.damage.replace(/[^0-9]/g, '')) || 0;
        }
        return { 
          nombre: a.name, 
          costo: a.cost || [], 
          dano: damageValue 
        };
      });

      // 2. 🚩 NUEVO: Mapeo de Debilidades (Weaknesses)
      const debilidades = (card.weaknesses || []).map(w => ({
        tipo: w.type,
        valor: w.value // Guardamos el "x2"
      }));

      // 3. 🚩 NUEVO: Mapeo de Resistencias (Resistances)
      const resistencias = (card.resistances || []).map(r => ({
        tipo: r.type,
        valor: r.value // Guardamos el "-20"
      }));

      let rutaImagenLocal = `/assets/images/cards/${id}.png`;
      const imgPath = path.join(IMG_DIR, `${id}.png`);
      
      try {
        await fsPromises.access(imgPath);
      } catch {
        console.log(`  📥 Descargando: ${nombre}...`);
        await downloadImage(card.images.small, imgPath);
      }

      cleanedCards.push({ 
        id, 
        nombre, 
        tipo, 
        hp, 
        ataques, 
        debilidades, 
        resistencias, 
        imagen: rutaImagenLocal 
      });
    }

    // 4. Inyección de Energías Básicas
    console.log('⚡ Inyectando energías...');
    for (const energia of ENERGIAS_BASICAS) {
      const imgPath = path.join(IMG_DIR, `${energia.id}.png`);
      try { await fsPromises.access(imgPath); } catch {
        await downloadImage(energia.imgUrl, imgPath);
      }
      
      cleanedCards.push({
        id: energia.id,
        nombre: energia.name,
        tipo: 'Energy',
        hp: '0',
        ataques: [],
        debilidades: [],
        resistencias: [],
        imagen: `/assets/images/cards/${energia.id}.png`
      });
    }

    await fsPromises.writeFile(JSON_PATH, JSON.stringify(cleanedCards, null, 2), 'utf-8');
    console.log(`\n✅ Proceso terminado. ${cleanedCards.length} cartas con debilidades guardadas.`);

  } catch (err) {
    console.error('❌ Error crítico:', err);
  }
}

main();