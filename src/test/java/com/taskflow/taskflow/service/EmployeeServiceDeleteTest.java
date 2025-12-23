package com.taskflow.taskflow.service;

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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService - Delete Method Tests")
class EmployeeServiceDeleteTest {

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
    @DisplayName("Should delete employee successfully")
    void shouldDeleteEmployeeSuccessfully() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(employeeId);

        assertDoesNotThrow(() -> employeeService.delete(employeeId));

        verify(employeeRepository, times(1)).existsById(employeeId);
        verify(employeeRepository, times(1)).deleteById(employeeId);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when id is null")
    void shouldThrowBadRequestWhenIdIsNull() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> employeeService.delete(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Id is required", exception.getReason());

        verify(employeeRepository, never()).existsById(any());
        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when employee does not exist")
    void shouldThrowNotFoundWhenEmployeeDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        when(employeeRepository.existsById(nonExistentId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> employeeService.delete(nonExistentId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Employee not found", exception.getReason());

        verify(employeeRepository, times(1)).existsById(nonExistentId);
        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should verify repository call order")
    void shouldVerifyRepositoryCallOrder() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(employeeId);

        employeeService.delete(employeeId);

        var inOrder = inOrder(employeeRepository);
        inOrder.verify(employeeRepository).existsById(employeeId);
        inOrder.verify(employeeRepository).deleteById(employeeId);
    }

    @Test
    @DisplayName("Should ensure deleteById is called only once")
    void shouldEnsureDeleteByIdIsCalledOnlyOnce() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(employeeId);

        employeeService.delete(employeeId);

        verify(employeeRepository, times(1)).deleteById(employeeId);
        verifyNoMoreInteractions(employeeRepository);
    }
}
