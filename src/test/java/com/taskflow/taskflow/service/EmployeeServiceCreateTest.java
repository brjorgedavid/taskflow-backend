package com.taskflow.taskflow.service;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.dto.EmployeeInput;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.repository.EmployeeRepository;
import com.taskflow.taskflow.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static com.taskflow.taskflow.util.EmployeeTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService - Create Method Tests")
class EmployeeServiceCreateTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, currentUserService, 20);
    }

    @Test
    @DisplayName("Should create employee successfully with MANAGER")
    void shouldCreateEmployeeSuccessfullyWithManager() {
        UUID managerId = UUID.randomUUID();
        Employee manager = createManager("Manager", "Test", "manager@example.com");
        manager.setId(managerId);

        EmployeeInput input = createEmployeeInput(
                "employee@example.com",
                "John",
                "Doe",
                "password123",
                Role.EMPLOYEE,
                managerId
        );

        when(employeeRepository.findByEmail(input.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Employee result = employeeService.create(input);

        assertNotNull(result);
        assertEquals(input.getEmail(), result.getEmail());
        assertEquals(input.getFirstName(), result.getFirstName());
        assertEquals(input.getLastName(), result.getLastName());
        assertEquals(manager, result.getManager());

        verify(employeeRepository, times(1)).findByEmail(input.getEmail());
        verify(employeeRepository, times(1)).findById(managerId);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should create employee successfully with ADMIN as manager")
    void shouldCreateEmployeeSuccessfullyWithAdminAsManager() {
        UUID adminId = UUID.randomUUID();
        Employee admin = createAdmin("Admin", "Test", "admin@example.com");
        admin.setId(adminId);

        EmployeeInput input = createEmployeeInput(
                "employee@example.com",
                "Jane",
                "Smith",
                "password123",
                Role.EMPLOYEE,
                adminId
        );

        when(employeeRepository.findByEmail(input.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.create(input);

        assertNotNull(result);
        assertEquals(admin, result.getManager());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when input is null")
    void shouldThrowBadRequestWhenInputIsNull() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Employee payload is required", exception.getReason());

        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when manager is null")
    void shouldThrowBadRequestWhenManagerIsNull() {
        EmployeeInput input = createEmployeeInput(
                "employee@example.com",
                "John",
                "Doe",
                "password123",
                Role.EMPLOYEE
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Manager is required", exception.getReason());

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CONFLICT when email already exists")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        UUID managerId = UUID.randomUUID();
        String duplicateEmail = "duplicate@example.com";

        Employee existingEmployee = createEmployee("Existing", "User", duplicateEmail, Role.EMPLOYEE);

        EmployeeInput input = createEmployeeInput(
                duplicateEmail,
                "New",
                "User",
                "password123",
                Role.EMPLOYEE,
                managerId
        );

        when(employeeRepository.findByEmail(duplicateEmail)).thenReturn(Optional.of(existingEmployee));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(input)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already in use", exception.getReason());

        verify(employeeRepository, times(1)).findByEmail(duplicateEmail);
        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when manager does not exist")
    void shouldThrowNotFoundWhenManagerDoesNotExist() {
        UUID nonExistentManagerId = UUID.randomUUID();

        EmployeeInput input = createEmployeeInput(
                "employee@example.com",
                "John",
                "Doe",
                "password123",
                Role.EMPLOYEE,
                nonExistentManagerId
        );

        when(employeeRepository.findByEmail(input.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.findById(nonExistentManagerId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(input)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Manager not found", exception.getReason());

        verify(employeeRepository, times(1)).findByEmail(input.getEmail());
        verify(employeeRepository, times(1)).findById(nonExistentManagerId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when assigned manager is not MANAGER or ADMIN")
    void shouldThrowBadRequestWhenAssignedManagerIsNotManagerOrAdmin() {
        UUID employeeManagerId = UUID.randomUUID();
        Employee employeeAsManager = createEmployee("John", "Doe", "john@example.com", Role.EMPLOYEE);
        employeeAsManager.setId(employeeManagerId);

        EmployeeInput input = createEmployeeInput(
                "newemployee@example.com",
                "Jane",
                "Smith",
                "password123",
                Role.EMPLOYEE,
                employeeManagerId
        );

        when(employeeRepository.findByEmail(input.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.findById(employeeManagerId)).thenReturn(Optional.of(employeeAsManager));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.create(input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Assigned manager does not have a managerial role", exception.getReason());

        verify(employeeRepository, times(1)).findByEmail(input.getEmail());
        verify(employeeRepository, times(1)).findById(employeeManagerId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set employee ID to null before saving")
    void shouldSetEmployeeIdToNullBeforeSaving() {
        UUID managerId = UUID.randomUUID();
        Employee manager = createManager("Manager", "Test", "manager@example.com");
        manager.setId(managerId);

        EmployeeInput input = createEmployeeInput(
                "employee@example.com",
                "John",
                "Doe",
                "password123",
                Role.EMPLOYEE,
                managerId
        );

        when(employeeRepository.findByEmail(input.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        when(employeeRepository.save(employeeCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        employeeService.create(input);

        Employee savedEmployee = employeeCaptor.getValue();
        assertNull(savedEmployee.getId());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should call repository methods in correct order")
    void shouldCallRepositoryMethodsInCorrectOrder() {
        UUID managerId = UUID.randomUUID();
        Employee manager = createAdmin("Admin", "Test", "admin@example.com");
        manager.setId(managerId);

        EmployeeInput input = createEmployeeInput(
                "employee@example.com",
                "John",
                "Doe",
                "password123",
                Role.EMPLOYEE,
                managerId
        );

        when(employeeRepository.findByEmail(input.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        employeeService.create(input);

        var inOrder = inOrder(employeeRepository);
        inOrder.verify(employeeRepository).findByEmail(input.getEmail());
        inOrder.verify(employeeRepository).findById(managerId);
        inOrder.verify(employeeRepository).save(any(Employee.class));
    }
}
