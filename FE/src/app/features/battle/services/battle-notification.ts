import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class BattleNotificationService {

  private Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    background: 'rgba(20, 20, 20, 0.85)',
    color: '#fff',
    didOpen: (toast) => {
      toast.onmouseenter = Swal.stopTimer;
      toast.onmouseleave = Swal.resumeTimer;
    }
  });

  show(message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info', duration = 3000) {
    this.Toast.fire({
      icon: type,
      title: message,
      timer: duration
    });
  }

  showModal(title: string, text: string, type: 'info' | 'success' | 'warning' | 'error' | 'question' = 'info') {
    Swal.fire({
      title,
      text,
      icon: type,
      background: 'rgba(20, 20, 20, 0.95)',
      color: '#fff',
      confirmButtonColor: '#3085d6',
    });
  }
}
