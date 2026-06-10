import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface BattleNotification {
  id: number;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class BattleNotificationService {
  private notifications: BattleNotification[] = [];
  private notificationsSubject = new BehaviorSubject<BattleNotification[]>([]);
  private nextId = 0;

  getNotifications(): Observable<BattleNotification[]> {
    return this.notificationsSubject.asObservable();
  }

  show(message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info', duration = 3000) {
    const id = this.nextId++;
    const notification: BattleNotification = { id, message, type, duration };
    
    this.notifications.push(notification);
    this.notificationsSubject.next([...this.notifications]);

    if (duration > 0) {
      setTimeout(() => this.remove(id), duration);
    }
  }

  remove(id: number) {
    this.notifications = this.notifications.filter(n => n.id !== id);
    this.notificationsSubject.next([...this.notifications]);
  }
}
