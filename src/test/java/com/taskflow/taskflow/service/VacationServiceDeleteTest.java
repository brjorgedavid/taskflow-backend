package com.taskflow.taskflow.service;

import com.taskflow.taskflow.repository.VacationRepository;
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
@DisplayName("VacationService - Delete Method Tests")
class VacationServiceDeleteTest {

    @Mock
    private VacationRepository vacationRepository;

    @Mock
    private CurrentUserService currentUserService;

    private VacationService vacationService;

    @BeforeEach
    void setUp() {
        vacationService = new VacationService(vacationRepository, currentUserService, 20);
    }

    @Test
    @DisplayName("Should delete vacation successfully when ID exists")
    void shouldDeleteVacationSuccessfully() {
        UUID vacationId = UUID.randomUUID();

        when(vacationRepository.existsById(vacationId)).thenReturn(true);
        doNothing().when(vacationRepository).deleteById(vacationId);

        assertDoesNotThrow(() -> vacationService.delete(vacationId));

        verify(vacationRepository, times(1)).existsById(vacationId);
        verify(vacationRepository, times(1)).deleteById(vacationId);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when ID is null")
    void shouldThrowBadRequestWhenIdIsNull() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.delete(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Id is required", exception.getReason());

        verify(vacationRepository, never()).existsById(any());
        verify(vacationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when vacation does not exist")
    void shouldThrowNotFoundWhenVacationDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        when(vacationRepository.existsById(nonExistentId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.delete(nonExistentId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Vacation not found", exception.getReason());

        verify(vacationRepository, times(1)).existsById(nonExistentId);
        verify(vacationRepository, never()).deleteById(any());
    }
}

