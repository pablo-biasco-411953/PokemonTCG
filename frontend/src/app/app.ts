import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LanguageSelectorComponent } from './components/language-selector/language-selector.component';

@Component({
  selector: 'app-root',
  standalone: true, // 🔥 ESTA ES LA CLAVE
  imports: [RouterOutlet, LanguageSelectorComponent],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  protected readonly title = signal('frontend');
}
