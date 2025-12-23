package com.taskflow.taskflow.service;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.dto.EmployeeInput;
import com.taskflow.taskflow.dto.EmployeeManagerResponse;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.repository.EmployeeRepository;
import com.taskflow.taskflow.security.CurrentUserService;
import com.taskflow.taskflow.util.EmployeeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final int pageSize;

    public EmployeeService(EmployeeRepository employeeRepository, CurrentUserService currentUserService, @Value("${app.employees.page-size:20}") int pageSize) {
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.pageSize = pageSize;
    }

    @Transactional
    public Employee create(EmployeeInput employeeInput) {

        if (employeeInput == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee payload is required");
        }

        Employee employee = EmployeeMapper.toEntity(employeeInput);

        if (employee.getManager() == null || employee.getManager().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager is required");
        }

        Optional<Employee> byEmail = employeeRepository.findByEmail(employee.getEmail());
        if (byEmail.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        if (employee.getManager() != null && employee.getManager().getId() != null) {
            UUID managerId = employee.getManager().getId();
            Employee manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));

            if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned manager does not have a managerial role");
            }

            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        employee.setId(null);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(UUID id, Employee payload) {
        if (id == null || payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id and payload are required");
        }

        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (payload.getEmail() != null && !payload.getEmail().equals(existing.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be changed");
        }

        if (payload.getFirstName() != null) {
            existing.setFirstName(payload.getFirstName());
        }

        if (payload.getLastName() != null) {
            existing.setLastName(payload.getLastName());
        }

        if (payload.getPasswordHash() != null) {
            existing.setPasswordHash(payload.getPasswordHash());
        }

        if (payload.getRole() != null) {
            existing.setRole(payload.getRole());
        }

        if (payload.getManager() != null && payload.getManager().getId() != null) {
            UUID managerId = payload.getManager().getId();
            if (managerId.equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee cannot be their own manager");
            }
            Employee manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));
            existing.setManager(manager);
        } else {
            if (payload.getManager() != null) {
                existing.setManager(null);
            }
        }

        return employeeRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is required");
        }
        if (!employeeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Employee> findAll(int page) {
        PageRequest pageRequest = PageRequest.of(page, this.pageSize, Sort.by("firstName").ascending());
        return employeeRepository.findAll(pageRequest);
    }

    @Transactional(readOnly = true)
    public Employee findById(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is required");
        }
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Transactional(readOnly = true)
    public Employee findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Transactional(readOnly = true)
    public Page<Employee> findByFirstNameContaining(String firstName, int page) {
        if (firstName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First Name is required");
        }

        PageRequest pageRequest = PageRequest.of(page, this.pageSize);
        return employeeRepository.findByFirstNameContainingIgnoreCase(firstName, pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<Employee> findByManager(UUID id, int page) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager Id is required");
        }
        Employee manager = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));

        PageRequest pageRequest = PageRequest.of(page, this.pageSize);
        return employeeRepository.findByManager(manager, pageRequest);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByManager() {
        Employee currentEmployee = getCurrentEmployee();
        UUID id = currentEmployee.getId();
        return employeeRepository.findByManagerId(id);
    }

    @Transactional(readOnly = true)
    public List<EmployeeManagerResponse> findManagers() {
        List<Employee> managers = employeeRepository.findByRole(Role.MANAGER);
        List<Employee> admins = employeeRepository.findByRole(Role.ADMIN);

        List<Employee> allManagers = new ArrayList<>();
        allManagers.addAll(managers);
        allManagers.addAll(admins);

        return EmployeeMapper.toManagerResponseList(allManagers);
    }

    @Transactional(readOnly = true)
    public Employee getCurrentEmployee() {
        return currentUserService.getCurrentEmployee()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));
    }
}

