import { Component, OnInit } from '@angular/core';
import { BattleService } from '../services/battle.service';

@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss']
})
export class BattleBoardComponent implements OnInit {
  constructor(private battleService: BattleService) {}

  ngOnInit(): void {
    // Inicialización de la pantalla de batalla
  }
}
