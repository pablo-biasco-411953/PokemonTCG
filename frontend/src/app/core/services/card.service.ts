import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Card } from '../../shared/models/card';
import { getBackendUrl } from './api-config';
import { I18nService } from '../../i18n/i18n.service';

@Injectable({ providedIn: 'root' })
export class CardService {
  private apiUrl = `${getBackendUrl()}/api/cards`;
  private cardMap = new Map<string, Card>();
  private i18n = inject(I18nService);

  constructor(private http: HttpClient) {}

  setCatalog(cards: Card[]) {
    this.cardMap.clear();
    for (const c of cards) {
      this.cardMap.set(c.id, c);
    }
  }

  // Devuelve el catalogo completo de cartas, usando el idioma seleccionado.
  getAll(lang?: string): Observable<Card[]> {
    const activeLang = lang || this.i18n.currentLanguage();
    const url = `${this.apiUrl}?lang=${activeLang}`;
    return this.http.get<Card[]>(url).pipe(
      tap(cards => this.setCatalog(cards))
    );
  }

  // Arma la ruta de la imagen basándose en el idioma configurado
  getImagenCarta(id: string): string {
    const lang = this.i18n.currentLanguage();
    if (lang === 'en') {
      return `/images/cards/${id}.png`;
    }
    return `/images/cards/${lang}/${id}.png`;
  }

  // Traduce una carta utilizando el catálogo cargado en memoria.
  // Preserva el nombre original (inglés) de cada ataque en `nombreOriginal`
  // para que el backend siempre reciba el nombre correcto.
  translateCard(card: Card | any): any {
    if (!card) return card;
    const id = card.id || card.card?.id;
    if (!id) return card;
    const tr = this.cardMap.get(id);
    if (!tr) return card;

    const mergeAtaques = (originalAtaques: any[] | undefined, translatedAtaques: any[] | undefined) => {
      if (!translatedAtaques) return originalAtaques;
      return translatedAtaques.map((trAtk: any, i: number) => ({
        ...trAtk,
        nombreOriginal: originalAtaques?.[i]?.nombreOriginal ?? originalAtaques?.[i]?.nombre ?? trAtk.nombre,
        textoOriginal: originalAtaques?.[i]?.textoOriginal ?? originalAtaques?.[i]?.texto ?? trAtk.texto
      }));
    };

    if (card.card) {
      return {
        ...card,
        card: {
          ...card.card,
          nombre: tr.nombre,
          reglas: tr.reglas,
          ataques: mergeAtaques(card.card.ataques, tr.ataques)
        }
      };
    } else {
      return {
        ...card,
        nombre: tr.nombre,
        reglas: tr.reglas,
        ataques: mergeAtaques(card.ataques, tr.ataques)
      };
    }
  }

  // Traduce recursivamente todas las cartas que se encuentran en el estado de partida de juego
  translatePartida(partida: any): any {
    if (!partida) return partida;
    
    const translateTablero = (tablero: any) => {
      if (!tablero) return tablero;
      return {
        ...tablero,
        mano: (tablero.mano || []).map((c: any) => this.translateCard(c)),
        mazo: (tablero.mazo || []).map((c: any) => this.translateCard(c)),
        premios: (tablero.premios || []).map((c: any) => this.translateCard(c)),
        pilaDescarte: (tablero.pilaDescarte || []).map((c: any) => this.translateCard(c)),
        activo: tablero.activo ? {
          ...tablero.activo,
          card: this.translateCard(tablero.activo.card),
          attachedTools: (tablero.activo.attachedTools || []).map((c: any) => this.translateCard(c)),
          energiasUnidas: (tablero.activo.energiasUnidas || []).map((c: any) => this.translateCard(c))
        } : null,
        banca: (tablero.banca || []).map((b: any) => ({
          ...b,
          card: this.translateCard(b.card),
          attachedTools: (b.attachedTools || []).map((c: any) => this.translateCard(c)),
          energiasUnidas: (b.energiasUnidas || []).map((c: any) => this.translateCard(c))
        }))
      };
    };

    return {
      ...partida,
      jugador: translateTablero(partida.jugador),
      bot: translateTablero(partida.bot),
      activeStadium: this.translateCard(partida.activeStadium),
      pendingAction: partida.pendingAction ? {
        ...partida.pendingAction,
        options: (partida.pendingAction.options || []).map((opt: any) => this.translateCard(opt))
      } : null
    };
  }

  // Maneja el error de carga de imagen de carta permitiendo fallback a la carpeta base (inglés)
  handleCardImageError(img: HTMLImageElement): void {
    if (!img || img.src.endsWith('/images/cards/back.png') || img.src.endsWith('/images/cards/card-back.png')) return;
    const match = img.src.match(/\/images\/cards\/[a-z]{2}\/([^/]+)$/i);
    if (match) {
      const cardFile = match[1];
      img.src = `/images/cards/${cardFile}`;
    } else {
      img.src = '/images/cards/back.png';
    }
  }
}
