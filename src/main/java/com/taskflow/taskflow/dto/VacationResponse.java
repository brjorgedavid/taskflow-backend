package com.taskflow.taskflow.dto;

import com.taskflow.taskflow.data.VacationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VacationResponse {
    private UUID id;
    private UUID employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate createdAt;
    private LocalDate decidedAt;
    private UUID decidedBy;
    private VacationStatus status;
    private String rejectionReason;
    private String approvalComment;
    private String requestReason;
}

