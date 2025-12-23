package com.taskflow.taskflow.util;

import com.taskflow.taskflow.dto.ApiResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PagingResponseBuilder {

    private static final String KEY_ITEMS = "items";
    private static final String KEY_PAGE = "page";
    private static final String KEY_SIZE = "size";
    private static final String KEY_TOTAL_ELEMENTS = "totalElements";
    private static final String KEY_TOTAL_PAGES = "totalPages";

    private PagingResponseBuilder() {
    }

    public static <T, R> ApiResponse build(Page<T> page, Function<? super T, ? extends R> mapper, String message) {
        List<R> items = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        Map<String, Object> data = Map.of(
                KEY_ITEMS, items,
                KEY_PAGE, page.getNumber(),
                KEY_SIZE, page.getSize(),
                KEY_TOTAL_ELEMENTS, page.getTotalElements(),
                KEY_TOTAL_PAGES, page.getTotalPages()
        );

        return ApiResponse.ok(message, data);
    }
}
