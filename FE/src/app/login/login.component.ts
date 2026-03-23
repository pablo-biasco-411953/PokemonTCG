import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  username: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onLogin() {
    if (this.username.trim()) {
      this.authService.login(this.username).subscribe({
        next: (jugador) => {
          localStorage.setItem('jugador', JSON.stringify(jugador));
          this.router.navigate(['/lobby']);
        },
        error: (error) => {
          console.error('Error en el login:', error);
        }
      });
    }
  }
}
