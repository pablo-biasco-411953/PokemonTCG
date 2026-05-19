import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { LobbyComponent } from './lobby/lobby.component';
import { DeckBuilderComponent } from './components/deck-builder/deck-builder.component';
import { BattleBoardComponent } from './components/battle-board/battle-board.component';

// Mapa central de navegacion del frontend.
export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'lobby', component: LobbyComponent },
  { path: 'deck-builder', component: DeckBuilderComponent },
  { path: 'battle/:id', component: BattleBoardComponent },
  { path: '**', redirectTo: 'login' }
];
