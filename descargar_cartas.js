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
      // 🚩 CAMBIO 1: Ahora dejamos pasar a los Pokémon Y a las cartas de Entrenador
      if (card.supertype !== 'Pokémon' && card.supertype !== 'Trainer') continue;

      const id = card.id;
      const nombre = card.name;
      const supertype = card.supertype; // 'Pokémon' o 'Trainer'
      const subtypes = card.subtypes || []; // Ej: ['Basic'], ['Stage 1'], ['Item']
      
      // Si es Trainer no tiene tipo elemental, le clavamos 'Trainer'
      const tipo = (card.types && card.types.length > 0) ? card.types[0] : supertype; 
      
      const hp = card.hp || '0';
      const costoRetirada = card.convertedRetreatCost || 0;
      const evolvesFrom = card.evolvesFrom || null; // 🚩 NUEVO: Para evoluciones
      const reglas = card.rules || []; // 🚩 NUEVO: Para saber qué hace un Objeto/Partidario

      // 🚩 NUEVO: Mapeo de Habilidades Pasivas
      const habilidades = (card.abilities || []).map(ab => ({
        nombre: ab.name,
        tipo: ab.type,
        texto: ab.text
      }));

      // 1. Mapeo de Ataques Mejorado
      const ataques = (card.attacks || []).map(a => {
        let damageValue = 0;
        if (a.damage) {
          damageValue = parseInt(a.damage.replace(/[^0-9]/g, '')) || 0;
        }
        return { 
          nombre: a.name, 
          costo: a.cost || [], 
          dano: damageValue,
          texto: a.text || '' // 🚩 ¡ACÁ ESTÁ LA MAGIA DE LOS EFECTOS!
        };
      });

      const debilidades = (card.weaknesses || []).map(w => ({
        tipo: w.type,
        valor: w.value
      }));

      const resistencias = (card.resistances || []).map(r => ({
        tipo: r.type,
        valor: r.value
      }));

      let rutaImagenLocal = `/images/cards/${id}.png`;
      const imgPath = path.join(IMG_DIR, `${id}.png`);
      
      try {
        await fsPromises.access(imgPath);
      } catch {
        console.log(`  📥 Descargando: ${nombre}...`);
        await downloadImage(card.images.small, imgPath);
      }

      // Agregamos todo al objeto final
      cleanedCards.push({ 
        id, 
        nombre, 
        supertype,
        subtypes,
        tipo, 
        hp, 
        costoRetirada, 
        evolvesFrom,
        habilidades,
        ataques, 
        debilidades, 
        resistencias,
        reglas,
        imagen: rutaImagenLocal 
      });
    }

    // 4. Inyección de Energías Básicas (Cambiamos un poquito para que coincida con el nuevo formato)
    console.log('⚡ Inyectando energías...');
    for (const energia of ENERGIAS_BASICAS) {
      const imgPath = path.join(IMG_DIR, `${energia.id}.png`);
      try { await fsPromises.access(imgPath); } catch {
        await downloadImage(energia.imgUrl, imgPath);
      }
      
      cleanedCards.push({
        id: energia.id,
        nombre: energia.name,
        supertype: 'Energy',
        subtypes: ['Basic'],
        tipo: energia.tipo,
        hp: '0',
        costoRetirada: 0,
        evolvesFrom: null,
        habilidades: [],
        ataques: [],
        debilidades: [],
        resistencias: [],
        reglas: [],
        imagen: `/images/cards/${energia.id}.png`
      });
    }

    await fsPromises.writeFile(JSON_PATH, JSON.stringify(cleanedCards, null, 2), 'utf-8');
    console.log(`\n✅ Proceso terminado. ${cleanedCards.length} cartas con debilidades guardadas.`);

  } catch (err) {
    console.error('❌ Error crítico:', err);
  }
}

main();