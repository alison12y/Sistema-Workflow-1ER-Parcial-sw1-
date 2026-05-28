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
        'Llena los datos reales del trámite. Puedes usar Ayuda IA para sugerir valores en los campos.',
        [
          'Complete los campos obligatorios marcados con *.',
          'Use el botón Ayuda IA para autocompletar campos vacíos.',
          'Guarde avance si necesita continuar más tarde.',
        ]
      );
    }

    if (path.startsWith('/workflow-designer/')) {
      return this.help(
        'Editor de diagramas UML 2.5',
        'Diseña el Diagrama de Actividades UML 2.5 de la política. Usa carriles para organizar responsables y utiliza la IA para sugerir flujos.',
        [
          'Organice actividades en calles/carriles por responsable.',
          'Use Sugerir con IA para generar un flujo base.',
          'Guarde el diagrama antes de salir.',
        ]
      );
    }

    if (path.startsWith('/form-designer/')) {
      return this.help(
        'Diseñador de formulario',
        'Crea la plantilla del formulario que será utilizada durante la ejecución de una actividad.',
        [
          'Defina campos según la actividad del diagrama.',
          'Marque como obligatorios los campos necesarios.',
          'Guarde la plantilla asociada a la actividad.',
        ]
      );
    }

    if (/^\/tramites\/[^/]+$/.test(path)) {
      return this.help(
        'Detalle de trámite',
        'Consulta el estado, la traza y las actividades del trámite seleccionado.',
        ['Revise el progreso y las actividades completadas.', 'Use Mis actividades para completar tareas pendientes.']
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
        'Bienvenido al panel principal. Desde aquí puedes acceder a políticas, trámites, monitoreo, KPIs y actividades.',
        ['Use las tarjetas de acceso rápido para navegar.', 'Consulte Mis actividades para tareas pendientes.']
      ),
      '/policies': this.help(
        'Políticas',
        'En este módulo puedes crear, editar y activar políticas de negocio.',
        ['Active una política antes de iniciar trámites.', 'Diseñe el diagrama UML y formularios por actividad.']
      ),
      '/tramites': this.help(
        'Trámites',
        'Inicia y gestiona trámites basados en políticas activas.',
        ['Seleccione una política activa al crear un trámite.', 'Consulte el detalle para ver el avance.']
      ),
      '/mis-actividades': this.help(
        'Mis actividades',
        'Consulta las tareas asignadas y completa los formularios correspondientes.',
        ['Presione Completar formulario para llenar datos del trámite.', 'Use Ayuda IA dentro del formulario.']
      ),
      '/monitoring': this.help(
        'Monitoreo',
        'Realiza seguimiento a los trámites en ejecución y consulta su traza.',
        ['Filtre por estado o política.', 'Identifique trámites detenidos o retrasados.']
      ),
      '/kpis': this.help(
        'KPIs',
        'Consulta indicadores y cuellos de botella del workflow.',
        ['Revise tiempos promedio por actividad.', 'Detecte etapas con mayor demora.']
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
        'Consulta el registro de acciones realizadas en el sistema.',
        ['Revise quién realizó cada acción.', 'Filtre por módulo o fecha.']
      ),
      '/settings': this.help(
        'Configuración',
        'Configura preferencias locales como tema y notificaciones.',
        ['El tema se aplica de inmediato.', 'Las preferencias se guardan en este navegador.']
      ),
      '/ai-assistant': this.help(
        'Asistente IA',
        'Usa el asistente para obtener ayuda sobre políticas, diagramas, formularios y procesos.',
        [
          'La IA integrada está en el Diseñador UML y Formulario de ejecución.',
          'No requiere conexión externa ni API keys.',
        ]
      ),
    };

    return exact[path] ?? this.help(
      'Sistema Workflow',
      'Navegue por el menú lateral para acceder a los módulos del sistema de gestión de políticas.',
      ['Use este asistente en cualquier pantalla para obtener ayuda contextual.']
    );
  }

  private help(title: string, message: string, tips: string[] = []): ContextualHelp {
    return { title, message, tips };
  }
}
