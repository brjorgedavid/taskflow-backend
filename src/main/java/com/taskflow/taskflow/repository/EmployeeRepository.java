package com.taskflow.taskflow.repository;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmail(String email);

    List<Employee> findByManagerId(UUID managerId);

    List<Employee> findByRole(Role role);

    Page<Employee> findByManager(Employee employee, Pageable pageable);

    Page<Employee> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

}
