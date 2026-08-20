package com.transit.platform.common;

import org.springframework.data.domain.Page;
import java.util.List;

/** Enveloppe standard pour toute réponse API paginée. */
public record PagedApiResponse<T>(boolean success, List<T> data, PaginationMeta pagination) {
    public static <T> PagedApiResponse<T> of(Page<T> page) {
        return new PagedApiResponse<>(true, page.getContent(), PaginationMeta.from(page));
    }
}
