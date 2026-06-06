package com.workflow.politicas.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentStoragePathBuilderTest {

    @Test
    void buildObjectKey_versionOne_usesTramiteRoot() {
        assertEquals(
                "TRM-007/documento.pdf",
                DocumentStoragePathBuilder.buildObjectKey("TRM-007", "documento.pdf", 1)
        );
    }

    @Test
    void buildObjectKey_versionTwo_usesVersionesFolder() {
        assertEquals(
                "TRM-007/versiones/documento_v2.pdf",
                DocumentStoragePathBuilder.buildObjectKey("TRM-007", "documento_v2.pdf", 2)
        );
    }

    @Test
    void buildStorageFileName_versionOne_keepsOriginalName() {
        assertEquals("imagen.jpg", DocumentStoragePathBuilder.buildStorageFileName("imagen.jpg", 1));
    }

    @Test
    void buildStorageFileName_versionThree_appendsSuffix() {
        assertEquals("documento_v3.pdf", DocumentStoragePathBuilder.buildStorageFileName("documento.pdf", 3));
    }

    @Test
    void normalizeTramiteCodigo_uppercasesAndTrims() {
        assertEquals("TRM-007", DocumentStoragePathBuilder.normalizeTramiteCodigo(" trm-007 "));
    }

    @Test
    void normalizeTramiteCodigo_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> DocumentStoragePathBuilder.normalizeTramiteCodigo(" "));
    }
}
