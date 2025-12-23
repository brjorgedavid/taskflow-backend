package com.taskflow.taskflow.exception;

import java.time.LocalDate;
import java.util.List;

public class OverlappingVacationException extends RuntimeException {
    private final List<Suggestion> suggestions;
    private final int requestedDays;

    public OverlappingVacationException(String message, List<Suggestion> suggestions, int requestedDays) {
        super(message);
        this.suggestions = suggestions;
        this.requestedDays = requestedDays;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public static class Suggestion {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public Suggestion(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }
    }
}
