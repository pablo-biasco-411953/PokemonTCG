import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { LobbyComponent } from './lobby/lobby.component';
import { DeckBuilderComponent } from './components/deck-builder/deck-builder.component';
// 1. IMPORTÁ EL COMPONENTE DE BATALLA (chequeá que la ruta sea esta)
import { BattleBoardComponent } from './components/battle-board/battle-board.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'lobby', component: LobbyComponent },
  { path: 'deck-builder', component: DeckBuilderComponent },
  
  // 🚨 REVISÁ ESTA LÍNEA: Debe tener los dos puntos ':'
  { path: 'battle/:id', component: BattleBoardComponent }, 

  // ⛔ ESTA SIEMPRE AL FINAL
  { path: '**', redirectTo: 'login' } 
];