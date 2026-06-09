package com.workflow.politicas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyActivitiesControllerMappingTest {

    @Test
    void taskAssistantEndpointIsMapped() throws NoSuchMethodException {
        RequestMapping classMapping = MyActivitiesController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertEquals("/api/my-activities", classMapping.value()[0]);

        Method method = MyActivitiesController.class.getMethod(
                "taskAssistant",
                String.class,
                int.class,
                org.springframework.security.core.Authentication.class
        );
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertEquals(
                "/{activityId}/tasks/{taskId}/ai-assistant",
                Arrays.asList(postMapping.value()).get(0)
        );
    }
}
