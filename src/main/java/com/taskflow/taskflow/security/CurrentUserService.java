package com.taskflow.taskflow.security;

import com.taskflow.taskflow.data.Role;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Service
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);

    private final EmployeeRepository employeeRepository;

    public CurrentUserService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Optional<Employee> getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        String principalStr = null;
        if (principal instanceof UserDetails) {
            principalStr = ((UserDetails) principal).getUsername();
        } else if (principal instanceof Principal) {
            principalStr = ((Principal) principal).getName();
        } else if (principal != null) {
            principalStr = String.valueOf(principal);
        }

        if (principalStr == null) return Optional.empty();

        try {
            UUID id = UUID.fromString(principalStr);
            return employeeRepository.findById(id);
        } catch (IllegalArgumentException ignored) {
        }

        return employeeRepository.findByEmail(principalStr);
    }

    public Optional<Role> getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        for (GrantedAuthority ga : auth.getAuthorities()) {
            String authority = ga.getAuthority();
            if (authority == null) continue;
            if (authority.equalsIgnoreCase("ROLE_ADMIN") || authority.equalsIgnoreCase("ADMIN")) return Optional.of(Role.ADMIN);
            if (authority.equalsIgnoreCase("ROLE_MANAGER") || authority.equalsIgnoreCase("MANAGER")) return Optional.of(Role.MANAGER);
            if (authority.equalsIgnoreCase("ROLE_EMPLOYEE") || authority.equalsIgnoreCase("EMPLOYEE")) return Optional.of(Role.EMPLOYEE);
        }

        return getCurrentEmployee().map(Employee::getRole);
    }

    public boolean hasRole(Role role) {
        return getCurrentRole().map(r -> r == role).orElse(false);
    }
}

