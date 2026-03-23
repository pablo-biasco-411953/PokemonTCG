import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { App } from './app';
import { LoginComponent } from './login/login.component';
import { LobbyComponent } from './lobby/lobby.component';
import { BattleBoardComponent } from './battle-board/battle-board.component';

const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'login', component: LoginComponent },
  { path: 'lobby', component: LobbyComponent },
  { path: 'battle', component: BattleBoardComponent }
];

@NgModule({
  declarations: [App, LoginComponent, LobbyComponent, BattleBoardComponent],
  imports: [BrowserModule, HttpClientModule, FormsModule, RouterModule.forRoot(routes)],
  bootstrap: [App]
})
export class AppModule {}
