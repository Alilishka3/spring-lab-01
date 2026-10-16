package kz.iitu.springlab.web.dto;

import java.util.List;

/** Lab 7, variant 1: a page of the collection together with the paging data. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) { }
