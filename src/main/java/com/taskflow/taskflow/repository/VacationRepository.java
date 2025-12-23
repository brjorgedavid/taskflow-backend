package com.taskflow.taskflow.repository;

import com.taskflow.taskflow.data.VacationStatus;
import com.taskflow.taskflow.model.Employee;
import com.taskflow.taskflow.model.Vacation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VacationRepository extends JpaRepository<Vacation, UUID> {
    List<Vacation> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(LocalDate endDate, LocalDate startDate, List<VacationStatus> statuses);

    Page<Vacation> findByRequester(Employee requester, Pageable pageable);

    Page<Vacation> findByRequesterIn(List<Employee> requesters, Pageable pageable);
}
