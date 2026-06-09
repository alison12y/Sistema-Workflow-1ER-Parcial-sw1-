package com.workflow.politicas.repository;

import com.workflow.politicas.model.DocumentPermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends MongoRepository<DocumentPermission, String> {
    List<DocumentPermission> findByDocumentFamilyId(String documentFamilyId);
    Optional<DocumentPermission> findByDocumentFamilyIdAndGranteeTypeAndGranteeKey(
            String documentFamilyId, String granteeType, String granteeKey);
    void deleteByDocumentFamilyIdAndGranteeTypeAndGranteeKey(
            String documentFamilyId, String granteeType, String granteeKey);
}
