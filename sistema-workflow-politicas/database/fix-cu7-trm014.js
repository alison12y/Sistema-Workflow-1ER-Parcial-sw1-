// Corrección puntual CU7 — ejecutar: mongosh mongodb://localhost:27017/workflow_db fix-cu7-trm014.js

const formFieldId = ObjectId('6a21128a5fdb014b28a0dcc0');
const decisionFromId = '6a20e97b10588211828dc586';

print('=== Antes: campo formulario ===');
printjson(db.form_fields.findOne({ _id: formFieldId }));

print('=== Antes: transiciones desde Documentacion valida? ===');
db.workflow_transitions.find({ fromActivityId: decisionFromId }).forEach((t) => printjson(t));

db.form_fields.updateOne(
  { _id: formFieldId },
  {
    $set: {
      name: 'valido',
      label: '¿La documentación es válida?',
      type: 'CHECKBOX',
      required: true,
      active: true,
      updatedAt: new Date(),
    },
  }
);

db.workflow_transitions.find({ fromActivityId: decisionFromId }).forEach((t) => {
  if (t.conditionLabel && (!t.conditionExpression || t.conditionExpression === '')) {
    db.workflow_transitions.updateOne(
      { _id: t._id },
      { $set: { conditionExpression: t.conditionLabel.trim(), updatedAt: new Date() } }
    );
  }
});

print('=== Después: campo formulario ===');
printjson(db.form_fields.findOne({ _id: formFieldId }));

print('=== Después: transiciones ===');
db.workflow_transitions.find({ fromActivityId: decisionFromId }).forEach((t) => printjson(t));
