package com.taskflow.taskflow.security;

import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public EmployeeUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee emp = employeeRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String pwd = emp.getPasswordHash();
        if (pwd == null) throw new UsernameNotFoundException("User has no password set");
        return User.withUsername(username)
                .password(pwd)
                .roles(emp.getRole().name())
                .build();
    }
}

