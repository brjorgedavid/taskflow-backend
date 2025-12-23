package com.taskflow.taskflow.service;

import com.taskflow.taskflow.data.VacationStatus;
import com.taskflow.taskflow.dto.VacationInput;
import com.taskflow.taskflow.exception.OverlappingVacationException;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.model.Vacation;
import com.taskflow.taskflow.repository.EmployeeRepository;
import com.taskflow.taskflow.repository.VacationRepository;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.taskflow.taskflow.util.VacationTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VacationService - Create Method Tests")
class VacationServiceCreateTest {

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
    @DisplayName("Should create vacation successfully without conflicts")
    void shouldCreateVacationSuccessfullyWithoutConflicts() {
        LocalDate startDate = futureDate(10);
        LocalDate endDate = futureDate(15);

        Employee currentEmployee = createDefaultEmployee();
        VacationInput input = createVacationInput(startDate, endDate);
        setupSuccessfulCreationMocks(currentUserService, vacationRepository, currentEmployee);

        Vacation result = vacationService.create(input);

        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(currentEmployee, result.getRequester());
        assertEquals(VacationStatus.PENDING, result.getStatus());

        verify(currentUserService, times(1)).getCurrentEmployee();
        verify(vacationRepository, times(1)).findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        );
        verify(vacationRepository, times(1)).save(any(Vacation.class));
    }

    @Test
    @DisplayName("Should set status to PENDING when creating vacation")
    void shouldSetStatusToPending() {
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(10);

        Employee currentEmployee = createDefaultEmployee();
        VacationInput input = createVacationInput(startDate, endDate);

        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(currentEmployee));
        when(vacationRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class), any(LocalDate.class), eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        )).thenReturn(Collections.emptyList());

        ArgumentCaptor<Vacation> vacationCaptor = ArgumentCaptor.forClass(Vacation.class);
        when(vacationRepository.save(vacationCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        vacationService.create(input);

        Vacation savedVacation = vacationCaptor.getValue();
        assertEquals(VacationStatus.PENDING, savedVacation.getStatus());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when input is null")
    void shouldThrowBadRequestWhenInputIsNull() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.create(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Request body required", exception.getReason());

        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when startDate is null")
    void shouldThrowNullPointerExceptionWhenStartDateIsNull() {
        VacationInput input = new VacationInput();
        input.setStartDate(null);
        input.setEndDate(LocalDate.now().plusDays(10));

        assertThrows(
                ResponseStatusException.class,
                () -> vacationService.create(input)
        );

        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when endDate is null")
    void shouldThrowResponseStatusExceptionWhenEndDateIsNull() {
        VacationInput input = new VacationInput();
        input.setStartDate(LocalDate.now().plusDays(5));
        input.setEndDate(null);

        assertThrows(
                ResponseStatusException.class,
                () -> vacationService.create(input)
        );

        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when startDate is in the past")
    void shouldThrowBadRequestWhenStartDateIsInThePast() {
        VacationInput input = new VacationInput();
        input.setStartDate(LocalDate.now().minusDays(1));
        input.setEndDate(LocalDate.now().plusDays(10));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.create(input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Start date cannot be in the past", exception.getReason());

        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when endDate is before startDate")
    void shouldThrowBadRequestWhenEndDateIsBeforeStartDate() {
        VacationInput input = new VacationInput();
        input.setStartDate(LocalDate.now().plusDays(10));
        input.setEndDate(LocalDate.now().plusDays(5));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.create(input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("End date cannot be before start date", exception.getReason());

        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when endDate equals startDate")
    void shouldThrowBadRequestWhenEndDateEqualsStartDate() {
        LocalDate sameDate = LocalDate.now().plusDays(5);

        VacationInput input = new VacationInput();
        input.setStartDate(sameDate);
        input.setEndDate(sameDate);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.create(input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Vacation must be at least one day long", exception.getReason());

        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CONFLICT when there is overlapping PENDING vacation")
    void shouldThrowConflictWhenThereIsOverlappingPendingVacation() {
        LocalDate[] dates = futureDateRange(10, 5);

        Employee currentEmployee = createDefaultEmployee();
        VacationInput input = createVacationInput(dates[0], dates[1]);

        Vacation existingVacation = createExistingVacation(
                futureDate(12),
                futureDate(17),
                VacationStatus.PENDING
        );

        setupConflictMocks(currentUserService, vacationRepository, currentEmployee, existingVacation);

        OverlappingVacationException exception = assertThrows(
                OverlappingVacationException.class,
                () -> vacationService.create(input)
        );

        assertEquals("Requested vacation overlaps with an existing vacation", exception.getMessage());
        assertNotNull(exception.getSuggestions());
        assertTrue(exception.getRequestedDays() > 0);

        verify(currentUserService, times(1)).getCurrentEmployee();
        verify(vacationRepository, times(1)).findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        );
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CONFLICT when there is overlapping APPROVED vacation")
    void shouldThrowConflictWhenThereIsOverlappingApprovedVacation() {
        LocalDate[] dates = futureDateRange(10, 5);

        Employee currentEmployee = createDefaultEmployee();
        VacationInput input = createVacationInput(dates[0], dates[1]);

        Vacation existingVacation = createExistingVacation(
                futureDate(12),
                futureDate(17),
                VacationStatus.APPROVED
        );

        setupConflictMocks(currentUserService, vacationRepository, currentEmployee, existingVacation);

        OverlappingVacationException exception = assertThrows(
                OverlappingVacationException.class,
                () -> vacationService.create(input)
        );

        assertEquals("Requested vacation overlaps with an existing vacation", exception.getMessage());
        assertNotNull(exception.getSuggestions());
        assertTrue(exception.getRequestedDays() > 0);

        verify(currentUserService, times(1)).getCurrentEmployee();
        verify(vacationRepository, times(1)).findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        );
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check conflicts only for PENDING and APPROVED statuses")
    void shouldCheckConflictsOnlyForPendingAndApprovedStatuses() {
        LocalDate[] dates = futureDateRange(5, 5);

        Employee currentEmployee = createDefaultEmployee();
        VacationInput input = createVacationInput(dates[0], dates[1]);
        setupSuccessfulCreationMocks(currentUserService, vacationRepository, currentEmployee);

        vacationService.create(input);

        verify(vacationRepository, times(1)).findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        );
    }
}

