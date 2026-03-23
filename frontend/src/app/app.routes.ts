import { Routes } from '@angular/router';
import { App } from './app';
import { LoginComponent } from './login/login.component';
import { LobbyComponent } from './lobby/lobby.component';
import { BattleBoardComponent } from './battle-board/battle-board.component';

export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'login', component: LoginComponent },
  { path: 'lobby', component: LobbyComponent },
  { path: 'battle', component: BattleBoardComponent }
];
