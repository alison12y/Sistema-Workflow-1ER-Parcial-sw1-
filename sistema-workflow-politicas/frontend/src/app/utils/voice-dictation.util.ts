/** Web Speech API — dictado por voz (CU14, sin API externa). */

export type SpeechRecognitionErrorCode =
  | 'no-speech'
  | 'aborted'
  | 'audio-capture'
  | 'network'
  | 'not-allowed'
  | 'service-not-allowed'
  | 'bad-grammar'
  | 'language-not-supported'
  | string;

export interface SpeechRecognitionResultLike {
  isFinal: boolean;
  [index: number]: { transcript: string };
}

export interface SpeechRecognitionEventLike {
  results: ArrayLike<SpeechRecognitionResultLike>;
  resultIndex: number;
}

export interface SpeechRecognitionInstance {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: { error: SpeechRecognitionErrorCode }) => void) | null;
  onend: (() => void) | null;
  onstart: (() => void) | null;
  start: () => void;
  stop: () => void;
  abort: () => void;
}

type SpeechRecognitionConstructor = new () => SpeechRecognitionInstance;

export interface VoiceDictationHandlers {
  onTranscript: (text: string) => void;
  onListeningChange: (listening: boolean) => void;
  onStatus?: (message: string) => void;
  onError?: (message: string) => void;
  /** Logs temporales de depuración (CU14). */
  onDebug?: (event: 'start' | 'result' | 'error' | 'end', detail?: string) => void;
}

export function isVoiceDictationSupported(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const w = window as unknown as {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  };
  return !!(w.SpeechRecognition ?? w.webkitSpeechRecognition);
}

export function mapSpeechRecognitionError(error: SpeechRecognitionErrorCode): string {
  switch (error) {
    case 'not-allowed':
    case 'service-not-allowed':
      return 'Permiso de micrófono denegado.';
    case 'no-speech':
      return 'No se detectó voz. Intente de nuevo hablando más cerca del micrófono.';
    case 'audio-capture':
      return 'Micrófono no disponible. Verifique que esté conectado y no lo use otra aplicación.';
    case 'aborted':
      return 'Dictado detenido.';
    case 'network':
      return 'Error de red durante el reconocimiento de voz.';
    case 'language-not-supported':
      return 'El idioma español no está soportado en este navegador.';
    default:
      return `No se pudo completar el dictado (${error}).`;
  }
}

export function extractTranscript(event: SpeechRecognitionEventLike): string {
  const finals: string[] = [];
  let latestInterim = '';

  for (let i = event.resultIndex; i < event.results.length; i++) {
    const result = event.results[i];
    const chunk = result?.[0]?.transcript?.trim();
    if (!chunk) {
      continue;
    }
    if (result.isFinal) {
      finals.push(chunk);
    } else {
      latestInterim = chunk;
    }
  }

  if (finals.length > 0) {
    return finals.join(' ');
  }
  if (latestInterim) {
    return latestInterim;
  }

  for (let i = event.results.length - 1; i >= 0; i--) {
    const chunk = event.results[i]?.[0]?.transcript?.trim();
    if (chunk) {
      return chunk;
    }
  }
  return '';
}

export function appendDictationText(current: string, dictated: string): string {
  const next = dictated.trim();
  if (!next) {
    return current;
  }
  const base = current.trim();
  return base ? `${base} ${next}` : next;
}

export class VoiceDictationController {
  private recognition: SpeechRecognitionInstance | null = null;
  private listening = false;
  private stoppedManually = false;
  private pendingInterimTranscript = '';
  private transcriptDelivered = false;

  constructor(private readonly handlers: VoiceDictationHandlers) {}

  get isListening(): boolean {
    return this.listening;
  }

  start(): void {
    if (!isVoiceDictationSupported()) {
      this.handlers.onError?.('El navegador no soporta dictado por voz.');
      return;
    }

    if (this.listening) {
      return;
    }

    const previous = this.recognition;
    if (previous) {
      this.stoppedManually = true;
      previous.abort();
    }

    this.stoppedManually = false;
    this.pendingInterimTranscript = '';
    this.transcriptDelivered = false;

    const w = window as unknown as {
      SpeechRecognition?: SpeechRecognitionConstructor;
      webkitSpeechRecognition?: SpeechRecognitionConstructor;
    };
    const Ctor = w.SpeechRecognition ?? w.webkitSpeechRecognition;
    if (!Ctor) {
      this.handlers.onError?.('El navegador no soporta dictado por voz.');
      return;
    }

    this.recognition = new Ctor();
    this.recognition.lang = 'es-ES';
    this.recognition.continuous = false;
    this.recognition.interimResults = true;

    this.recognition.onstart = () => {
      this.handlers.onDebug?.('start');
      this.listening = true;
      this.handlers.onListeningChange(true);
      this.handlers.onStatus?.('Dictado iniciado. Hable ahora.');
      this.handlers.onStatus?.('Escuchando...');
    };

    this.recognition.onresult = (event) => {
      const transcript = extractTranscript(event);
      this.handlers.onDebug?.('result', transcript || '(vacío)');
      if (transcript) {
        this.pendingInterimTranscript = transcript;
      }
      if (transcript && this.hasFinalResult(event)) {
        this.deliverTranscript(transcript);
      }
    };

    this.recognition.onerror = (event) => {
      this.handlers.onDebug?.('error', event.error);
      if (event.error === 'aborted' && this.stoppedManually) {
        return;
      }
      if (event.error === 'no-speech' && this.flushPendingTranscript()) {
        this.setListening(false);
        return;
      }
      this.handlers.onError?.(mapSpeechRecognitionError(event.error));
      this.setListening(false);
    };

    this.recognition.onend = () => {
      this.handlers.onDebug?.('end');
      this.flushPendingTranscript();
      this.setListening(false);
    };

    try {
      this.recognition.start();
    } catch {
      this.handlers.onError?.('No se pudo iniciar el dictado. Intente de nuevo.');
      this.setListening(false);
    }
  }

  stop(): void {
    this.stoppedManually = true;
    this.recognition?.stop();
    this.setListening(false);
    this.handlers.onStatus?.('Dictado detenido.');
  }

  abort(): void {
    this.stoppedManually = true;
    this.recognition?.abort();
    this.recognition = null;
    this.setListening(false);
  }

  private setListening(value: boolean): void {
    this.listening = value;
    this.handlers.onListeningChange(value);
  }

  private hasFinalResult(event: SpeechRecognitionEventLike): boolean {
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i]?.isFinal) {
        return true;
      }
    }
    return false;
  }

  private deliverTranscript(transcript: string): void {
    const text = transcript.trim();
    if (!text || this.transcriptDelivered) {
      return;
    }
    this.transcriptDelivered = true;
    this.pendingInterimTranscript = '';
    this.handlers.onTranscript(text);
    this.handlers.onStatus?.('Texto reconocido correctamente.');
  }

  private flushPendingTranscript(): boolean {
    const text = this.pendingInterimTranscript.trim();
    if (!text || this.transcriptDelivered) {
      return false;
    }
    this.deliverTranscript(text);
    return true;
  }
}
