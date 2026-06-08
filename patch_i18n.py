import sys

with open('frontend/src/app/i18n/i18n.service.ts', 'r', encoding='utf-8') as f:
    content = f.read()

import re

def insert_translations(lang_code, translations_str):
    pattern = r"('" + lang_code + r"':\s*\{)(.*?)(  \},|  \}$)"
    
    def repl(match):
        return match.group(1) + match.group(2) + translations_str + match.group(3)
        
    return re.sub(pattern, repl, content, flags=re.DOTALL)

en_add = """
    'battle.mulliganRevealTitle': 'MULLIGAN REVEAL',
    'battle.mulliganRevealSub': 'No Basic Pokémon in hand. Revealing and reshuffling...',
    'battle.mulliganExtraDrawTitle': 'MULLIGAN EXTRA DRAW',
    'battle.mulliganExtraDrawSub': 'Opponent mulligans: {oponenteCount}. You can draw up to {permitidas} extra cards.',
    'battle.drawAll': 'DRAW CARDS',
    'battle.declineDraw': 'NO THANKS',
    'battle.waitingRival': 'Waiting for rival to draw extra cards...',
"""

es_add = """
    'battle.mulliganRevealTitle': 'MULLIGAN DE {rival}',
    'battle.mulliganRevealSub': 'Sin Pokémon Básicos. Revelando mano y barajando...',
    'battle.mulliganExtraDrawTitle': 'ROBO EXTRA POR MULLIGAN',
    'battle.mulliganExtraDrawSub': 'Mulligans del rival: {oponenteCount}. Podés robar hasta {permitidas} cartas extra.',
    'battle.drawAll': 'ROBAR CARTAS',
    'battle.declineDraw': 'NO, GRACIAS',
    'battle.waitingRival': 'Esperando que el rival robe cartas extra...',
"""

pt_add = """
    'battle.mulliganRevealTitle': 'MULLIGAN REVELADO',
    'battle.mulliganRevealSub': 'Nenhum Pokémon Básico na mão. Revelando e embaralhando...',
    'battle.mulliganExtraDrawTitle': 'COMPRA EXTRA DE MULLIGAN',
    'battle.mulliganExtraDrawSub': 'Mulligans do rival: {oponenteCount}. Você pode comprar até {permitidas} cartas extras.',
    'battle.drawAll': 'COMPRAR CARTAS',
    'battle.declineDraw': 'NÃO, OBRIGADO',
    'battle.waitingRival': 'Aguardando o rival comprar cartas extras...',
"""

ja_add = """
    'battle.mulliganRevealTitle': 'マリガン公開',
    'battle.mulliganRevealSub': '手札にたねポケモンがいません。手札を公開して山札に戻します...',
    'battle.mulliganExtraDrawTitle': 'マリガン追加ドロー',
    'battle.mulliganExtraDrawSub': '相手のマリガン: {oponenteCount}。最大{permitidas}枚のカードを追加で引くことができます。',
    'battle.drawAll': 'カードを引く',
    'battle.declineDraw': 'いいえ、結構です',
    'battle.waitingRival': '相手が追加のカードを引くのを待っています...',
"""

content = insert_translations('en', en_add)
content = insert_translations('es', es_add)
content = insert_translations('pt', pt_add)
content = insert_translations('ja', ja_add)

with open('frontend/src/app/i18n/i18n.service.ts', 'w', encoding='utf-8') as f:
    f.write(content)
print('Translations added successfully.')
