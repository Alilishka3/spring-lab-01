package kz.iitu.springlab.catalog;

import java.time.Instant;

/** The domain object. It never crosses the HTTP boundary: the API speaks BookRequest/BookResponse. */
public record Book(Long id, String title, String author, int year,
                   Genre genre, String isbn, Instant createdAt) { }
