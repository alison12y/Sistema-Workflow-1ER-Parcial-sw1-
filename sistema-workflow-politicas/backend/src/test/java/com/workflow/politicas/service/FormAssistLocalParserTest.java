package com.workflow.politicas.service;

import com.workflow.politicas.dto.AiFormAssistRequest;
import com.workflow.politicas.dto.AiFormAssistResponse;
import com.workflow.politicas.dto.AiFormFieldDefinitionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormAssistLocalParserTest {

    private static final String PERMISO_REPORT = """
            El solicitante necesita permiso por motivos familiares desde el 10 de junio hasta el 12 de junio. \
            Adjunta certificado de respaldo.""";

    @Test
    void parse_mapsSpanishDateRangeToStartAndEndFields() {
        AiFormAssistRequest request = new AiFormAssistRequest();
        request.setReport(PERMISO_REPORT);
        request.setFields(List.of(
                dateField("fecha_inicio", "Fecha de inicio"),
                dateField("fecha_fin", "Fecha de fin"),
                fileField("certificado", "Certificado")
        ));

        AiFormAssistResponse response = FormAssistLocalParser.parse(request);

        assertEquals("2026-06-10", response.getSuggestedValues().get("fecha_inicio"));
        assertEquals("2026-06-12", response.getSuggestedValues().get("fecha_fin"));

        var fileSuggestion = response.getFieldSuggestions().stream()
                .filter(s -> "certificado".equals(s.getFieldName()))
                .findFirst()
                .orElseThrow();
        assertFalse(fileSuggestion.getApplicable());
        assertEquals(
                "Los campos FILE no se autocompletan; adjunte el archivo manualmente.",
                fileSuggestion.getMessage()
        );
    }

    @Test
    void parse_doesNotUseTodayWhenReportContainsDates() {
        AiFormAssistRequest request = new AiFormAssistRequest();
        request.setReport("desde el 10 de junio hasta el 12 de junio");
        request.setFields(List.of(
                dateField("fecha_inicio", "Fecha de inicio"),
                dateField("fecha_fin", "Fecha de fin")
        ));

        AiFormAssistResponse response = FormAssistLocalParser.parse(request);

        assertNotEquals(java.time.LocalDate.now().toString(), response.getSuggestedValues().get("fecha_inicio"));
        assertNotEquals(java.time.LocalDate.now().toString(), response.getSuggestedValues().get("fecha_fin"));
        assertEquals("2026-06-10", response.getSuggestedValues().get("fecha_inicio"));
        assertEquals("2026-06-12", response.getSuggestedValues().get("fecha_fin"));
    }

    @Test
    void parse_usesTodayOnlyWhenNoDatesInReport() {
        AiFormAssistRequest request = new AiFormAssistRequest();
        request.setReport("El solicitante necesita permiso por motivos familiares.");
        request.setFields(List.of(dateField("fecha_inicio", "Fecha de inicio")));

        AiFormAssistResponse response = FormAssistLocalParser.parse(request);

        assertEquals(java.time.LocalDate.now().toString(), response.getSuggestedValues().get("fecha_inicio"));
    }

    private static AiFormFieldDefinitionDto dateField(String name, String label) {
        AiFormFieldDefinitionDto field = new AiFormFieldDefinitionDto();
        field.setName(name);
        field.setLabel(label);
        field.setType("DATE");
        field.setRequired(true);
        return field;
    }

    private static AiFormFieldDefinitionDto fileField(String name, String label) {
        AiFormFieldDefinitionDto field = new AiFormFieldDefinitionDto();
        field.setName(name);
        field.setLabel(label);
        field.setType("FILE");
        field.setRequired(true);
        return field;
    }
}
