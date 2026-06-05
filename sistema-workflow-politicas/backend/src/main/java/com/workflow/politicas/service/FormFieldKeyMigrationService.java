package com.workflow.politicas.service;

import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.model.FormField;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.DynamicFormRepository;
import com.workflow.politicas.repository.FormFieldRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Corrige datos existentes de formularios (p. ej. campo {@code llenar} → {@code valido}).
 */
@Service
public class FormFieldKeyMigrationService {

    private static final Logger log = LoggerFactory.getLogger(FormFieldKeyMigrationService.class);

    private static final String TARGET_ACTIVITY_FRAGMENT = "recepcion";
    private static final String LEGACY_FIELD_NAME = "llenar";
    private static final String TARGET_FIELD_NAME = "valido";
    private static final String TARGET_LABEL = "¿La documentación es válida?";

    private final WorkflowActivityRepository workflowActivityRepository;
    private final DynamicFormRepository dynamicFormRepository;
    private final FormFieldRepository formFieldRepository;

    public FormFieldKeyMigrationService(
            WorkflowActivityRepository workflowActivityRepository,
            DynamicFormRepository dynamicFormRepository,
            FormFieldRepository formFieldRepository
    ) {
        this.workflowActivityRepository = workflowActivityRepository;
        this.dynamicFormRepository = dynamicFormRepository;
        this.formFieldRepository = formFieldRepository;
    }

    public void migrateRecepcionValidoField() {
        List<WorkflowActivity> activities = workflowActivityRepository.findAll().stream()
                .filter(WorkflowActivity::isActive)
                .filter(a -> a.getName() != null
                        && a.getName().toLowerCase(Locale.ROOT).replace("ó", "o").contains(TARGET_ACTIVITY_FRAGMENT))
                .filter(a -> a.getName().toLowerCase(Locale.ROOT).contains("bien"))
                .toList();

        int updated = 0;
        for (WorkflowActivity activity : activities) {
            updated += migrateActivityForm(activity);
        }
        if (updated > 0) {
            log.info("FormFieldKeyMigrationService: actualizados {} campo(s) valido en actividades de recepción", updated);
        }
    }

    private int migrateActivityForm(WorkflowActivity activity) {
        if (activity.getId() == null) {
            return 0;
        }
        List<DynamicForm> forms = dynamicFormRepository.findByActivityIdAndActiveTrue(activity.getId());
        if (forms.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (DynamicForm form : forms) {
            List<FormField> fields = formFieldRepository.findByFormIdOrderByOrderAsc(form.getId());
            boolean hasValido = fields.stream()
                    .anyMatch(f -> TARGET_FIELD_NAME.equalsIgnoreCase(f.getName()));
            for (FormField field : fields) {
                String name = field.getName() != null ? field.getName().trim().toLowerCase(Locale.ROOT) : "";
                if (LEGACY_FIELD_NAME.equals(name) && !hasValido) {
                    applyValidoField(field);
                    field.setActive(true);
                    formFieldRepository.save(field);
                    count++;
                    hasValido = true;
                    continue;
                }
                if (!field.isActive()) {
                    continue;
                }
                if (!LEGACY_FIELD_NAME.equals(name) && !hasValido) {
                    if (field.getType() != null
                            && "CHECKBOX".equalsIgnoreCase(field.getType())
                            && (name.isBlank() || name.equals("campo"))) {
                        applyValidoField(field);
                        field.setActive(true);
                        formFieldRepository.save(field);
                        count++;
                        hasValido = true;
                    }
                }
            }
            if (!hasValido) {
                FormField created = new FormField();
                created.setFormId(form.getId());
                applyValidoField(created);
                created.setOrder(1);
                created.setActive(true);
                created.setCreatedAt(LocalDateTime.now());
                created.setUpdatedAt(LocalDateTime.now());
                formFieldRepository.save(created);
                count++;
            }
        }
        return count;
    }

    private void applyValidoField(FormField field) {
        field.setName(TARGET_FIELD_NAME);
        field.setLabel(TARGET_LABEL);
        field.setType("CHECKBOX");
        field.setRequired(true);
        field.setUpdatedAt(LocalDateTime.now());
    }
}
