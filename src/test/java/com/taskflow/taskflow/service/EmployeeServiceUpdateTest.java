package com.taskflow.taskflow.service;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.repository.EmployeeRepository;
import com.taskflow.taskflow.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("EmployeeService - Update Method Tests")
class EmployeeServiceUpdateTest {

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
    @DisplayName("Should update all employee fields successfully")
    void shouldUpdateAllEmployeeFieldsSuccessfully() {
        UUID employeeId = UUID.randomUUID();
        UUID oldManagerId = UUID.randomUUID();
        UUID newManagerId = UUID.randomUUID();

        Employee existing = createEmployee("OldFirstName", "OldLastName", "old@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee oldManager = createManager("Old", "Manager", "oldmanager@example.com");
        oldManager.setId(oldManagerId);
        existing.setManager(oldManager);

        Employee newManager = createManager("New", "Manager", "newmanager@example.com");
        newManager.setId(newManagerId);

        Employee payload = new Employee();
        payload.setEmail("old@example.com");
        payload.setFirstName("NewFirstName");
        payload.setLastName("NewLastName");
        payload.setPasswordHash("newPasswordHash");
        payload.setRole(Role.MANAGER);

        Employee managerReference = new Employee();
        managerReference.setId(newManagerId);
        payload.setManager(managerReference);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.findById(newManagerId)).thenReturn(Optional.of(newManager));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertNotNull(result);
        assertEquals("NewFirstName", result.getFirstName());
        assertEquals("NewLastName", result.getLastName());
        assertEquals("newPasswordHash", result.getPasswordHash());
        assertEquals(Role.MANAGER, result.getRole());
        assertEquals(newManager, result.getManager());

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(employeeRepository, times(1)).findById(newManagerId);
        verify(employeeRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should update only firstName when other fields are null")
    void shouldUpdateOnlyFirstName() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("OldName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        payload.setFirstName("NewName");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertEquals("NewName", result.getFirstName());
        assertEquals("LastName", result.getLastName());
        assertEquals(Role.EMPLOYEE, result.getRole());

        verify(employeeRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should update only lastName")
    void shouldUpdateOnlyLastName() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("FirstName", "OldLastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        payload.setLastName("NewLastName");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertEquals("FirstName", result.getFirstName());
        assertEquals("NewLastName", result.getLastName());

        verify(employeeRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should update only role")
    void shouldUpdateOnlyRole() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("FirstName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        payload.setRole(Role.MANAGER);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertEquals(Role.MANAGER, result.getRole());
        assertEquals("FirstName", result.getFirstName());

        verify(employeeRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should update manager successfully")
    void shouldUpdateManagerSuccessfully() {
        UUID employeeId = UUID.randomUUID();
        UUID newManagerId = UUID.randomUUID();

        Employee existing = createEmployee("FirstName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee newManager = createManager("New", "Manager", "manager@example.com");
        newManager.setId(newManagerId);

        Employee payload = new Employee();
        Employee managerReference = new Employee();
        managerReference.setId(newManagerId);
        payload.setManager(managerReference);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.findById(newManagerId)).thenReturn(Optional.of(newManager));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertEquals(newManager, result.getManager());

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(employeeRepository, times(1)).findById(newManagerId);
        verify(employeeRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when id is null")
    void shouldThrowBadRequestWhenIdIsNull() {
        Employee payload = new Employee();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.update(null, payload)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Id and payload are required", exception.getReason());

        verify(employeeRepository, never()).findById(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when payload is null")
    void shouldThrowBadRequestWhenPayloadIsNull() {
        UUID employeeId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.update(employeeId, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Id and payload are required", exception.getReason());

        verify(employeeRepository, never()).findById(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when employee does not exist")
    void shouldThrowNotFoundWhenEmployeeDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        Employee payload = new Employee();

        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.update(nonExistentId, payload)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Employee not found", exception.getReason());

        verify(employeeRepository, times(1)).findById(nonExistentId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when trying to change email")
    void shouldThrowBadRequestWhenTryingToChangeEmail() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("FirstName", "LastName", "old@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        payload.setEmail("new@example.com");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.update(employeeId, payload)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Email cannot be changed", exception.getReason());

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when employee tries to be their own manager")
    void shouldThrowBadRequestWhenEmployeeTriesToBeTheirOwnManager() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("FirstName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        Employee managerReference = new Employee();
        managerReference.setId(employeeId);
        payload.setManager(managerReference);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.update(employeeId, payload)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Employee cannot be their own manager", exception.getReason());

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when new manager does not exist")
    void shouldThrowNotFoundWhenNewManagerDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID nonExistentManagerId = UUID.randomUUID();

        Employee existing = createEmployee("FirstName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        Employee managerReference = new Employee();
        managerReference.setId(nonExistentManagerId);
        payload.setManager(managerReference);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.findById(nonExistentManagerId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.update(employeeId, payload)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Manager not found", exception.getReason());

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(employeeRepository, times(1)).findById(nonExistentManagerId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should call repository methods in correct order")
    void shouldCallRepositoryMethodsInCorrectOrder() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("OldName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        payload.setFirstName("NewName");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        employeeService.update(employeeId, payload);

        var inOrder = inOrder(employeeRepository);
        inOrder.verify(employeeRepository).findById(employeeId);
        inOrder.verify(employeeRepository).save(existing);
    }

    @Test
    @DisplayName("Should preserve unchanged fields")
    void shouldPreserveUnchangedFields() {
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Employee manager = createManager("Manager", "Test", "manager@example.com");
        manager.setId(managerId);

        Employee existing = createEmployee("FirstName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);
        existing.setPasswordHash("oldHash");
        existing.setManager(manager);

        Employee payload = new Employee();
        payload.setFirstName("NewFirstName");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertEquals("NewFirstName", result.getFirstName());
        assertEquals("LastName", result.getLastName());
        assertEquals("oldHash", result.getPasswordHash());
        assertEquals(Role.EMPLOYEE, result.getRole());
        assertEquals(manager, result.getManager());

        verify(employeeRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should allow updating with same email")
    void shouldAllowUpdatingWithSameEmail() {
        UUID employeeId = UUID.randomUUID();
        Employee existing = createEmployee("OldName", "LastName", "test@example.com", Role.EMPLOYEE);
        existing.setId(employeeId);

        Employee payload = new Employee();
        payload.setEmail("test@example.com");
        payload.setFirstName("NewName");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(employeeId, payload);

        assertEquals("NewName", result.getFirstName());
        assertEquals("test@example.com", result.getEmail());

        verify(employeeRepository, times(1)).save(existing);
    }
}
