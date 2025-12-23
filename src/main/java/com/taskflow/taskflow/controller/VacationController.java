package com.taskflow.taskflow.controller;

import com.taskflow.taskflow.dto.ApiResponse;
import com.taskflow.taskflow.dto.VacationDecisionInput;
import com.taskflow.taskflow.dto.VacationInput;
import com.taskflow.taskflow.dto.VacationResponse;
import com.taskflow.taskflow.model.Vacation;
import com.taskflow.taskflow.service.VacationService;
import com.taskflow.taskflow.util.PagingResponseBuilder;
import com.taskflow.taskflow.util.VacationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static com.taskflow.taskflow.util.VacationMapper.toResponse;

@RestController
@RequestMapping("/vacations")
@Tag(name = "Vacations", description = "Endpoints for managing vacation requests")
@SecurityRequirement(name = "bearerAuth")
public class VacationController {

    private final VacationService vacationService;

    public VacationController(VacationService vacationService) {
        this.vacationService = vacationService;
    }

    @PostMapping
    @Operation(
        summary = "Create Vacation Request",
        description = "Creates a new vacation request for the current user"
    )
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody VacationInput vacationInput) {
        Vacation created = vacationService.create(vacationInput);
        VacationResponse resp = toResponse(created);
        ApiResponse body = ApiResponse.of(HttpStatus.CREATED.value(), "Vacation request created", resp);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    @Operation(
        summary = "List Vacations",
        description = "Lists all vacation requests (Admin/Manager only)"
    )
    public ApiResponse getVacations(
        @Parameter(description = "Número da página (0-based)")
        @RequestParam(value = "page", required = false) Integer page) {
        int pageIndex = page == null ? 0 : page;
        if (pageIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        Page<Vacation> pageResult = vacationService.findAll(pageIndex);
        return PagingResponseBuilder.build(pageResult, VacationMapper::toResponse, "Vacations fetched successfully");
    }


    @PreAuthorize("@authorizationService.isAdminOrOwner(#id)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete Vacation Request",
            description = "Deletes a vacation request by ID (Admin or Owner only)"
    )
    public ApiResponse delete(@PathVariable UUID id) {
        vacationService.delete(id);
        return ApiResponse.ok("Employee deleted successfully");
    }

    @PreAuthorize("@authorizationService.isAdminOrOwner(#id)")
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Vacation Request",
        description = "Fetches a vacation request by ID (Admin or Owner only)"
    )
    public ApiResponse getOne(@PathVariable UUID id) {
        Vacation vacation = vacationService.findById(id);
        return ApiResponse.ok("Vacation fetched successfully", VacationMapper.toResponse(vacation));
    }

    @PreAuthorize("@authorizationService.isManager(#id)")
    @PatchMapping("/{id}/decision")
    @Operation(
        summary = "Decide on Vacation Request",
        description = "Approves or rejects a vacation request (Manager only)"
    )
    public ApiResponse decide(
        @Parameter(description = "ID da solicitação de férias")
        @PathVariable UUID id,
        @Valid @RequestBody VacationDecisionInput req) {
        Vacation updated = vacationService.decide(id, req.getApproved(), req.getComment());
        return ApiResponse.ok("Vacation decision applied", VacationMapper.toResponse(updated));
    }
}
