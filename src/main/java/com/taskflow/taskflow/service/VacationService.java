package com.taskflow.taskflow.service;

import com.taskflow.taskflow.data.VacationStatus;
import com.taskflow.taskflow.dto.VacationInput;
import com.taskflow.taskflow.exception.OverlappingVacationException;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.model.Vacation;
import com.taskflow.taskflow.repository.VacationRepository;
import com.taskflow.taskflow.security.CurrentUserService;
import com.taskflow.taskflow.util.VacationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class VacationService {

    private final VacationRepository vacationRepository;
    private final CurrentUserService currentUserService;
    private final int pageSize;

    public VacationService(VacationRepository vacationRepository, CurrentUserService currentUserService, @Value("${app.employees.page-size:20}") int pageSize) {
        this.vacationRepository = vacationRepository;
        this.currentUserService = currentUserService;
        this.pageSize = pageSize;
    }

    @Transactional
    public Vacation create(VacationInput req) {
        VacationHelper.validateVacationInput(req);

        LocalDate startDate = req.getStartDate();
        LocalDate endDate = req.getEndDate();
        String reason = req.getRequestReason();

        var currentEmployeeOpt = currentUserService.getCurrentEmployee();
        if (currentEmployeeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve current user");
        }

        validateNoOverlappingVacations(startDate, endDate);

        Vacation v = new Vacation();
        v.setStartDate(startDate);
        v.setEndDate(endDate);
        v.setRequester(currentEmployeeOpt.get());
        v.setStatus(VacationStatus.PENDING);
        v.setRequestReason(reason);
        return vacationRepository.save(v);
    }

    @Transactional(readOnly = true)
    public Page<Vacation> findAll(int page) {
        var currentEmployeeOpt = currentUserService.getCurrentEmployee();
        if (currentEmployeeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        Employee currentEmployee = currentEmployeeOpt.get();
        PageRequest pageRequest = PageRequest.of(page, this.pageSize);

        if (currentEmployee.getRole() == com.taskflow.taskflow.data.Role.ADMIN) {
            return vacationRepository.findAll(pageRequest);
        }

        if (currentEmployee.getRole() == com.taskflow.taskflow.data.Role.MANAGER) {
            List<Employee> teamAndManager = new ArrayList<>(currentEmployee.getTeam());
            teamAndManager.add(currentEmployee);
            return vacationRepository.findByRequesterIn(teamAndManager, pageRequest);
        }

        return vacationRepository.findByRequester(currentEmployee, pageRequest);
    }

    @Transactional
    public void delete(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is required");
        }
        if (!vacationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation not found");
        }
        vacationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Vacation findById(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is required");
        }
        return vacationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation not found"));
    }

    @Transactional
    public Vacation decide(UUID vacationId, boolean approved, String comment) {
        VacationHelper.validateVacationId(vacationId);

        Vacation v = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation not found"));

        if (v.getStatus() != VacationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending vacations can be decided");
        }

        var currentEmployeeOpt = currentUserService.getCurrentEmployee();
        if (currentEmployeeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve current user");
        }

        if (approved) {
            v.setStatus(VacationStatus.APPROVED);
            v.setApprovalComment(comment);
            v.setDecidedAt(LocalDate.now());
            v.setDecidedBy(currentEmployeeOpt.get().getId());
        } else {
            v.setStatus(VacationStatus.REJECTED);
            v.setRejectionReason(comment);
            v.setDecidedAt(LocalDate.now());
            v.setDecidedBy(currentEmployeeOpt.get().getId());
        }

        return vacationRepository.save(v);
    }

    private void validateNoOverlappingVacations(LocalDate newStart, LocalDate newEnd) {
        if (newStart == null || newEnd == null) return;

        final int LOOKBACK_DAYS = 30;
        final int LOOKFORWARD_DAYS = 180;
        final int MAX_SUGGESTIONS = 3;

        long requestDuration = VacationHelper.calculateVacationDuration(newStart, newEnd);

        LocalDate searchStart = VacationHelper.calculateSearchWindowStart(newStart, LOOKBACK_DAYS);
        LocalDate searchEnd = VacationHelper.calculateSearchWindowEnd(newEnd, LOOKFORWARD_DAYS, requestDuration);

        List<VacationStatus> relevantStatuses = List.of(VacationStatus.PENDING, VacationStatus.APPROVED);

        List<Vacation> candidates = vacationRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                        searchEnd, searchStart, relevantStatuses
                );

        List<Pair<LocalDate, LocalDate>> occupied = new ArrayList<>();
        for (Vacation v : candidates) {
            LocalDate s = v.getStartDate();
            LocalDate e = v.getEndDate();
            if (s == null || e == null) continue;
            occupied.add(Pair.of(s, e));
        }

        List<Pair<LocalDate, LocalDate>> merged = VacationHelper.mergeOverlappingIntervals(occupied);

        for (Pair<LocalDate, LocalDate> ex : merged) {
            LocalDate exStart = ex.getFirst();
            LocalDate exEnd = ex.getSecond();
            boolean overlaps = !(newEnd.isBefore(exStart) || newStart.isAfter(exEnd));
            if (overlaps) {
                int requestedDays = (int) requestDuration;
                List<OverlappingVacationException.Suggestion> suggestions = VacationHelper.generateVacationSuggestions(
                        merged, newStart, newEnd, searchStart, searchEnd, MAX_SUGGESTIONS
                );

                LocalDate earliest = LocalDate.now().plusDays(1);
                List<OverlappingVacationException.Suggestion> filtered = suggestions.stream()
                        .filter(sug -> !sug.getStartDate().isBefore(earliest))
                        .collect(Collectors.toList());

                throw new OverlappingVacationException(
                        "Requested vacation overlaps with an existing vacation", filtered, requestedDays
                );
            }
        }
    }
}
