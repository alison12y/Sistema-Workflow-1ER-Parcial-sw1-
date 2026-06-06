package com.workflow.politicas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 *  Crea índices MongoDB en arranque (idempotente, no borra datos).
 */
@Component
public class MongoCycle1IndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoCycle1IndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoCycle1IndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTramiteIndexes();
        ensureWorkflowIndexes();
        ensureFormAndBitacoraIndexes();
        ensureDocumentRepositoryIndexes();
        log.info("Índices Ciclo 1 (F10.2) verificados");
    }

    private void ensureTramiteIndexes() {
        var ops = mongoTemplate.indexOps("tramites");
        ops.ensureIndex(new Index().on("policyId", Sort.Direction.ASC));
        ops.ensureIndex(new Index().on("status", Sort.Direction.ASC));
        ops.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
        ops.ensureIndex(new Index().on("policyId", Sort.Direction.ASC).on("status", Sort.Direction.ASC));
        ops.ensureIndex(new Index().on("tasks.status", Sort.Direction.ASC));
        ops.ensureIndex(new Index().on("tasks.workflowActivityId", Sort.Direction.ASC));
        ops.ensureIndex(new Index().on("tasks.responsible", Sort.Direction.ASC));
        ops.ensureIndex(new Index().on("tasks.takenBy", Sort.Direction.ASC));
    }

    private void ensureWorkflowIndexes() {
        var activities = mongoTemplate.indexOps("workflow_activities");
        activities.ensureIndex(new Index().on("policyId", Sort.Direction.ASC));
        activities.ensureIndex(new Index().on("policyId", Sort.Direction.ASC).on("active", Sort.Direction.ASC));

        var transitions = mongoTemplate.indexOps("workflow_transitions");
        transitions.ensureIndex(new Index().on("policyId", Sort.Direction.ASC));
        transitions.ensureIndex(new Index().on("policyId", Sort.Direction.ASC).on("active", Sort.Direction.ASC));
    }

    private void ensureFormAndBitacoraIndexes() {
        var forms = mongoTemplate.indexOps("form_submissions");
        forms.ensureIndex(new Index().on("tramiteId", Sort.Direction.ASC));
        forms.ensureIndex(new Index().on("workflowActivityId", Sort.Direction.ASC));
        forms.ensureIndex(new Index()
                .on("tramiteId", Sort.Direction.ASC)
                .on("workflowActivityId", Sort.Direction.ASC));

        var bitacora = mongoTemplate.indexOps("bitacora");
        bitacora.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
        bitacora.ensureIndex(new Index().on("module", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
    }

    private void ensureDocumentRepositoryIndexes() {
        var repositories = mongoTemplate.indexOps("document_repositories");
        repositories.ensureIndex(new Index().on("tramiteId", Sort.Direction.ASC).unique());

        var records = mongoTemplate.indexOps("document_records");
        records.ensureIndex(new Index().on("repositoryId", Sort.Direction.ASC).on("estado", Sort.Direction.ASC));
        records.ensureIndex(new Index().on("tramiteId", Sort.Direction.ASC).on("estado", Sort.Direction.ASC));
        records.ensureIndex(new Index().on("documentFamilyId", Sort.Direction.ASC).on("version", Sort.Direction.DESC));
        records.ensureIndex(new Index().on("repositoryId", Sort.Direction.ASC).on("nombreOriginal", Sort.Direction.ASC));
        records.ensureIndex(new Index().on("fechaSubida", Sort.Direction.DESC));
    }
}
