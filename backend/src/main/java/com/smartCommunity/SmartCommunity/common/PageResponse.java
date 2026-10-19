package com.smartCommunity.SmartCommunity.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Simple, stable JSON shape for any paginated list.
 * (We don't return Spring's Page directly because its JSON is huge and not guaranteed stable.)
 *
 * page is 0-based: the first page is page=0.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {

    // "items" are the already-converted DTOs, "page" is the Spring Page they came from
    public static <T> PageResponse<T> of(List<T> items, Page<?> page) {
        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
