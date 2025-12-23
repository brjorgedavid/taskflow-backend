package com.taskflow.taskflow.model;

import com.taskflow.taskflow.data.VacationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vacations")
@Data
public class Vacation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate createdAt = LocalDate.now();
    private LocalDate endDate;
    private LocalDate decidedAt;

    private UUID decidedBy;

    @Enumerated(EnumType.STRING)
    private VacationStatus status = VacationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee requester;

    private String rejectionReason;
    private String approvalComment;
    private String requestReason;
}
