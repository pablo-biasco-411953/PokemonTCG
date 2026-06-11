const fs = require('fs');
const path = require('path');

const cardsPath = path.join(__dirname, 'cards.json');
const cardsData = JSON.parse(fs.readFileSync(cardsPath, 'utf8'));

const bunnelby = cardsData.find(c => c.nombre === 'Bunnelby');
const diggersby = cardsData.find(c => c.nombre === 'Diggersby');

const attacks = [...bunnelby.ataques, ...diggersby.ataques];

function simulateParser(texto) {
    if (!texto) return [];
    let lowerText = texto.toLowerCase();
    let commands = [];
    
    // Simulate what AttackEffectParserService does
    if (lowerText.includes("if heads, prevent all effects of attacks") || lowerText.includes("if heads, prevent all damage done to this pok")) {
        commands.push("CoinFlipCommand(SetInvulnerableCommand)");
    }
    
    if (lowerText.includes("prevent all damage done to this pok") && !lowerText.includes("if heads")) {
        commands.push("SetInvulnerableCommand");
    }

    if (lowerText.includes("flip a coin. if tails, this attack does nothing")) {
        // attack fails on tails
    }

    return commands;
}

attacks.forEach(att => {
    if (att.texto) {
        console.log("Attack:", att.nombre);
        console.log("Commands:", simulateParser(att.texto));
    }
});
