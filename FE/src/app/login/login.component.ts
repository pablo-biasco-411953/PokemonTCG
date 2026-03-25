import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // <-- Importante
import { FormsModule } from '@angular/forms'; // <-- Importante
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true, // <-- Le avisamos que es independiente
  imports: [CommonModule, FormsModule] // <-- Le damos las herramientas para el HTML
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