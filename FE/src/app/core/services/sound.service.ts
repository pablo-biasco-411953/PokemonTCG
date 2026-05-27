import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SoundService {
  private ctx: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private muted = false;

  constructor() {
    this.initInteractionListener();
  }

  private initInteractionListener(): void {
    const resumeAudio = () => {
      // Force creation if not exists
      this.getCtx();
      if (this.ctx && this.ctx.state !== 'running') {
        this.ctx.resume().then(() => {
          if (this.ctx!.state === 'running') {
            ['click', 'keydown', 'touchstart'].forEach(e => 
              document.removeEventListener(e, resumeAudio)
            );
          }
        }).catch(() => {});
      } else if (this.ctx && this.ctx.state === 'running') {
        ['click', 'keydown', 'touchstart'].forEach(e => 
          document.removeEventListener(e, resumeAudio)
        );
      }
    };
    ['click', 'keydown', 'touchstart'].forEach(e => 
      document.addEventListener(e, resumeAudio, { capture: true })
    );
  }

  private getCtx(): AudioContext {
    if (!this.ctx) {
      this.ctx = new AudioContext();
      this.masterGain = this.ctx.createGain();
      this.masterGain.gain.value = 0.35;
      this.masterGain.connect(this.ctx.destination);
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
    return this.ctx;
  }

  private out(): GainNode {
    this.getCtx();
    return this.masterGain!;
  }

  toggleMute(): boolean {
    this.muted = !this.muted;
    if (this.masterGain) {
      this.masterGain.gain.value = this.muted ? 0 : 0.35;
    }
    return this.muted;
  }

  get isMuted(): boolean {
    return this.muted;
  }

  // ─── Intro burst / explosion ───
  burst(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;

    // Massive cinematic sub-bass impact (Sine sweeping down)
    const subOsc = ctx.createOscillator();
    const subGain = ctx.createGain();
    subOsc.type = 'sine';
    subOsc.frequency.setValueAtTime(180, now);
    subOsc.frequency.exponentialRampToValueAtTime(20, now + 1.2);
    subGain.gain.setValueAtTime(1.0, now);
    subGain.gain.exponentialRampToValueAtTime(0.001, now + 1.5);
    subOsc.connect(subGain).connect(this.out());
    subOsc.start(now);
    subOsc.stop(now + 1.5);

    // Thick distortion/crunch layer (Sawtooth + lowpass)
    const crunchOsc = ctx.createOscillator();
    const crunchFilter = ctx.createBiquadFilter();
    const crunchGain = ctx.createGain();
    crunchOsc.type = 'sawtooth';
    crunchOsc.frequency.setValueAtTime(60, now);
    crunchOsc.frequency.linearRampToValueAtTime(10, now + 0.8);
    crunchFilter.type = 'lowpass';
    crunchFilter.frequency.setValueAtTime(2000, now);
    crunchFilter.frequency.exponentialRampToValueAtTime(100, now + 0.8);
    crunchGain.gain.setValueAtTime(0.7, now);
    crunchGain.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
    crunchOsc.connect(crunchFilter).connect(crunchGain).connect(this.out());
    crunchOsc.start(now);
    crunchOsc.stop(now + 0.9);

    // High pitched crackle / thunderclap (Noise burst)
    const bufferSize = ctx.sampleRate * 2;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 3); // natural decay in buffer
    }
    const noise = ctx.createBufferSource();
    noise.buffer = buffer;
    
    // Filter the noise to sound like a heavy thunder strike
    const noiseFilter = ctx.createBiquadFilter();
    noiseFilter.type = 'bandpass';
    noiseFilter.frequency.setValueAtTime(800, now);
    noiseFilter.frequency.exponentialRampToValueAtTime(100, now + 1.5);
    noiseFilter.Q.value = 0.5;

    const noiseGain = ctx.createGain();
    noiseGain.gain.setValueAtTime(1.5, now);
    noiseGain.gain.exponentialRampToValueAtTime(0.001, now + 2);

    noise.connect(noiseFilter).connect(noiseGain).connect(this.out());
    noise.start(now);
  }

  // ─── Panel slide in ───
  panelIn(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(300, now);
    osc.frequency.exponentialRampToValueAtTime(900, now + 0.15);
    g.gain.setValueAtTime(0.15, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.2);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.2);
  }

  // ─── Button hover ───
  hover(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'square';
    osc.frequency.value = 1200;
    g.gain.setValueAtTime(0.06, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.04);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.04);
  }

  // ─── Button click ───
  click(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'square';
    osc.frequency.setValueAtTime(600, now);
    osc.frequency.exponentialRampToValueAtTime(200, now + 0.08);
    g.gain.setValueAtTime(0.2, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.12);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.12);

    // Echo tick
    const osc2 = ctx.createOscillator();
    const g2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.value = 400;
    g2.gain.setValueAtTime(0, now);
    g2.gain.setValueAtTime(0.08, now + 0.06);
    g2.gain.exponentialRampToValueAtTime(0.001, now + 0.14);
    osc2.connect(g2).connect(this.out());
    osc2.start(now + 0.06);
    osc2.stop(now + 0.15);
  }

  // ─── Panel switch (login ↔ register) ───
  switchPanel(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;

    // Swoosh
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(200, now);
    osc.frequency.exponentialRampToValueAtTime(800, now + 0.1);
    osc.frequency.exponentialRampToValueAtTime(500, now + 0.2);
    g.gain.setValueAtTime(0.12, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.22);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.22);

    // Bright ding
    const osc2 = ctx.createOscillator();
    const g2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.value = 1047; // C6
    g2.gain.setValueAtTime(0.12, now + 0.08);
    g2.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
    osc2.connect(g2).connect(this.out());
    osc2.start(now + 0.08);
    osc2.stop(now + 0.25);
  }

  // ─── Password requirement met (ding) ───
  check(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, now);
    osc.frequency.setValueAtTime(1318, now + 0.04);
    g.gain.setValueAtTime(0.15, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.1);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.1);
  }

  // ─── Lightning / thunder ───
  thunder(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;

    // Electric zap
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(2400, now);
    osc.frequency.exponentialRampToValueAtTime(80, now + 0.25);
    g.gain.setValueAtTime(0.4, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.5);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.5);

    // Crackle noise
    this.playNoise(now, 0.3, 0.35);

    // Sub rumble
    const osc2 = ctx.createOscillator();
    const g2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(60, now + 0.1);
    osc2.frequency.exponentialRampToValueAtTime(25, now + 0.6);
    g2.gain.setValueAtTime(0.3, now + 0.1);
    g2.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
    osc2.connect(g2).connect(this.out());
    osc2.start(now + 0.1);
    osc2.stop(now + 0.6);
  }

  // ─── Terminal beep (each line typed) ───
  terminal(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'square';
    osc.frequency.value = 740;
    g.gain.setValueAtTime(0.08, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.06);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.06);
  }

  // ─── Access granted fanfare ───
  granted(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const notes = [523, 659, 784, 1047]; // C5, E5, G5, C6

    notes.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      osc.type = 'square';
      osc.frequency.value = freq;
      const t = now + i * 0.1;
      g.gain.setValueAtTime(0.18, t);
      g.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
      osc.connect(g).connect(this.out());
      osc.start(t);
      osc.stop(t + 0.15);
    });

    // Final shimmer
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.value = 1568; // G6
    const t = now + 0.4;
    g.gain.setValueAtTime(0.12, t);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
    osc.connect(g).connect(this.out());
    osc.start(t);
    osc.stop(t + 0.3);
  }

  // ─── Error buzz ───
  error(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'square';
    osc.frequency.setValueAtTime(220, now);
    osc.frequency.exponentialRampToValueAtTime(80, now + 0.25);
    g.gain.setValueAtTime(0.2, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.25);

    // Second buzz
    const osc2 = ctx.createOscillator();
    const g2 = ctx.createGain();
    osc2.type = 'square';
    osc2.frequency.setValueAtTime(180, now + 0.12);
    osc2.frequency.exponentialRampToValueAtTime(60, now + 0.3);
    g2.gain.setValueAtTime(0.15, now + 0.12);
    g2.gain.exponentialRampToValueAtTime(0.001, now + 0.3);
    osc2.connect(g2).connect(this.out());
    osc2.start(now + 0.12);
    osc2.stop(now + 0.3);
  }

  // ─── Pikachu happy "Pika!" ───
  pikaHappy(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;

    // "Pi-" ascending
    const osc1 = ctx.createOscillator();
    const g1 = ctx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(800, now);
    osc1.frequency.exponentialRampToValueAtTime(1600, now + 0.08);
    g1.gain.setValueAtTime(0.2, now);
    g1.gain.exponentialRampToValueAtTime(0.05, now + 0.08);
    osc1.connect(g1).connect(this.out());
    osc1.start(now);
    osc1.stop(now + 0.09);

    // "-ka!" bright
    const osc2 = ctx.createOscillator();
    const g2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(1400, now + 0.1);
    osc2.frequency.exponentialRampToValueAtTime(1800, now + 0.16);
    g2.gain.setValueAtTime(0.22, now + 0.1);
    g2.gain.exponentialRampToValueAtTime(0.001, now + 0.22);
    osc2.connect(g2).connect(this.out());
    osc2.start(now + 0.1);
    osc2.stop(now + 0.22);
  }

  // ─── Pikachu covers eyes ───
  pikaCover(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(600, now);
    osc.frequency.exponentialRampToValueAtTime(300, now + 0.12);
    g.gain.setValueAtTime(0.12, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.15);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.15);
  }

  // ─── Typing tick (soft) ───
  typeTick(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'square';
    osc.frequency.value = 440 + Math.random() * 200;
    g.gain.setValueAtTime(0.03, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.02);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.02);
  }

  // ─── Register success jingle ───
  success(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const notes = [659, 784, 988, 1319]; // E5, G5, B5, E6

    notes.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.value = freq;
      const t = now + i * 0.08;
      g.gain.setValueAtTime(0.16, t);
      g.gain.exponentialRampToValueAtTime(0.001, t + 0.18);
      osc.connect(g).connect(this.out());
      osc.start(t);
      osc.stop(t + 0.18);
    });
  }

  // ─── Checkbox toggle ───
  checkbox(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(500, now);
    osc.frequency.setValueAtTime(800, now + 0.03);
    g.gain.setValueAtTime(0.12, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.08);
    osc.connect(g).connect(this.out());
    osc.start(now);
    osc.stop(now + 0.08);
  }

  // ── Noise helper ──
  private playNoise(startTime: number, duration: number, gain: number): void {
    const ctx = this.getCtx();
    const bufferSize = ctx.sampleRate * duration;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 2);
    }
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    const g = ctx.createGain();
    g.gain.setValueAtTime(gain, startTime);
    g.gain.exponentialRampToValueAtTime(0.001, startTime + duration);
    source.connect(g).connect(this.out());
    source.start(startTime);
    source.stop(startTime + duration);
  }

  // ═══════════════════════════════════════
  //  EPIC BACKGROUND MUSIC — Cinematic Storm Drone
  // ═══════════════════════════════════════
  private musicPlaying = false;
  private musicGain: GainNode | null = null;
  private droneNodes: (OscillatorNode | AudioBufferSourceNode)[] = [];
  private droneGains: GainNode[] = [];
  private sparkTimer: ReturnType<typeof setTimeout> | null = null;

  get isMusicPlaying(): boolean { return this.musicPlaying; }

  startMusic(skipBurst: boolean = false): void {
    if (this.musicPlaying) return;
    this.musicPlaying = true;
    localStorage.setItem('ptcg_music', '1');
    
    // Play the massive thunder burst when turning music on, so the user 
    // always hears it when they enable the audio.
    if (!skipBurst) {
      this.burst();
    }

    this.startEpicDrone();
    this.scheduleSparks();
  }

  stopMusic(): void {
    this.musicPlaying = false;
    localStorage.setItem('ptcg_music', '0');
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    
    if (this.sparkTimer) {
      clearTimeout(this.sparkTimer);
      this.sparkTimer = null;
    }

    // Fade out smoothly
    if (this.musicGain) {
      this.musicGain.gain.setValueAtTime(this.musicGain.gain.value, now);
      this.musicGain.gain.exponentialRampToValueAtTime(0.001, now + 2);
    }

    setTimeout(() => {
      this.droneNodes.forEach(n => { try { n.stop(); } catch {} });
      this.droneNodes = [];
      this.droneGains = [];
      if (this.musicGain) {
        this.musicGain.disconnect();
        this.musicGain = null;
      }
    }, 2100);
  }

  toggleMusic(): boolean {
    if (this.musicPlaying) { this.stopMusic(); } else { this.startMusic(); }
    return this.musicPlaying;
  }

  private async startEpicDrone(): Promise<void> {
    const ctx = this.getCtx();
    const now = ctx.currentTime;

    if (!this.musicGain) {
      this.musicGain = ctx.createGain();
      this.musicGain.gain.setValueAtTime(0, now);
      this.musicGain.gain.linearRampToValueAtTime(0.8, now + 3); // Slow fade in
      this.musicGain.connect(this.out());
    }

    // Attempt to load a custom MP3 first
    try {
      const response = await fetch('/assets/login-bgm.mp3');
      const contentType = response.headers.get('content-type');
      
      // If Angular serves index.html (fallback) instead of audio, it's not the MP3
      if (response.ok && contentType && !contentType.includes('text/html')) {
        const arrayBuffer = await response.arrayBuffer();
        const audioBuffer = await ctx.decodeAudioData(arrayBuffer);
        const source = ctx.createBufferSource();
        source.buffer = audioBuffer;
        source.loop = true;
        source.connect(this.musicGain);
        source.start(now);
        this.droneNodes.push(source);
        console.log("Custom MP3 loaded successfully!");
        return; // Success! Skip procedural drone.
      } else if (response.ok) {
        console.warn("Found a file at /assets/login-bgm.mp3, but it looks like an HTML fallback. Content-Type:", contentType);
      }
    } catch (e) {
      console.warn("No se pudo cargar el MP3 personalizado, usando fondo por defecto.", e);
    }

    // 1. Deep Sub Bass Rumble (Sine)
    const subOsc = ctx.createOscillator();
    const subGain = ctx.createGain();
    subOsc.type = 'sine';
    subOsc.frequency.value = 41.20; // E1 - very low
    subGain.gain.value = 0.5;
    
    // LFO for sub bass (creates a throbbing/rumbling effect)
    const subLfo = ctx.createOscillator();
    const subLfoGain = ctx.createGain();
    subLfo.type = 'sine';
    subLfo.frequency.value = 0.5; // 0.5 Hz throb
    subLfoGain.gain.value = 0.3;
    subLfo.connect(subLfoGain);
    subLfoGain.connect(subGain.gain);
    
    subOsc.connect(subGain).connect(this.musicGain);
    subOsc.start(now);
    subLfo.start(now);
    this.droneNodes.push(subOsc, subLfo);
    this.droneGains.push(subGain);

    // 2. Dark Detuned Pads (Sawtooth + Lowpass Filter)
    const filter = ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 250; // Keep it dark and brooding
    filter.Q.value = 1;
    filter.connect(this.musicGain);

    // Filter LFO to slowly open and close the filter (breathing effect)
    const filterLfo = ctx.createOscillator();
    const filterLfoGain = ctx.createGain();
    filterLfo.type = 'sine';
    filterLfo.frequency.value = 0.05; // Very slow (20s cycle)
    filterLfoGain.gain.value = 150; // Modulate cutoff by ±150Hz
    filterLfo.connect(filterLfoGain);
    filterLfoGain.connect(filter.frequency);
    filterLfo.start(now);
    this.droneNodes.push(filterLfo);

    // The pad chords (E minor: E2, G2, B2)
    const padFreqs = [82.41, 98.00, 123.47];
    padFreqs.forEach(freq => {
      // Main oscillator
      const osc1 = ctx.createOscillator();
      osc1.type = 'sawtooth';
      osc1.frequency.value = freq;
      
      // Detuned oscillator for width/thickness
      const osc2 = ctx.createOscillator();
      osc2.type = 'sawtooth';
      osc2.frequency.value = freq * 1.005; // Slight detune

      const pGain = ctx.createGain();
      pGain.gain.value = 0.15; // Soft volume

      osc1.connect(pGain);
      osc2.connect(pGain);
      pGain.connect(filter);
      
      osc1.start(now);
      osc2.start(now);
      this.droneNodes.push(osc1, osc2);
    });

    // 3. Constant Storm/Wind Noise
    const bufferSize = ctx.sampleRate * 5; // 5 seconds of noise (looped)
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      // Pink noise approximation for a warmer, rushing wind sound
      data[i] = (Math.random() * 2 - 1) * 0.5; 
    }
    const noiseSource = ctx.createBufferSource();
    noiseSource.buffer = buffer;
    noiseSource.loop = true;
    
    // Filter the noise to sound like muffled wind
    const noiseFilter = ctx.createBiquadFilter();
    noiseFilter.type = 'lowpass';
    noiseFilter.frequency.value = 400;
    
    const noiseGain = ctx.createGain();
    noiseGain.gain.value = 0.15;
    
    // Modulate wind volume with an LFO for gusts
    const windLfo = ctx.createOscillator();
    const windLfoGain = ctx.createGain();
    windLfo.type = 'sine';
    windLfo.frequency.value = 0.1; // 10s gusts
    windLfoGain.gain.value = 0.1;
    windLfo.connect(windLfoGain);
    windLfoGain.connect(noiseGain.gain);

    noiseSource.connect(noiseFilter).connect(noiseGain).connect(this.musicGain);
    noiseSource.start(now);
    windLfo.start(now);
    this.droneNodes.push(noiseSource, windLfo);
  }

  // ── Random electric sparks generator ──
  private scheduleSparks(): void {
    if (!this.musicPlaying) return;

    // Random interval between 2s and 6s
    const nextSparkMs = 2000 + Math.random() * 4000;
    this.sparkTimer = setTimeout(() => {
      if (this.musicPlaying) {
        this.playRandomSpark();
        this.scheduleSparks();
      }
    }, nextSparkMs);
  }

  private playRandomSpark(): void {
    const ctx = this.getCtx();
    const now = ctx.currentTime;
    
    // SSJ2 sparks often come in quick clusters of 1 to 3 "zaps"
    const zapCount = Math.floor(Math.random() * 3) + 1;
    const panner = ctx.createStereoPanner();
    panner.pan.value = (Math.random() * 1.6) - 0.8; // Random left/right pan
    panner.connect(this.out());

    for (let i = 0; i < zapCount; i++) {
      const offset = i * (0.05 + Math.random() * 0.05); // Rapid succession (50-100ms apart)
      const t = now + offset;

      // 1. Sharp buzzy sawtooth for the "electric" body
      const osc = ctx.createOscillator();
      const oscGain = ctx.createGain();
      osc.type = 'sawtooth';
      
      // SSJ2 zap pitch drop (very sharp, starts high, drops fast)
      const startFreq = 2000 + Math.random() * 3000;
      osc.frequency.setValueAtTime(startFreq, t);
      osc.frequency.exponentialRampToValueAtTime(150, t + 0.08); // Drops very fast to low freq
      
      oscGain.gain.setValueAtTime(0, t);
      oscGain.gain.linearRampToValueAtTime(0.08, t + 0.01); // Sharp attack
      oscGain.gain.exponentialRampToValueAtTime(0.001, t + 0.08); // Sharp decay

      // 2. High-frequency noise for the crackle/static
      const bufferSize = ctx.sampleRate * 0.1; // 100ms
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let j = 0; j < bufferSize; j++) {
        data[j] = (Math.random() * 2 - 1) * Math.pow(1 - j / bufferSize, 2); 
      }
      const noise = ctx.createBufferSource();
      noise.buffer = buffer;

      const noiseFilter = ctx.createBiquadFilter();
      noiseFilter.type = 'bandpass';
      noiseFilter.frequency.value = 3500 + Math.random() * 1000;
      noiseFilter.Q.value = 1.5;

      const noiseGain = ctx.createGain();
      noiseGain.gain.setValueAtTime(0, t);
      noiseGain.gain.linearRampToValueAtTime(0.15, t + 0.01);
      noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.08);

      // Connect and start
      osc.connect(oscGain).connect(panner);
      noise.connect(noiseFilter).connect(noiseGain).connect(panner);
      
      osc.start(t);
      osc.stop(t + 0.09);
      noise.start(t);
    }
  }
}
