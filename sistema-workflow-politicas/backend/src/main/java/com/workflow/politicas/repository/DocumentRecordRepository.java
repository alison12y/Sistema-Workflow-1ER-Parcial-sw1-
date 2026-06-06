package com.workflow.politicas.repository;

import com.workflow.politicas.model.DocumentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentRecordRepository extends MongoRepository<DocumentRecord, String> {

    List<DocumentRecord> findByRepositoryIdAndEstadoOrderByFechaSubidaDesc(String repositoryId, String estado);

    List<DocumentRecord> findByRepositoryIdAndNombreOriginalAndEstadoNot(
            String repositoryId,
            String nombreOriginal,
            String estado
    );

    List<DocumentRecord> findByDocumentFamilyIdAndEstadoNotOrderByVersionDesc(
            String documentFamilyId,
            String estado
    );
}
