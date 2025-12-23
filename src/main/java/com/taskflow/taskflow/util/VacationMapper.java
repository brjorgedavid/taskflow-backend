package com.taskflow.taskflow.util;

import com.taskflow.taskflow.dto.VacationResponse;
import com.taskflow.taskflow.model.Vacation;

public final class VacationMapper {

    private VacationMapper() {}

    public static VacationResponse toResponse(Vacation v) {
        VacationResponse r = new VacationResponse();
        r.setId(v.getId());
        r.setEmployeeId(v.getRequester().getId());
        r.setStartDate(v.getStartDate());
        r.setEndDate(v.getEndDate());
        r.setStatus(v.getStatus());
        r.setApprovalComment(v.getApprovalComment());
        r.setRejectionReason(v.getRejectionReason());
        r.setRequestReason(v.getRequestReason());
        r.setCreatedAt(v.getCreatedAt());
        r.setDecidedAt(v.getDecidedAt());
        r.setDecidedBy(v.getDecidedBy());
        return r;
    }

}
