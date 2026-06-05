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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormSubmissionRequiredValidationTest {

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
    void validateResponses_rejectsEmptyRequiredFieldOnComplete() {
        FormFieldDto field = new FormFieldDto();
        field.setName("nombreCliente");
        field.setLabel("Nombre del cliente");
        field.setType("TEXT");
        field.setRequired(true);

        DynamicFormDetailResponse form = new DynamicFormDetailResponse();
        form.setId("form-1");
        form.setFields(List.of(field));

        when(dynamicFormService.getDetailByWorkflowActivityId("act-1")).thenReturn(form);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                formSubmissionService.validateResponsesForWorkflowActivity(
                        "act-1",
                        "policy-1",
                        List.of(),
                        true));

        assertTrue(ex.getMessage().contains("obligatorios"));
        assertTrue(ex.getMessage().contains("Nombre del cliente"));
    }
}
