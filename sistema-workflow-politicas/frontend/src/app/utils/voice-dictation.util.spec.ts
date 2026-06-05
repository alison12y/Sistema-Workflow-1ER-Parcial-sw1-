import {
  appendDictationText,
  extractTranscript,
  isVoiceDictationSupported,
  mapSpeechRecognitionError,
} from './voice-dictation.util';

describe('voice-dictation.util', () => {
  it('appendDictationText appends without replacing existing prompt', () => {
    expect(appendDictationText('Crear workflow', 'con tres carriles')).toBe(
      'Crear workflow con tres carriles',
    );
    expect(appendDictationText('', 'solo dictado')).toBe('solo dictado');
  });

  it('mapSpeechRecognitionError returns Spanish messages', () => {
    expect(mapSpeechRecognitionError('not-allowed')).toBe('Permiso de micrófono denegado.');
    expect(mapSpeechRecognitionError('no-speech')).toContain('No se detectó voz');
  });

  it('extractTranscript reads final speech results', () => {
    const event = {
      resultIndex: 0,
      results: [
        {
          isFinal: true,
          0: { transcript: 'Crear flujo de permiso laboral' },
        },
      ],
    };
    expect(extractTranscript(event)).toBe('Crear flujo de permiso laboral');
  });

  it('extractTranscript falls back to interim results when no final chunk exists', () => {
    const event = {
      resultIndex: 0,
      results: [
        {
          isFinal: false,
          0: { transcript: 'crear flujo de permiso laboral' },
        },
      ],
    };
    expect(extractTranscript(event)).toBe('crear flujo de permiso laboral');
  });

  it('isVoiceDictationSupported returns a boolean', () => {
    expect(typeof isVoiceDictationSupported()).toBe('boolean');
  });
});
