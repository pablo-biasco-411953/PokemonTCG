import { Injectable } from '@angular/core';
import { Observable, Subscriber } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ImagePreloaderService {
  constructor() {}

  /**
   * Preloads an array of image URLs and emits the percentage progress.
   * @param urls Array of image URLs to preload
   * @returns An Observable that emits progress (0 to 100) and completes at 100.
   */
  preloadImages(urls: string[]): Observable<number> {
    return new Observable<number>((observer: Subscriber<number>) => {
      const total = urls.length;
      if (total === 0) {
        observer.next(100);
        observer.complete();
        return;
      }

      let loaded = 0;

      const onLoadComplete = () => {
        loaded++;
        const progress = Math.floor((loaded / total) * 100);
        observer.next(progress);
        
        if (loaded === total) {
          observer.complete();
        }
      };

      urls.forEach(url => {
        const img = new Image();
        img.onload = onLoadComplete;
        img.onerror = onLoadComplete; // Contamos los fallos como carga completa para no bloquear
        img.src = url;
      });
    });
  }
}
