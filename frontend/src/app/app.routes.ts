import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { LobbyComponent } from './lobby/lobby.component';
import { DeckBuilderComponent } from './components/deck-builder/deck-builder.component';
// 1. IMPORTÁ EL COMPONENTE DE BATALLA (chequeá que la ruta sea esta)
import { BattleBoardComponent } from './battle-board/battle-board.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'lobby', component: LobbyComponent },
  { path: 'deck-builder', component: DeckBuilderComponent },
  
  // 2. AGREGÁ LA RUTA QUE TE ESTABA HACIENDO REBOTAR
  { path: 'battle', component: BattleBoardComponent },

  { path: '**', redirectTo: 'login' }
];