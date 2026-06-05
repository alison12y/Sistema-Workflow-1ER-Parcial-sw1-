import { Injectable } from '@angular/core';

export interface ContextualHelp {
  title: string;
  message: string;
  tips?: string[];
}

@Injectable({ providedIn: 'root' })
export class ContextualAssistantService {
  getHelpForUrl(url: string): ContextualHelp {
    const path = url.split('?')[0].split('#')[0];

    if (path.startsWith('/mis-actividades/') && path.endsWith('/form')) {
      return this.help(
        'Formulario de ejecución',
        'Complete los datos reales del trámite. Use Asistencia IA con informe libre o dictado por voz; revise y aplique sugerencias antes de enviar.',
        [
          'Campos obligatorios (*) deben completarse antes de finalizar.',
          'Asistir formulario sugiere valores y extrae fechas del informe.',
          'Los campos FILE no se autocompletan: adjunte archivos manualmente.',
          'Al completar, el motor enruta automáticamente; no use Avanzar manual salvo depuración.',
        ]
      );
    }

    if (path.startsWith('/workflow-designer/')) {
      return this.help(
        'Editor de diagramas UML 2.5',
        'Diseñe flujos con START, TASK, DECISION, END, swimlanes y transiciones secuenciales, condicionales, iterativas y paralelas (división/unión).',
        [
          'Asistente IA: prompt por texto o dictado por voz → Generar sugerencia → vista previa → Aplicar.',
          'Ejemplo: "Crear workflow de permiso laboral con carriles Funcionario, RRHH y Supervisor".',
          'Valide el flujo antes de activar la política.',
          'Colaboración: vea usuarios conectados, última modificación y recargue si hay conflicto.',
        ]
      );
    }

    if (/^\/activities\/[^/]+\/form$/.test(path) || path.startsWith('/form-designer/')) {
      return this.help(
        'Diseñador de formulario',
        'Defina la plantilla dinámica de la actividad: texto, número, fecha, checkbox, selección y archivo.',
        [
          'Marque campos obligatorios y asigne nombre técnico (variable).',
          'El nombre técnico se usa en condiciones del workflow (ej. valido == true).',
          'Guarde la plantilla asociada a la actividad TASK del diagrama.',
        ]
      );
    }

    if (/^\/tramites\/[^/]+$/.test(path)) {
      return this.help(
        'Detalle de trámite',
        'Consulte estado, actividad actual, responsable, historial, respuestas de formularios y trazas.',
        [
          'Puede cancelar trámites según permisos.',
          'Solo elimine trámites CANCELADOS o COMPLETADOS; no elimine EN_PROCESO.',
          'Use Mis tareas para completar actividades pendientes.',
        ]
      );
    }

    const exact: Record<string, ContextualHelp> = {
      '/login': this.help(
        'Inicio de sesión',
        'Ingresa tus credenciales para acceder al sistema de gestión de políticas.',
        ['Use el usuario y contraseña proporcionados por el administrador.']
      ),
      '/dashboard': this.help(
        'Dashboard',
        'Panel principal con acceso a políticas, diseñador UML, trámites, bandeja, monitoreo y KPIs.',
        [
          'Active políticas antes de iniciar trámites.',
          'La IA está integrada en Diseñador workflow y Formulario de ejecución.',
          'Consulte Guía asistente IA en el menú para ver todas las funcionalidades.',
        ]
      ),
      '/policies': this.help(
        'Políticas de negocio',
        'Cree y administre políticas que agrupan diagrama UML, formularios por actividad y trámites.',
        [
          'Diseñe workflow UML 2.5 con swimlanes y valide el flujo.',
          'Configure formularios dinámicos por actividad TASK.',
          'Active la política para permitir nuevos trámites.',
        ]
      ),
      '/tramites': this.help(
        'Trámites',
        'Inicie y gestione trámites basados en políticas activas.',
        [
          'Inicie trámite → el motor crea tareas según el diagrama.',
          'Cancelar trámite según permisos.',
          'Eliminar solo trámites CANCELADOS o COMPLETADOS.',
        ]
      ),
      '/mis-actividades': this.help(
        'Mis tareas (bandeja)',
        'Bandeja del funcionario: tareas pendientes, en curso y finalizadas con asignación por usuario, rol o departamento.',
        [
          'Tome la tarea antes de completar si está pendiente.',
          'Completar formulario → Asistencia IA con informe o dictado por voz.',
          'El motor avanza automáticamente; no elija la ruta manualmente.',
        ]
      ),
      '/monitoring': this.help(
        'Monitoreo',
        'Seguimiento en tiempo real: estado actual, actividad, responsable, historial y respuestas de formularios.',
        [
          'Consulte trazas: TRAMITE_CREADO, TAREA_TOMADA, ACTIVIDAD_COMPLETADA, etc.',
          'La vista se actualiza automáticamente sin recargar la página.',
          'Filtre por política o estado del trámite.',
        ]
      ),
      '/kpis': this.help(
        'KPIs / Cuellos de botella',
        'Indicadores: trámites activos/finalizados, tiempos promedio, carga por funcionario y departamento.',
        [
          'Detecte cuellos de botella por actividad con mayor demora.',
          'Relacione con estimatedTimeHours definido en el diseñador.',
          'Filtre por política para análisis focalizado.',
        ]
      ),
      '/users': this.help(
        'Usuarios',
        'Administra usuarios del sistema.',
        ['Asigne roles y departamentos.', 'Desactive cuentas que ya no se utilicen.']
      ),
      '/roles': this.help(
        'Roles',
        'Define roles y permisos de acceso.',
        ['Configure permisos por módulo.', 'Asocie roles a usuarios desde Usuarios.']
      ),
      '/departments': this.help(
        'Departamentos',
        'Administra áreas o departamentos responsables.',
        ['Defina responsables por área.', 'Use departamentos en carriles del diagrama UML.']
      ),
      '/bitacora': this.help(
        'Bitácora',
        'Registro de acciones: diseño, colaboración, IA (resumen) y operaciones del sistema.',
        ['Revise quién realizó cada acción.', 'Filtre por módulo o fecha.']
      ),
      '/settings': this.help(
        'Configuración',
        'Preferencias locales: tema y notificaciones.',
        ['El tema se aplica de inmediato.', 'Las preferencias se guardan en este navegador.']
      ),
      '/ai-assistant': this.help(
        'Guía asistente IA',
        'Referencia de funcionalidades: diseño UML con IA, formularios dinámicos, ejecución, monitoreo, KPIs y colaboración.',
        [
          'IA en diseñador: texto, voz, vista previa antes de aplicar.',
          'IA en formularios: informe libre, dictado, sugerencias (FILE manual).',
          'Ciclo 2 no incluido: S3, Flutter, offline, predictivo ni reportes dinámicos.',
        ]
      ),
      '/seguimiento': this.help(
        'Seguimiento de trámites',
        'Vista alternativa de trazabilidad y estado de trámites en ejecución.',
        [
          'Consulte actividad actual y responsable.',
          'Actualización automática sin recargar la página.',
        ]
      ),
    };

    return exact[path] ?? this.help(
      'Sistema Workflow',
      'Navegue por el menú para políticas, diseñador UML, trámites, bandeja, monitoreo y KPIs.',
      ['Use este botón Ayuda en cualquier pantalla para orientación contextual.']
    );
  }

  private help(title: string, message: string, tips: string[] = []): ContextualHelp {
    return { title, message, tips };
  }
}
