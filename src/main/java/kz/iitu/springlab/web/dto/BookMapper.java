package kz.iitu.springlab.web.dto;

import kz.iitu.springlab.catalog.Book;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** The one place that knows both shapes. */
@Component
public class BookMapper {

    public Book toDomain(BookRequest request) {
        return new Book(null, request.title(), request.author(),
                request.year(), request.genre(), blankToNull(request.isbn()), Instant.now());
    }

    public BookResponse toResponse(Book book) {
        return new BookResponse(book.id(), book.title(), book.author(),
                book.year(), book.genre(), book.isbn(), book.createdAt());
    }

    public List<BookResponse> toResponseList(List<Book> books) {
        return books.stream().map(this::toResponse).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
