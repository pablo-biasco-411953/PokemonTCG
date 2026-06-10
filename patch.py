import sys
content = open('frontend/src/app/features/battle/battle-board.component.html', 'r', encoding='utf-8').read()

target = '''<div
  class="endgame-overlay"'''

replacement = '''<div class="setup-overlay" *ngIf="estadoSetupMulligan">
  <div class="setup-backdrop"></div>
  <div class="setup-panel" *ngIf="estadoSetupMulligan === 'REVEAL'">
    <h2 class="setup-title">{{ 'battle.mulliganRevealTitle' | t:{rival: nombreRival} }}</h2>
    <p class="setup-sub">{{ 'battle.mulliganRevealSub' | t }}</p>
    <div class="setup-hand-grid">
      <div class="setup-card" *ngFor="let c of mulliganCartasRival">
        <img [src]="getImagenCarta(c.id)" (error)="onCardImageError($event)" />
      </div>
    </div>
  </div>
  <div class="setup-panel" *ngIf="estadoSetupMulligan === 'EXTRA_DRAW'">
    <h2 class="setup-title">{{ 'battle.mulliganExtraDrawTitle' | t }}</h2>
    <p class="setup-sub">{{ 'battle.mulliganExtraDrawSub' | t:{permitidas: cartasExtraPermitidas.toString(), oponenteCount: mulliganOponenteCount.toString()} }}</p>
    <div class="setup-draw-actions" *ngIf="cartasExtraPermitidas > 0">
      <button class="btn-primary" (click)="enviarCartasExtra(cartasExtraPermitidas)">{{ 'battle.drawAll' | t }} ({{ cartasExtraPermitidas }})</button>
      <button class="btn-secondary" (click)="enviarCartasExtra(0)">{{ 'battle.declineDraw' | t }}</button>
    </div>
    <div class="setup-draw-actions" *ngIf="cartasExtraPermitidas === 0">
      <p>{{ 'battle.waitingRival' | t }}</p>
    </div>
  </div>
</div>

<div
  class="endgame-overlay"'''

if target in content:
    with open('frontend/src/app/features/battle/battle-board.component.html', 'w', encoding='utf-8') as f:
        f.write(content.replace(target, replacement))
    print('Replaced successfully.')
else:
    print('Target not found.')
