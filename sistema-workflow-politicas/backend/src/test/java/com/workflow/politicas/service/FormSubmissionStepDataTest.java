package com.workflow.politicas.service;

import com.workflow.politicas.dto.DynamicFormDetailResponse;
import com.workflow.politicas.dto.FormFieldDto;
import com.workflow.politicas.dto.ResponseItemDto;
import com.workflow.politicas.repository.FormSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormSubmissionStepDataTest {

    @Mock
    private FormSubmissionRepository formSubmissionRepository;
    @Mock
    private DynamicFormService dynamicFormService;
    @Mock
    private com.workflow.politicas.repository.UserRepository userRepository;
    @Mock
    private FormSubmissionFileService formSubmissionFileService;
    @Mock
    private WorkflowFormConditionValidationService workflowFormConditionValidationService;

    @InjectMocks
    private FormSubmissionService formSubmissionService;

    @Test
    void buildStepData_usesTechnicalNameForCheckboxTrue() {
        mockFormWithValidoField();
        ResponseItemDto response = new ResponseItemDto();
        response.setFieldName("valido");
        response.setValue("true");

        Map<String, Object> stepData = formSubmissionService.buildStepDataForWorkflowActivity(
                "act-recepcion",
                List.of(response)
        );

        assertEquals(Boolean.TRUE, stepData.get("valido"));
    }

    @Test
    void buildStepData_usesTechnicalNameForCheckboxFalseWhenUnchecked() {
        mockFormWithValidoField();

        Map<String, Object> stepData = formSubmissionService.buildStepDataForWorkflowActivity(
                "act-recepcion",
                List.of()
        );

        assertFalse((Boolean) stepData.get("valido"));
    }

    @Test
    void buildStepData_mergesLegacyResponseKeyAlongsideFormField() {
        mockFormWithValidoField();
        ResponseItemDto wrongKey = new ResponseItemDto();
        wrongKey.setFieldName("llenar");
        wrongKey.setFieldLabel("¿La documentación es válida?");
        wrongKey.setValue("true");

        Map<String, Object> stepData = formSubmissionService.buildStepDataForWorkflowActivity(
                "act-recepcion",
                List.of(wrongKey)
        );

        assertEquals(Boolean.FALSE, stepData.get("valido"));
        assertEquals(Boolean.TRUE, stepData.get("llenar"));
    }

    private void mockFormWithValidoField() {
        FormFieldDto field = new FormFieldDto();
        field.setName("valido");
        field.setLabel("¿La documentación es válida?");
        field.setType("checkbox");
        field.setRequired(true);

        DynamicFormDetailResponse form = new DynamicFormDetailResponse();
        form.setId("form-1");
        form.setFields(List.of(field));

        when(dynamicFormService.getDetailByWorkflowActivityId("act-recepcion")).thenReturn(form);
    }
}
