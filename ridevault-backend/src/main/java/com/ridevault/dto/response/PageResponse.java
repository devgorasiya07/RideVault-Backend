package com.ridevault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> of(Page<?> pageMeta, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(pageMeta.getNumber())
                .size(pageMeta.getSize())
                .totalElements(pageMeta.getTotalElements())
                .totalPages(pageMeta.getTotalPages())
                .first(pageMeta.isFirst())
                .last(pageMeta.isLast())
                .build();
    }
}
