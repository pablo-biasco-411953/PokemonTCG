import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getBackendUrl } from '../../../core/services/api-config';

export type LobbyRoomStatus = 'OPEN' | 'IN_PROGRESS' | 'FINISHED';

export interface LobbyRoomChatMessage {
  sender: string;
  text: string;
  sentAt: number;
  system: boolean;
}

export interface LobbyRoomReaction {
  id: string;
  sender: string;
  reaction: string;
  sentAt: number;
}

export interface LobbyRoomSnapshot {
  id: string;
  name: string;
  status: LobbyRoomStatus;
  locked: boolean;
  ownerUsername: string;
  ownerDeckName: string;
  ownerReady: boolean;
  guestUsername?: string | null;
  guestDeckName?: string | null;
  guestReady: boolean;
  guestBot: boolean;
  botDifficulty?: 'EASY' | 'NORMAL' | 'HARD';
  playerCount: number;
  spectatorCount: number;
  matchId?: string | null;
  canJoin: boolean;
  canSpectate: boolean;
  currentUserSpectator: boolean;
  updatedAt: number;
  chat: LobbyRoomChatMessage[];
  reactions: LobbyRoomReaction[];
}

export interface LobbyRoomStartResponse {
  room: LobbyRoomSnapshot;
  matchId: string;
}

@Injectable({ providedIn: 'root' })
export class LobbyRoomService {
  private base = `${getBackendUrl()}/api/lobby-rooms`;

  constructor(private http: HttpClient) {}

  listRooms(): Observable<LobbyRoomSnapshot[]> {
    return this.http.get<LobbyRoomSnapshot[]>(this.base, this.headers());
  }

  getRoomByMatch(matchId: string): Observable<LobbyRoomSnapshot> {
    return this.http.get<LobbyRoomSnapshot>(`${this.base}/match/${matchId}`, this.headers());
  }

  createRoom(roomName: string, mazoId: number, deckName: string, password = ''): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(this.base, { roomName, mazoId, deckName, password }, this.headers());
  }

  joinRoom(roomId: string, mazoId: number, deckName: string, password = ''): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/join`, { mazoId, deckName, password }, this.headers());
  }

  leaveRoom(roomId: string): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/leave`, {}, this.headers());
  }

  kickGuest(roomId: string): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/kick`, {}, this.headers());
  }

  addBot(roomId: string, botDifficulty: 'EASY' | 'NORMAL' | 'HARD'): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/bot`, { botDifficulty }, this.headers());
  }

  setReady(roomId: string, ready: boolean, mazoId: number | null): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/ready`, { ready, mazoId }, this.headers());
  }

  startRoom(roomId: string): Observable<LobbyRoomStartResponse> {
    return this.http.post<LobbyRoomStartResponse>(`${this.base}/${roomId}/start`, {}, this.headers());
  }

  spectateRoom(roomId: string, password = ''): Observable<LobbyRoomStartResponse> {
    return this.http.post<LobbyRoomStartResponse>(`${this.base}/${roomId}/spectate`, { password }, this.headers());
  }

  sendChat(roomId: string, text: string): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/chat`, { text }, this.headers());
  }

  sendReaction(roomId: string, reaction: string): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/${roomId}/reaction`, { text: reaction }, this.headers());
  }

  sendMatchReaction(matchId: string, reaction: string): Observable<LobbyRoomSnapshot> {
    return this.http.post<LobbyRoomSnapshot>(`${this.base}/match/${matchId}/reaction`, { text: reaction }, this.headers());
  }

  private headers(): { headers: HttpHeaders } {
    let username = '';
    try {
      const data = localStorage.getItem('jugador');
      username = data ? JSON.parse(data).username || '' : '';
    } catch {
      username = '';
    }
    return { headers: new HttpHeaders({ 'X-Username': username }) };
  }
}
