package com.taskflow.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeManagerResponse {
    private UUID id;
    private String firstName;
    private String lastName;
}

