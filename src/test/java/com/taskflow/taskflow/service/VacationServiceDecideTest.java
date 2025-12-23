package com.taskflow.taskflow.service;

import com.taskflow.taskflow.data.VacationStatus;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.model.Vacation;
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
import java.util.Optional;
import java.util.UUID;

import static com.taskflow.taskflow.util.VacationTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VacationService - Decide Method Tests")
class VacationServiceDecideTest {

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
    @DisplayName("Should approve vacation successfully")
    void shouldApproveVacationSuccessfully() {
        UUID vacationId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        String approvalComment = "Approved - enjoy your vacation!";

        Employee manager = createDefaultEmployee();
        manager.setId(managerId);

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(manager));
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vacation result = vacationService.decide(vacationId, true, approvalComment);

        assertNotNull(result);
        assertEquals(VacationStatus.APPROVED, result.getStatus());
        assertEquals(approvalComment, result.getApprovalComment());
        assertNotNull(result.getDecidedAt());
        assertEquals(managerId, result.getDecidedBy());
        assertNull(result.getRejectionReason());

        verify(vacationRepository, times(1)).findById(vacationId);
        verify(currentUserService, times(1)).getCurrentEmployee();
        verify(vacationRepository, times(1)).save(pendingVacation);
    }

    @Test
    @DisplayName("Should reject vacation successfully")
    void shouldRejectVacationSuccessfully() {
        UUID vacationId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        String rejectionReason = "Insufficient staffing during this period";

        Employee manager = createDefaultEmployee();
        manager.setId(managerId);

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(manager));
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vacation result = vacationService.decide(vacationId, false, rejectionReason);

        assertNotNull(result);
        assertEquals(VacationStatus.REJECTED, result.getStatus());
        assertEquals(rejectionReason, result.getRejectionReason());
        assertNotNull(result.getDecidedAt());
        assertEquals(managerId, result.getDecidedBy());
        assertNull(result.getApprovalComment());

        verify(vacationRepository, times(1)).findById(vacationId);
        verify(currentUserService, times(1)).getCurrentEmployee();
        verify(vacationRepository, times(1)).save(pendingVacation);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when vacationId is null")
    void shouldThrowBadRequestWhenVacationIdIsNull() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.decide(null, true, "Comment")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Vacation id is required", exception.getReason());

        verify(vacationRepository, never()).findById(any());
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when vacation does not exist")
    void shouldThrowNotFoundWhenVacationDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        when(vacationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.decide(nonExistentId, true, "Comment")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Vacation not found", exception.getReason());

        verify(vacationRepository, times(1)).findById(nonExistentId);
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when vacation is already APPROVED")
    void shouldThrowBadRequestWhenVacationIsAlreadyApproved() {
        UUID vacationId = UUID.randomUUID();

        Vacation approvedVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.APPROVED
        );
        approvedVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(approvedVacation));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.decide(vacationId, true, "Comment")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Only pending vacations can be decided", exception.getReason());

        verify(vacationRepository, times(1)).findById(vacationId);
        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when vacation is already REJECTED")
    void shouldThrowBadRequestWhenVacationIsAlreadyRejected() {
        UUID vacationId = UUID.randomUUID();

        Vacation rejectedVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.REJECTED
        );
        rejectedVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(rejectedVacation));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.decide(vacationId, false, "Comment")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Only pending vacations can be decided", exception.getReason());

        verify(vacationRepository, times(1)).findById(vacationId);
        verify(currentUserService, never()).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when current user cannot be resolved")
    void shouldThrowForbiddenWhenCurrentUserCannotBeResolved() {
        UUID vacationId = UUID.randomUUID();

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vacationService.decide(vacationId, true, "Comment")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Unable to resolve current user", exception.getReason());

        verify(vacationRepository, times(1)).findById(vacationId);
        verify(currentUserService, times(1)).getCurrentEmployee();
        verify(vacationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set decidedAt to current date when approving")
    void shouldSetDecidedAtToCurrentDateWhenApproving() {
        UUID vacationId = UUID.randomUUID();
        Employee manager = createDefaultEmployee();

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(manager));

        ArgumentCaptor<Vacation> vacationCaptor = ArgumentCaptor.forClass(Vacation.class);
        when(vacationRepository.save(vacationCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        vacationService.decide(vacationId, true, "Approved");

        Vacation savedVacation = vacationCaptor.getValue();
        assertNotNull(savedVacation.getDecidedAt());
        assertEquals(LocalDate.now(), savedVacation.getDecidedAt());
    }

    @Test
    @DisplayName("Should set decidedAt to current date when rejecting")
    void shouldSetDecidedAtToCurrentDateWhenRejecting() {
        UUID vacationId = UUID.randomUUID();
        Employee manager = createDefaultEmployee();

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(manager));

        ArgumentCaptor<Vacation> vacationCaptor = ArgumentCaptor.forClass(Vacation.class);
        when(vacationRepository.save(vacationCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        vacationService.decide(vacationId, false, "Rejected");

        Vacation savedVacation = vacationCaptor.getValue();
        assertNotNull(savedVacation.getDecidedAt());
        assertEquals(LocalDate.now(), savedVacation.getDecidedAt());
    }

    @Test
    @DisplayName("Should handle approval with null comment")
    void shouldHandleApprovalWithNullComment() {
        UUID vacationId = UUID.randomUUID();
        Employee manager = createDefaultEmployee();

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(manager));
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vacation result = vacationService.decide(vacationId, true, null);

        assertNotNull(result);
        assertEquals(VacationStatus.APPROVED, result.getStatus());
        assertNull(result.getApprovalComment());
    }

    @Test
    @DisplayName("Should handle rejection with null comment")
    void shouldHandleRejectionWithNullComment() {
        UUID vacationId = UUID.randomUUID();
        Employee manager = createDefaultEmployee();

        Vacation pendingVacation = createExistingVacation(
                futureDate(10),
                futureDate(15),
                VacationStatus.PENDING
        );
        pendingVacation.setId(vacationId);

        when(vacationRepository.findById(vacationId)).thenReturn(Optional.of(pendingVacation));
        when(currentUserService.getCurrentEmployee()).thenReturn(Optional.of(manager));
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vacation result = vacationService.decide(vacationId, false, null);

        assertNotNull(result);
        assertEquals(VacationStatus.REJECTED, result.getStatus());
        assertNull(result.getRejectionReason());
    }
}

