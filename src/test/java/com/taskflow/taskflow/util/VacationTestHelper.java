package com.taskflow.taskflow.util;

import com.taskflow.taskflow.data.VacationStatus;
import com.taskflow.taskflow.dto.VacationInput;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.model.Vacation;
import com.taskflow.taskflow.repository.VacationRepository;
import com.taskflow.taskflow.security.CurrentUserService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VacationTestHelper {

    public static Employee createDefaultEmployee() {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@example.com");
        return employee;
    }

    public static VacationInput createVacationInput(LocalDate startDate, LocalDate endDate) {
        VacationInput input = new VacationInput();
        input.setStartDate(startDate);
        input.setEndDate(endDate);
        return input;
    }

    public static Vacation createExistingVacation(LocalDate startDate, LocalDate endDate, VacationStatus status) {
        Vacation vacation = new Vacation();
        vacation.setId(UUID.randomUUID());
        vacation.setStartDate(startDate);
        vacation.setEndDate(endDate);
        vacation.setStatus(status);
        return vacation;
    }

    public static void setupSuccessfulCreationMocks(
            CurrentUserService currentUserService,
            VacationRepository vacationRepository,
            Employee employee) {

        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(employee));
        when(vacationRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        )).thenReturn(Collections.emptyList());

        when(vacationRepository.save(any(Vacation.class))).thenAnswer(invocation -> {
            Vacation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    public static void setupConflictMocks(
            CurrentUserService currentUserService,
            VacationRepository vacationRepository,
            Employee employee,
            Vacation conflictingVacation) {

        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(employee));
        when(vacationRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Arrays.asList(VacationStatus.PENDING, VacationStatus.APPROVED))
        )).thenReturn(Collections.singletonList(conflictingVacation));
    }

    public static LocalDate futureDate(int daysFromNow) {
        return LocalDate.now().plusDays(daysFromNow);
    }

    public static LocalDate[] futureDateRange(int startDaysFromNow, int durationDays) {
        LocalDate start = futureDate(startDaysFromNow);
        LocalDate end = start.plusDays(durationDays);
        return new LocalDate[]{start, end};
    }
}

