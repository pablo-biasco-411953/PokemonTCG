const https = require('https');
const fs = require('fs');
const fsPromises = require('fs').promises;
const path = require('path');

// --- CONFIGURACIÓN DE RUTAS ---
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

// --- FUNCIONES AUXILIARES ---
async function fetchJSON(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'X-Api-Key': 'TU_API_KEY_OPCIONAL' } }, (res) => {
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
      // Si no es 200, cerramos el archivo y rechazamos para que el try/catch de arriba lo agarre
      if (res.statusCode !== 200) {
        file.close();
        fs.unlink(prop, () => {}); // Borramos el archivo vacío
        reject(new Error(`Status ${res.statusCode}`));
        return;
      }
      res.pipe(file);
      file.on('finish', () => file.close(resolve));
    }).on('error', err => {
      file.close();
      fsPromises.unlink(dest).catch(() => {});
      reject(err);
    });
  });
}
// --- PROCESO PRINCIPAL ---
async function main() {
  try {
    console.log('🚀 Iniciando descarga estratégica (2000 cartas de varias eras)...');
    let allCards = [];
    
    // Definimos cuotas para asegurar rarezas y tipos de carta
    const CUOTAS = [
      { filtro: 'rarity:"Rare Holo GX" OR rarity:"Rare Holo V" OR rarity:"Rare Ultra"', cantidad: 200, label: '🌟 ÉPICAS (V/GX/VMAX)' },
      { filtro: 'rarity:"Rare Holo" OR rarity:"Rare"', cantidad: 300, label: '💎 RARAS' },
      { filtro: 'supertype:trainer', cantidad: 400, label: '🎒 ENTRENADORES' },
      { filtro: 'supertype:pokemon', cantidad: 1100, label: '🃏 COMUNES / INFRECUENTES' }
    ];

    await fsPromises.mkdir(IMG_DIR, { recursive: true });

    for (const cuota of CUOTAS) {
      let page = 1;
      let countEnCuota = 0;
      console.log(`\n🔹 Buscando ${cuota.label}...`);

      while (countEnCuota < cuota.cantidad) {
        const url = `${API_URL}&q=${encodeURIComponent(cuota.filtro)}&page=${page}`;
        const response = await fetchJSON(url);
        const cards = response.data;

        if (!cards || cards.length === 0) break;

        for (const card of cards) {
          if (countEnCuota >= cuota.cantidad) break;
          if (allCards.some(c => c.id === card.id)) continue;

          // Mapeo detallado para el Backend
          const id = card.id;
          const ataques = (card.attacks || []).map(a => ({
            nombre: a.name,
            costo: a.cost || [],
            danio: parseInt(a.damage?.replace(/[^0-9]/g, '')) || 0,
            texto: a.text || ""
          }));

          const habilidades = (card.abilities || []).map(ab => ({
            nombre: ab.name,
            texto: ab.text,
            tipo: ab.type
          }));

        const imgPath = path.join(IMG_DIR, `${id}.png`);
try {
  await fsPromises.access(imgPath);
} catch {
  try {
    process.stdout.write(`📥 [Total: ${allCards.length + 1}] Descargando ${card.name}...          \r`);
    await downloadImage(card.images.small, imgPath);
  } catch (imgErr) {
    // 🚩 ACÁ ESTÁ EL SECRETO: 
    // Si falla la imagen, logueamos el error pero NO cortamos el proceso.
    console.log(`\n⚠️ Saltando imagen de ${card.name} (Error: ${imgErr.message})`);
    // Opcional: Podés elegir NO agregar la carta al JSON si no tiene imagen
    // continue; 
  }
}

          allCards.push({
            id,
            nombre: card.name,
            supertype: card.supertype,
            subtypes: card.subtypes || [],
            evolucionDe: card.evolvesFrom || null,
            tipo: card.types ? card.types[0] : 'Colorless',
            hp: card.hp || '0',
            costoRetirada: card.convertedRetreatCost || 0,
            ataques,
            habilidades,
            debilidades: (card.weaknesses || []).map(w => ({ tipo: w.type, valor: w.value })),
            resistencias: (card.resistances || []).map(r => ({ tipo: r.type, valor: r.value })),
            reglas: card.rules || [],
            rareza: card.rarity || 'Common',
            imagen: `/assets/images/cards/${id}.png`
          });

          countEnCuota++;
        }
        page++;
        if (page > 15) break; 
        await new Promise(r => setTimeout(r, 400)); // Delay para evitar baneo de IP
      }
    }

    // Inyectar Energías Básicas
    console.log('\n\n⚡ Verificando energías básicas...');
    for (const energia of ENERGIAS_BASICAS) {
      const imgPath = path.join(IMG_DIR, `${energia.id}.png`);
      try { await fsPromises.access(imgPath); } catch {
        await downloadImage(energia.imgUrl, imgPath);
      }
      if (!allCards.some(c => c.id === energia.id)) {
        allCards.push({
          id: energia.id,
          nombre: energia.name,
          supertype: 'Energy',
          subtypes: ['Basic'],
          tipo: 'Energy',
          hp: '0',
          ataques: [],
          habilidades: [],
          debilidades: [],
          resistencias: [],
          imagen: `/assets/images/cards/${energia.id}.png`
        });
      }
    }

    await fsPromises.writeFile(JSON_PATH, JSON.stringify(allCards, null, 2), 'utf-8');
    console.log(`\n✅ ¡ÉXITO! Se generó el archivo con ${allCards.length} cartas.`);
    console.log(`Ubicación: ${JSON_PATH}`);

  } catch (err) {
    console.error('❌ Error crítico:', err);
  }
}

main();