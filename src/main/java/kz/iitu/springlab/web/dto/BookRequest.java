package kz.iitu.springlab.web.dto;

import jakarta.validation.constraints.*;
import kz.iitu.springlab.catalog.Genre;
import kz.iitu.springlab.web.Isbn;

/** What a client is allowed to send: no id, no createdAt — those belong to the server. */
public record BookRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String author,
        @Min(1450) @Max(2100) int year,
        @NotNull Genre genre,
        @Isbn String isbn) { }
