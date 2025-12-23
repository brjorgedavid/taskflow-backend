package com.taskflow.taskflow.util;

import com.taskflow.taskflow.dto.VacationInput;
import com.taskflow.taskflow.exception.OverlappingVacationException;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VacationHelper {

    private VacationHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void validateVacationInput(VacationInput request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body required");
        }

        if (request.getEndDate() == null || request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be in the past");
        }

        if (request.getStartDate().isEqual(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vacation must be at least one day long");
        }
    }

    public static void validateVacationId(Object id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vacation id is required");
        }
    }

    public static List<Pair<LocalDate, LocalDate>> mergeOverlappingIntervals(
            List<Pair<LocalDate, LocalDate>> intervals) {

        if (intervals.isEmpty()) {
            return new ArrayList<>();
        }

        List<Pair<LocalDate, LocalDate>> sortedIntervals = new ArrayList<>(intervals);
        sortedIntervals.sort(Comparator.comparing(Pair::getFirst));

        List<Pair<LocalDate, LocalDate>> mergedIntervals = new ArrayList<>();
        Pair<LocalDate, LocalDate> currentInterval = sortedIntervals.get(0);
        LocalDate currentStart = currentInterval.getFirst();
        LocalDate currentEnd = currentInterval.getSecond();

        for (int i = 1; i < sortedIntervals.size(); i++) {
            Pair<LocalDate, LocalDate> nextInterval = sortedIntervals.get(i);
            LocalDate nextStart = nextInterval.getFirst();
            LocalDate nextEnd = nextInterval.getSecond();

            boolean overlapsOrAdjacent = !currentEnd.isBefore(nextStart.minusDays(1));

            if (overlapsOrAdjacent) {
                currentEnd = maxDate(currentEnd, nextEnd);
            } else {
                mergedIntervals.add(Pair.of(currentStart, currentEnd));
                currentStart = nextStart;
                currentEnd = nextEnd;
            }
        }

        mergedIntervals.add(Pair.of(currentStart, currentEnd));
        return mergedIntervals;
    }

    public static List<OverlappingVacationException.Suggestion> generateVacationSuggestions(
            List<Pair<LocalDate, LocalDate>> occupiedIntervals,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate,
            LocalDate searchWindowStart,
            LocalDate searchWindowEnd,
            int maxSuggestions) {

        List<OverlappingVacationException.Suggestion> suggestions = new ArrayList<>();
        long requestedDurationInDays = calculateDurationInDays(requestedStartDate, requestedEndDate);

        if (occupiedIntervals.isEmpty()) {
            suggestions.add(createSuggestion(requestedStartDate, requestedEndDate));
            return suggestions;
        }

        LocalDate searchCursor = searchWindowStart;

        for (Pair<LocalDate, LocalDate> occupiedInterval : occupiedIntervals) {
            LocalDate occupiedStart = occupiedInterval.getFirst();
            LocalDate occupiedEnd = occupiedInterval.getSecond();

            if (suggestions.size() >= maxSuggestions) {
                break;
            }

            if (searchCursor.isBefore(occupiedStart)) {
                LocalDate availableEnd = occupiedStart.minusDays(1);
                long availableDuration = calculateDurationInDays(searchCursor, availableEnd);

                if (availableDuration >= requestedDurationInDays) {
                    LocalDate suggestionEnd = searchCursor.plusDays(requestedDurationInDays - 1);
                    suggestions.add(createSuggestion(searchCursor, suggestionEnd));
                }
            }

            searchCursor = maxDate(searchCursor, occupiedEnd.plusDays(1));
        }

        if (suggestions.size() < maxSuggestions && searchCursor.isBefore(searchWindowEnd)) {
            LocalDate suggestionEnd = searchCursor.plusDays(requestedDurationInDays - 1);

            if (!suggestionEnd.isAfter(searchWindowEnd)) {
                suggestions.add(createSuggestion(searchCursor, suggestionEnd));
            }
        }

        return suggestions;
    }

    public static long calculateVacationDuration(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public static LocalDate calculateSearchWindowStart(LocalDate requestStartDate, int daysToLookBack) {
        return requestStartDate.minusDays(daysToLookBack);
    }

    public static LocalDate calculateSearchWindowEnd(
            LocalDate requestEndDate,
            int daysToLookForward,
            long requestDuration) {
        return requestEndDate.plusDays(daysToLookForward + requestDuration);
    }

    private static long calculateDurationInDays(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private static OverlappingVacationException.Suggestion createSuggestion(LocalDate startDate, LocalDate endDate) {
        return new OverlappingVacationException.Suggestion(startDate, endDate);
    }

    private static LocalDate maxDate(LocalDate date1, LocalDate date2) {
        return date1.isAfter(date2) ? date1 : date2;
    }
}

