package com.taskflow.taskflow.util;

import com.taskflow.taskflow.dto.EmployeeInput;
import com.taskflow.taskflow.dto.EmployeeManagerResponse;
import com.taskflow.taskflow.dto.EmployeeResponse;
import com.taskflow.taskflow.model.Employee;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static EmployeeResponse toResponse(Employee e) {
        if (e == null) return null;
        UUID managerId = e.getManager() != null ? e.getManager().getId() : null;

        return new EmployeeResponse(
                e.getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmail(),
                e.getRole(),
                managerId
        );
    }

    public static EmployeeManagerResponse toManagerResponse(Employee e) {
        if (e == null) return null;
        return new EmployeeManagerResponse(e.getId(), e.getFirstName(), e.getLastName());
    }

    public static List<EmployeeManagerResponse> toManagerResponseList(List<Employee> employees) {
        if (employees == null) return null;
        return employees.stream()
                .map(EmployeeMapper::toManagerResponse)
                .collect(Collectors.toList());
    }

    public static Employee toEntity(EmployeeInput req) {
        if (req == null) return null;
        Employee e = new Employee();
        e.setFirstName(req.getFirstName());
        e.setLastName(req.getLastName());
        e.setPasswordHash(PasswordHashUtil.hashPassword(req.getPassword()));
        e.setEmail(req.getEmail());
        e.setRole(req.getRole());
        if (req.getManagerId() != null) {
            Employee manager = new Employee();
            manager.setId(req.getManagerId());
            e.setManager(manager);
        } else {
            e.setManager(null);
        }
        return e;
    }
}
