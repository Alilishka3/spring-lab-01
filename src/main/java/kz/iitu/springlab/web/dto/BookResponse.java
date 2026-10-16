package kz.iitu.springlab.web.dto;

import kz.iitu.springlab.catalog.Genre;

import java.time.Instant;

/** What the contract promises to return. */
public record BookResponse(
        Long id,
        String title,
        String author,
        int year,
        Genre genre,
        String isbn,
        Instant createdAt) { }
