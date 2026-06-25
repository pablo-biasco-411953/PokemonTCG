import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

// ==========================================
// 1. COMPOSITE PATTERN
// ==========================================
export interface MusicComponent {
  getTitle(): string;
  getArtist(): string;
  getDuration(): string;
  getUrl(): string;
  isPlaylist(): boolean;
}

export class Song implements MusicComponent {
  constructor(
    private title: string,
    private artist: string,
    private duration: string,
    private url: string
  ) {}

  getTitle(): string { return this.title; }
  getArtist(): string { return this.artist; }
  getDuration(): string { return this.duration; }
  getUrl(): string { return this.url; }
  isPlaylist(): boolean { return false; }
}

export class Playlist implements MusicComponent {
  private components: MusicComponent[] = [];
  constructor(private name: string) {}

  add(component: MusicComponent) {
    this.components.push(component);
  }

  getTitle(): string { return this.name; }
  getArtist(): string { return 'Various Artists'; }
  getDuration(): string {
    return '14:20';
  }
  getUrl(): string { return ''; }
  isPlaylist(): boolean { return true; }

  getSongs(): Song[] {
    const songs: Song[] = [];
    this.components.forEach(c => {
      if (c instanceof Song) {
        songs.push(c);
      } else if (c instanceof Playlist) {
        songs.push(...c.getSongs());
      }
    });
    return songs;
  }
}

// ==========================================
// 2. STATE PATTERN
// ==========================================
export interface MusicPlayerContext {
  audioElement: HTMLAudioElement;
  setState(state: PlayerState): void;
}

export interface PlayerState {
  getStateName(): 'PLAYING' | 'PAUSED' | 'STOPPED';
  play(context: MusicPlayerContext): void;
  pause(context: MusicPlayerContext): void;
}

export class PlayingState implements PlayerState {
  getStateName(): 'PLAYING' { return 'PLAYING'; }
  play(context: MusicPlayerContext): void {
    // Already playing, do nothing
  }
  pause(context: MusicPlayerContext): void {
    context.audioElement.pause();
    context.setState(new PausedState());
  }
}

export class PausedState implements PlayerState {
  getStateName(): 'PAUSED' { return 'PAUSED'; }
  play(context: MusicPlayerContext): void {
    context.audioElement.play().catch(() => {});
    context.setState(new PlayingState());
  }
  pause(context: MusicPlayerContext): void {
    // Already paused
  }
}

export class StoppedState implements PlayerState {
  getStateName(): 'STOPPED' { return 'STOPPED'; }
  play(context: MusicPlayerContext): void {
    context.audioElement.play().catch(() => {});
    context.setState(new PlayingState());
  }
  pause(context: MusicPlayerContext): void {
    // Already stopped
  }
}

// ==========================================
// 3. COMMAND PATTERN
// ==========================================
export interface MusicCommand {
  execute(): void;
}

export class PlayCommand implements MusicCommand {
  constructor(private player: MusicPlayerService) {}
  execute(): void { this.player.play(); }
}

export class PauseCommand implements MusicCommand {
  constructor(private player: MusicPlayerService) {}
  execute(): void { this.player.pause(); }
}

export class NextCommand implements MusicCommand {
  constructor(private player: MusicPlayerService) {}
  execute(): void { this.player.next(); }
}

export class PrevCommand implements MusicCommand {
  constructor(private player: MusicPlayerService) {}
  execute(): void { this.player.prev(); }
}

// ==========================================
// 4. ITERATOR PATTERN
// ==========================================
export class PlaylistIterator {
  private index = 0;
  constructor(private songs: Song[], initialIndex = 0) {
    this.index = initialIndex;
  }

  hasNext(): boolean { return this.index < this.songs.length - 1; }
  hasPrev(): boolean { return this.index > 0; }

  next(): Song {
    if (this.index < this.songs.length - 1) {
      this.index++;
    } else {
      this.index = 0; // Loop back
    }
    return this.songs[this.index];
  }

  prev(): Song {
    if (this.index > 0) {
      this.index--;
    } else {
      this.index = this.songs.length - 1; // Loop to end
    }
    return this.songs[this.index];
  }

  current(): Song { return this.songs[this.index]; }
  setIndex(idx: number): void {
    if (idx >= 0 && idx < this.songs.length) {
      this.index = idx;
    }
  }
  getCurrentIndex(): number { return this.index; }
}

// ==========================================
// 5. FACADE PATTERN (angular singleton service)
// ==========================================
@Injectable({
  providedIn: 'root'
})
export class MusicPlayerService implements MusicPlayerContext {
  public audioElement: HTMLAudioElement;
  private playerState: PlayerState = new StoppedState();
  private iterator!: PlaylistIterator;
  private mainPlaylist!: Playlist;

  // Web Audio Nodes for Visualizer
  private audioContext?: AudioContext;
  private analyser?: AnalyserNode;
  private sourceNode?: MediaElementAudioSourceNode;
  private animationFrameId?: number;

  // Observer Pattern Subjects (RxJS)
  private currentSongSubject = new BehaviorSubject<Song | null>(null);
  public currentSong$: Observable<Song | null> = this.currentSongSubject.asObservable();

  private isPlayingSubject = new BehaviorSubject<boolean>(false);
  public isPlaying$: Observable<boolean> = this.isPlayingSubject.asObservable();

  private timeProgressSubject = new BehaviorSubject<{ current: string, total: string, pct: number }>({ current: '0:00', total: '0:00', pct: 0 });
  public timeProgress$: Observable<{ current: string, total: string, pct: number }> = this.timeProgressSubject.asObservable();

  private showToastSubject = new Subject<Song>();
  public showToast$: Observable<Song> = this.showToastSubject.asObservable();

  private frequencyDataSubject = new BehaviorSubject<Uint8Array | null>(null);
  public frequencyData$: Observable<Uint8Array | null> = this.frequencyDataSubject.asObservable();

  private isPinned = false;
  private isPinnedSubject = new BehaviorSubject<boolean>(false);
  public isPinned$: Observable<boolean> = this.isPinnedSubject.asObservable();

  constructor(private ngZone: NgZone) {
    this.audioElement = new Audio();
    this.audioElement.crossOrigin = 'anonymous';
    this.setupPlaylist();
    this.setupAudioListeners();
  }

  private setupPlaylist() {
    // 1. Establecer primero un playlist por defecto (fallback)
    const defaultSongs = [
      new Song('Lobby UTN FRC (Chill Lofi)', 'Pokémon Symphony Orchestra', '4:15', '/assets/login-bgm.mp3'),
      new Song('Batalla en Aula Magna (Heavy Rock)', 'Fuego y Relámpago Band', '3:45', '/assets/login-bgm.mp3'),
      new Song('Cantina Universitaria (Café Jazz)', 'Hilda\'s Smooth Trio', '3:20', '/assets/login-bgm.mp3'),
      new Song('Victoria Estudiantil (Retro 8-Bit)', 'Game Boy Soundchip', '2:50', '/assets/login-bgm.mp3')
    ];
    this.applyPlaylist(defaultSongs);

    // 2. Intentar cargar dinámicamente playlist.json
    fetch('/assets/playlist.json')
      .then(response => {
        if (!response.ok) {
          throw new Error('No se pudo cargar la playlist dinámica.');
        }
        return response.json();
      })
      .then((data: any[]) => {
        if (Array.isArray(data) && data.length > 0) {
          const dynamicSongs = data.map(item => {
            return new Song(
              item.title || 'Canción Desconocida',
              item.artist || 'Artista Desconocido',
              item.duration || '3:00',
              item.url
            );
          });
          this.applyPlaylist(dynamicSongs);
        }
      })
      .catch(err => {
        console.warn('Usando playlist por defecto:', err.message);
      });
  }

  private applyPlaylist(songs: Song[]) {
    this.mainPlaylist = new Playlist('PokeUTN Main Tracklist');
    songs.forEach(s => this.mainPlaylist.add(s));
    
    const songsList = this.mainPlaylist.getSongs();
    this.iterator = new PlaylistIterator(songsList, 0);
    
    // Si no está sonando nada, inicializamos la primera pista sin reproducir
    if (!this.isPlayingSubject.value) {
      this.setTrack(songsList[0], false);
    } else {
      // Si ya está reproduciendo, actualizamos la canción actual para que la UI se mantenga sincronizada
      this.currentSongSubject.next(this.iterator.current());
    }
  }

  private setupAudioListeners() {
    const updateProgress = () => {
      const currentSeconds = this.audioElement.currentTime;
      const song = this.iterator.current();
      
      let totalSeconds = this.audioElement.duration;
      let total = song.getDuration();
      
      // Si tenemos la duración real del elemento de audio, la usamos
      if (totalSeconds && !isNaN(totalSeconds) && totalSeconds > 0) {
        total = this.formatTime(totalSeconds);
      } else {
        const parts = total.split(':');
        totalSeconds = parseInt(parts[0], 10) * 60 + (parseInt(parts[1], 10) || 0);
      }
      
      const currentSecondsBounded = currentSeconds % (totalSeconds || 1);
      const pct = totalSeconds > 0 ? (currentSecondsBounded / totalSeconds) * 100 : 0;
      
      this.timeProgressSubject.next({
        current: this.formatTime(currentSecondsBounded),
        total,
        pct
      });
    };

    this.audioElement.addEventListener('timeupdate', () => {
      updateProgress();
    });

    this.audioElement.addEventListener('loadedmetadata', () => {
      this.ngZone.run(() => {
        updateProgress();
      });
    });

    this.audioElement.addEventListener('ended', () => {
      this.ngZone.run(() => {
        this.next(); // Auto advance on finish
      });
    });
  }

  // Web Audio Context initialization on first user interaction
  public initWebAudio() {
    if (this.audioContext) return;
    try {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 64; // Small size for simple wave graphic

      this.sourceNode = this.audioContext.createMediaElementSource(this.audioElement);
      this.sourceNode.connect(this.analyser);
      this.analyser.connect(this.audioContext.destination);

      this.startFrequencyAnalysis();
    } catch (e) {
      console.warn('Web Audio API not supported or blocked by browser policies:', e);
    }
  }

  private startFrequencyAnalysis() {
    if (!this.analyser) return;
    const bufferLength = this.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    const analyze = () => {
      this.animationFrameId = requestAnimationFrame(analyze);
      if (this.analyser && this.isPlayingSubject.value) {
        this.analyser.getByteFrequencyData(dataArray);
        this.frequencyDataSubject.next(new Uint8Array(dataArray));
      }
    };
    this.ngZone.runOutsideAngular(() => {
      analyze();
    });
  }

  private formatTime(secs: number): string {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  public setState(state: PlayerState) {
    this.playerState = state;
    this.isPlayingSubject.next(state.getStateName() === 'PLAYING');
  }

  private setTrack(song: Song, autoPlay = true) {
    this.audioElement.src = song.getUrl();
    this.audioElement.load();
    this.currentSongSubject.next(song);
    
    if (autoPlay) {
      this.audioContext?.resume();
      this.audioElement.play().then(() => {
        this.setState(new PlayingState());
        this.showToastSubject.next(song);
      }).catch(() => {
        this.setState(new PausedState());
      });
    }
  }

  // High-Level Facade Methods
  public play() {
    this.initWebAudio();
    if (this.audioContext?.state === 'suspended') {
      this.audioContext.resume();
    }
    this.playerState.play(this);
  }

  public pause() {
    this.playerState.pause(this);
  }

  public togglePlay() {
    if (this.isPlayingSubject.value) {
      this.pause();
    } else {
      this.play();
    }
  }

  public next() {
    this.initWebAudio();
    const nextSong = this.iterator.next();
    this.setTrack(nextSong, true);
  }

  public prev() {
    this.initWebAudio();
    const prevSong = this.iterator.prev();
    this.setTrack(prevSong, true);
  }

  public selectTrack(index: number) {
    this.initWebAudio();
    this.iterator.setIndex(index);
    this.setTrack(this.iterator.current(), true);
  }

  public selectRandomTrack() {
    this.initWebAudio();
    const songs = this.mainPlaylist.getSongs();
    const randomIndex = Math.floor(Math.random() * songs.length);
    this.selectTrack(randomIndex);
  }

  public getSongsList(): Song[] {
    return this.mainPlaylist.getSongs();
  }

  public getCurrentTrackIndex(): number {
    return this.iterator.getCurrentIndex();
  }

  public togglePin() {
    this.isPinned = !this.isPinned;
    this.isPinnedSubject.next(this.isPinned);
  }

  public getIsPinned(): boolean {
    return this.isPinned;
  }

  public playSong(type: 'lobby' | 'battle') {
    const songs = this.getSongsList();
    if (!songs || songs.length === 0) return;
    
    // Intentar buscar una canción por palabra clave en título o URL
    const index = songs.findIndex(song => 
      song.getTitle().toLowerCase().includes(type) || 
      song.getUrl().toLowerCase().includes(type)
    );
    
    if (index !== -1) {
      this.selectTrack(index);
    } else {
      // Fallback
      if (type === 'lobby') {
        this.selectTrack(0);
      } else if (type === 'battle') {
        this.selectTrack(songs.length > 1 ? 1 : 0);
      }
    }
  }
}
