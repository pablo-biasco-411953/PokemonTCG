import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleNotification, BattleNotificationService } from '../../services/battle-notification';

@Component({
  selector: 'app-battle-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-notification.html',
  styleUrls: ['./battle-notification.scss']
})
export class BattleNotificationComponent implements OnInit {
  notifications: BattleNotification[] = [];

  constructor(private notificationService: BattleNotificationService) {}

  ngOnInit(): void {
    this.notificationService.getNotifications().subscribe(n => {
      this.notifications = n;
    });
  }

  remove(id: number): void {
    this.notificationService.remove(id);
  }
}
