package com.workflow.politicas.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormFieldKeyUtilTest {

    @Test
    void extractVariablesFromCondition_parsesValidoExpression() {
        List<String> vars = FormFieldKeyUtil.extractVariablesFromCondition("valido == true");
        assertEquals(List.of("valido"), vars);
    }

    @Test
    void validateTechnicalName_rejectsSpaces() {
        assertThrows(IllegalArgumentException.class, () -> FormFieldKeyUtil.validateTechnicalName("valido campo"));
    }

    @Test
    void generateFromLabel_normalizesAccents() {
        assertEquals("la_documentacion_es_valida", FormFieldKeyUtil.generateFromLabel("¿La documentación es válida?"));
    }
}
