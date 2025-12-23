package com.taskflow.taskflow.util;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.dto.EmployeeInput;
import com.taskflow.taskflow.model.Employee;

import java.util.UUID;

public class EmployeeTestHelper {

    private static final String DEFAULT_PASSWORD_HASH = "$2a$10$cOFFrd9N/J0yFfvnDxRz..TjvE1SKeMVBIqKSsE3V9fX1pYZzcJ0e";

    public static Employee createDefaultEmployee() {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@example.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setPasswordHash(DEFAULT_PASSWORD_HASH);
        return employee;
    }

    public static Employee createEmployee(String firstName, String lastName, String email, Role role) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setRole(role);
        employee.setPasswordHash(DEFAULT_PASSWORD_HASH);
        return employee;
    }

    public static Employee createManager(String firstName, String lastName, String email) {
        return createEmployee(firstName, lastName, email, Role.MANAGER);
    }

    public static Employee createAdmin(String firstName, String lastName, String email) {
        return createEmployee(firstName, lastName, email, Role.ADMIN);
    }

    public static EmployeeInput createEmployeeInput(
            String email,
            String firstName,
            String lastName,
            String password,
            Role role,
            UUID managerId) {

        EmployeeInput input = new EmployeeInput();
        input.setEmail(email);
        input.setFirstName(firstName);
        input.setLastName(lastName);
        input.setPassword(password);
        input.setRole(role);
        input.setManagerId(managerId);
        return input;
    }

    public static EmployeeInput createEmployeeInput(
            String email,
            String firstName,
            String lastName,
            String password,
            Role role) {

        return createEmployeeInput(email, firstName, lastName, password, role, null);
    }
}

