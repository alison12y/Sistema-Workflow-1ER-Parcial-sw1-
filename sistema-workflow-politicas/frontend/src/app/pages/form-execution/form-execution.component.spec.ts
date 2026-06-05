import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { FormExecutionComponent } from './form-execution.component';
import { MyActivitiesService } from '../../services/my-activities.service';
import { FormService } from '../../services/form.service';
import { FormSubmissionService } from '../../services/form-submission.service';
import { AuthService } from '../../services/auth.service';
import { AiService } from '../../services/ai.service';
import { AiAssistantService } from '../../services/ai-assistant.service';
import {
  appendDictationText,
  VoiceDictationController,
  VoiceDictationHandlers,
} from '../../utils/voice-dictation.util';

describe('FormExecutionComponent voice dictation', () => {
  let component: FormExecutionComponent;
  let fixture: ComponentFixture<FormExecutionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormExecutionComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => null },
              queryParamMap: { get: () => null },
            },
          },
        },
        {
          provide: MyActivitiesService,
          useValue: { getById: () => of(null) },
        },
        {
          provide: FormService,
          useValue: { getFormByActivity: () => of({ fields: [] }) },
        },
        {
          provide: FormSubmissionService,
          useValue: { getForTask: () => of(null) },
        },
        {
          provide: AuthService,
          useValue: { canExecuteTasks: () => true },
        },
        {
          provide: AiService,
          useValue: { assistForm: () => of({}) },
        },
        {
          provide: AiAssistantService,
          useValue: { getFormAssistAnswer: () => '' },
        },
        ChangeDetectorRef,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FormExecutionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function wireHandlers(target: FormExecutionComponent): VoiceDictationHandlers {
    return {
      onTranscript: (text) => {
        target.aiReport = appendDictationText(target.aiReport, text);
      },
      onListeningChange: (listening) => {
        target.voiceListening = listening;
      },
      onStatus: (message) => {
        target.voiceStatus = message;
        if (message !== 'Escuchando...') {
          target.aiError = '';
        }
      },
      onError: (message) => {
        target.aiError = message;
        target.voiceStatus = '';
      },
    };
  }

  it('voiceButtonLabel alternates between Dictar por voz and Detener dictado', () => {
    expect(component.voiceButtonLabel).toBe('Dictar por voz');
    component.voiceListening = true;
    expect(component.voiceButtonLabel).toBe('Detener dictado');
  });

  it('startVoiceDictation creates VoiceDictationController when supported', () => {
    if (!component.voiceSupported) {
      component.startVoiceDictation();
      expect(component.aiError).toBe('El navegador no soporta dictado por voz.');
      return;
    }

    component.startVoiceDictation();
    const controller = (component as unknown as { voiceDictation: VoiceDictationController | null })
      .voiceDictation;

    expect(controller).toBeTruthy();
    expect(controller).toBeInstanceOf(VoiceDictationController);
  });

  it('appends dictated text to aiReport without replacing previous content', () => {
    component.aiReport = 'Informe previo';
    wireHandlers(component).onTranscript('desde el 10 de junio hasta el 12 de junio');

    expect(component.aiReport).toBe(
      'Informe previo desde el 10 de junio hasta el 12 de junio',
    );
  });

  it('shows Spanish status messages used by VoiceDictationController', () => {
    const handlers = wireHandlers(component);

    handlers.onStatus?.('Dictado iniciado. Hable ahora.');
    expect(component.voiceStatus).toBe('Dictado iniciado. Hable ahora.');

    handlers.onStatus?.('Texto reconocido correctamente.');
    expect(component.voiceStatus).toBe('Texto reconocido correctamente.');
  });

  it('shows Spanish error messages used by VoiceDictationController', () => {
    const handlers = wireHandlers(component);

    handlers.onError?.('Permiso de micrófono denegado.');
    expect(component.aiError).toBe('Permiso de micrófono denegado.');
    expect(component.voiceStatus).toBe('');

    handlers.onError?.(
      'No se detectó voz. Intente de nuevo hablando más cerca del micrófono.',
    );
    expect(component.aiError).toContain('No se detectó voz');
  });

  it('stopVoiceDictation stops the controller', () => {
    const stop = jasmine.createSpy('stop');
    const abort = jasmine.createSpy('abort');
    (component as unknown as { voiceDictation: { stop: jasmine.Spy; abort: jasmine.Spy } }).voiceDictation =
      { stop, abort };
    component.voiceListening = true;

    component.stopVoiceDictation();

    expect(stop).toHaveBeenCalled();
    expect(component.voiceListening).toBeFalse();
    (component as unknown as { voiceDictation: null }).voiceDictation = null;
  });

  it('ngOnDestroy aborts the voice session', () => {
    const abort = jasmine.createSpy('abort');
    (component as unknown as { voiceDictation: { abort: jasmine.Spy } }).voiceDictation = { abort };

    component.ngOnDestroy();

    expect(abort).toHaveBeenCalled();
  });

  it('toggleVoiceDictation stops when already listening', () => {
    const stop = jasmine.createSpy('stop');
    const abort = jasmine.createSpy('abort');
    (component as unknown as { voiceDictation: { stop: jasmine.Spy; abort: jasmine.Spy } }).voiceDictation =
      { stop, abort };
    component.voiceListening = true;

    component.toggleVoiceDictation();

    expect(stop).toHaveBeenCalled();
    (component as unknown as { voiceDictation: null }).voiceDictation = null;
  });
});
