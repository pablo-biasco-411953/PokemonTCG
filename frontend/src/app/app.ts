import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  // Componente raiz que solo renderiza las rutas principales.
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  // Titulo interno heredado del scaffold.
  protected readonly title = signal('frontend');
}
