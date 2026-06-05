// Ciclo 1: colecciones OFICIALES de diseño → workflow_activities, workflow_transitions
// Runtime → tramites, form_submissions. Ver docs/ciclo1-modelo-workflow.md
// F10.2: índices recomendados (createIndex es idempotente en instalaciones existentes)

const db = db.getSiblingDB('workflow_db');

const collections = [
  'workflow_activities',
  'workflow_transitions',
  'tramites',
  'users',
  'roles',
  'permissions',
  'departments',
  'business_policies',
  'workflow_diagrams',
  'activities',
  'transitions',
  'dynamic_forms',
  'form_fields',
  'process_instances',
  'task_instances',
  'audit_logs',
  'bitacora',
  'form_submissions',
  'kpi_reports',
  'ai_requests',
  'workflow_collaboration',
];

collections.forEach((name) => {
  try {
    db.createCollection(name);
  } catch (e) {
    // ya existe
  }
});

function ensureIndex(coll, keys, options) {
  try {
    db.getCollection(coll).createIndex(keys, Object.assign({ background: true }, options || {}));
  } catch (e) {
    print('Index skip ' + coll + ': ' + e);
  }
}

// Trámites — consultas bandeja, monitor, KPI
ensureIndex('tramites', { policyId: 1 });
ensureIndex('tramites', { status: 1 });
ensureIndex('tramites', { createdAt: -1 });
ensureIndex('tramites', { policyId: 1, status: 1 });
ensureIndex('tramites', { 'tasks.status': 1 });
ensureIndex('tramites', { 'tasks.workflowActivityId': 1 });
// Asignación en runtime vía responsible/takenBy (no hay assignedUserId en el modelo)
ensureIndex('tramites', { 'tasks.responsible': 1 });
ensureIndex('tramites', { 'tasks.takenBy': 1 });

// Diseño UML
ensureIndex('workflow_activities', { policyId: 1 });
ensureIndex('workflow_activities', { policyId: 1, active: 1 });
ensureIndex('workflow_transitions', { policyId: 1 });
ensureIndex('workflow_transitions', { policyId: 1, active: 1 });

// Formularios y auditoría
ensureIndex('form_submissions', { tramiteId: 1 });
ensureIndex('form_submissions', { workflowActivityId: 1 });
ensureIndex('form_submissions', { tramiteId: 1, workflowActivityId: 1 });
ensureIndex('bitacora', { createdAt: -1 });
ensureIndex('bitacora', { module: 1, createdAt: -1 });

print('Ciclo 1 — colecciones e índices F10.2 aplicados en workflow_db');
