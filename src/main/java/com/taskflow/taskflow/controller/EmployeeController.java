package com.taskflow.taskflow.controller;

import com.taskflow.taskflow.dto.ApiResponse;
import com.taskflow.taskflow.dto.EmployeeInput;
import com.taskflow.taskflow.dto.EmployeeManagerResponse;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.service.EmployeeService;
import com.taskflow.taskflow.util.EmployeeMapper;
import com.taskflow.taskflow.util.PagingResponseBuilder;
import com.taskflow.taskflow.util.UriUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employees", description = "Endpoints for managing employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/by-email/{email}")
    @Operation(
            summary = "Get Employee by Email",
            description = "Fetches an employee by their email address (Admin only)"
    )
    public ApiResponse getByEmail(@PathVariable String email) {
        Employee e = employeeService.findByEmail(email);
        return ApiResponse.ok("Employee fetched successfully", EmployeeMapper.toResponse(e));
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/by-first-name/{firstName}")
    @Operation(
            summary = "Get Employees by First Name",
            description = "Fetches employees whose first names contain the given string (Admin only)"
    )
    public ApiResponse getByFirstName(@PathVariable String firstName, @RequestParam(value = "page", required = false) Integer page) {
        int pageIndex = page == null ? 0 : page;

        if (pageIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        Page<Employee> pageResult = employeeService.findByFirstNameContaining(firstName, pageIndex);
        return PagingResponseBuilder.build(pageResult, EmployeeMapper::toResponse, "Employees fetched successfully");
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/by-manager/{id}")
    @Operation(
            summary = "Get Employees by Manager",
            description = "Fetches employees managed by the specified manager (Admin only)"
    )
    public ApiResponse getByManager(@PathVariable UUID id, @RequestParam(value = "page", required = false) Integer page) {
        int pageIndex = page == null ? 0 : page;

        if (pageIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        Page<Employee> pageResult = employeeService.findByManager(id, pageIndex);
        return PagingResponseBuilder.build(pageResult, EmployeeMapper::toResponse, "Employees fetched successfully");
    }

    @PreAuthorize("@authorizationService.isAdminOrManagerOfTeam()")
    @GetMapping("/by-manager")
    @Operation(
            summary = "Get Employees by Logged-in Manager",
            description = "Fetches employees managed by the currently logged-in manager (Admin or Manager)"
    )
    public ApiResponse getByManagerLogged() {
        List<Employee> employees = employeeService.findByManager();
        return ApiResponse.ok("Employees fetched successfully", employees.stream().map(EmployeeMapper::toResponse).toList());
    }

    @GetMapping
    @PreAuthorize("@authorizationService.isAdmin()")
    @Operation(
            summary = "List Employees",
            description = "Lists all employees with pagination (Admin only)"
    )
    public ApiResponse getEmployees(
            @Parameter(description = "Número da página (0-based)")
            @RequestParam(value = "page", required = false) Integer page) {
        int pageIndex = page == null ? 0 : page;
        if (pageIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        Page<Employee> pageResult = employeeService.findAll(pageIndex);
        return PagingResponseBuilder.build(pageResult, EmployeeMapper::toResponse, "Employees fetched successfully");
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/managers")
    @Operation(
            summary = "Get Managers",
            description = "Fetches all employees with Manager or Admin roles (Admin only)"
    )
    public ApiResponse getManagers() {
        List<EmployeeManagerResponse> managers = employeeService.findManagers();
        return ApiResponse.ok("Managers fetched successfully", managers);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get Current User",
            description = "Fetches the currently logged-in user's details"
    )
    public ApiResponse getCurrentUser() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        return ApiResponse.ok("Current user fetched successfully", EmployeeMapper.toResponse(currentEmployee));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Employee by ID",
            description = "Fetches an employee by their ID"
    )
    public ApiResponse getOne(@PathVariable UUID id) {
        Employee e = employeeService.findById(id);
        return ApiResponse.ok("Employee fetched successfully", EmployeeMapper.toResponse(e));
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping
    @Operation(
            summary = "Create Employee",
            description = "Creates a new employee (Admin only)"
    )
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody EmployeeInput employeeInput) {
        Employee created = employeeService.create(employeeInput);
        URI location = UriUtils.locationForCurrentRequest(created.getId());

        ApiResponse body = ApiResponse.of(HttpStatus.CREATED.value(), "Employee created successfully", EmployeeMapper.toResponse(created));
        return ResponseEntity.created(location).body(body);
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PatchMapping("/{id}")
    @Operation(
            summary = "Update Employee",
            description = "Updates an existing employee (Admin only)"
    )
    public ApiResponse update(@PathVariable UUID id, @Valid @RequestBody EmployeeInput req) {
        Employee payload = EmployeeMapper.toEntity(req);
        Employee updated = employeeService.update(id, payload);
        return ApiResponse.ok("Employee updated successfully", EmployeeMapper.toResponse(updated));
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete Employee",
            description = "Deletes an employee by ID (Admin only)"
    )
    public ApiResponse delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ApiResponse.ok("Employee deleted successfully");
    }
}
