package com.workflow.politicas.service;

import com.workflow.politicas.dto.KpiLoadMetricDto;
import com.workflow.politicas.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeDisplayNameResolverTest {

    @Test
    void isRoleLabel_detectsFuncionarioRole() {
        assertTrue(EmployeeDisplayNameResolver.isRoleLabel("Funcionario", List.of("Supervisor")));
    }

    @Test
    void resolvePersonLabel_prefersFullNameOverRoleDisplayName() {
        User user = new User();
        user.setUsername("ana.rodriguez");
        user.setFullName("Ana Rodríguez Paz");

        KpiLoadMetricDto load = new KpiLoadMetricDto();
        load.setKey("ana.rodriguez");
        load.setDisplayName("Funcionario");

        String label = EmployeeDisplayNameResolver.resolvePersonLabel(
                load,
                Map.of("ana.rodriguez", user),
                List.of("Funcionario")
        );

        assertEquals("Ana Rodríguez Paz", label);
    }

    @Test
    void isRankableMetric_rejectsRoleOnlyBucket() {
        KpiLoadMetricDto load = new KpiLoadMetricDto();
        load.setKey("Funcionario");
        load.setDisplayName("Funcionario");
        load.setTotalActive(8);

        assertFalse(EmployeeDisplayNameResolver.isRankableMetric(load, List.of("Funcionario"), Map.of()));
    }
}
