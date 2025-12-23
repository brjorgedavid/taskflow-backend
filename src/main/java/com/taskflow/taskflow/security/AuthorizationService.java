package com.taskflow.taskflow.security;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.model.Vacation;
import com.taskflow.taskflow.repository.VacationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthorizationService {

    private final CurrentUserService currentUserService;
    private final VacationRepository vacationRepository;

    public AuthorizationService(CurrentUserService currentUserService, VacationRepository vacationRepository) {
        this.currentUserService = currentUserService;
        this.vacationRepository = vacationRepository;
    }

    public boolean isAdmin() {
        return attemptRequire(this::requireAdminRole);
    }

    public boolean isManager(UUID vacationId) {
        return attemptRequire(() -> requireManagerRole(vacationId));
    }

    public boolean isAdminOrManagerOfTeam() {
        return attemptRequire(this::requireAdminOrManagerOfTeam);
    }

    public boolean isAdminOrOwner(UUID employeeId) {
        return attemptRequire(() -> requireAdminOrOwner(employeeId));
    }


    private boolean attemptRequire(Runnable requirement) {
        try {
            requirement.run();
            return true;
        } catch (ResponseStatusException ex) {
            String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
            throw new AccessDeniedException(msg);
        }
    }

    private void requireAdminRole() {
        Optional<Role> callerRole = currentUserService.getCurrentRole();
        if (callerRole.isEmpty() || !(callerRole.get() == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role privileges");
        }
    }

    private void requireAdminOrOwner(UUID vacationId) {

        Optional<Role> callerRole = currentUserService.getCurrentRole();
        var currentEmployeeOpt = currentUserService.getCurrentEmployee();
        if (currentEmployeeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve current user");
        }

        var currentEmployee = currentEmployeeOpt.get();

        if (callerRole.isPresent() && callerRole.get() == Role.ADMIN) {
            return;
        }

        Vacation vac = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation not found"));


        if (vac.getRequester().getId().equals(currentEmployee.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Caller is not authorized to perform this action on the vacation");
    }

    private void requireManagerRole(UUID vacationId) {
        Optional<Role> callerRole = currentUserService.getCurrentRole();

        if (callerRole.isPresent() && callerRole.get() == Role.ADMIN) {
            return;
        }

        if (callerRole.isEmpty() || !(callerRole.get() == Role.MANAGER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role privileges");
        }

        Vacation vac = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation not found"));

        if (vac.getRequester() == null || vac.getRequester().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vacation has no associated requester");
        }

        var currentEmployeeOpt = currentUserService.getCurrentEmployee();
        if (currentEmployeeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve current user");
        }

        if (vac.getRequester().getManager().getId() == currentEmployeeOpt.get().getId()) {
            return;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Caller is not authorized to perform this action on the vacation");
        }

    }

    private void requireAdminOrManagerOfTeam() {
        Optional<Role> callerRole = currentUserService.getCurrentRole();
        if (callerRole.isEmpty() || !(callerRole.get() == Role.ADMIN || callerRole.get() == Role.MANAGER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role privileges");
        }

        var currentEmployeeOpt = currentUserService.getCurrentEmployee();
        if (currentEmployeeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve current user");
        }
    }
}
