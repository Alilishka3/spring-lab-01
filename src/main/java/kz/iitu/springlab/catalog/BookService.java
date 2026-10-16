package kz.iitu.springlab.catalog;

import kz.iitu.springlab.service.BookNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    /** Every book when author is null or blank, otherwise only the books of that author. */
    public List<Book> findAll(String author) {
        if (author == null || author.isBlank()) {
            return repository.findAll();
        }
        String needle = author.toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(b -> b.author().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /** Variant 1: case-insensitive search by a fragment of the title. */
    public List<Book> searchByTitle(String fragment) {
        String needle = fragment == null ? "" : fragment.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(b -> b.title().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    /** Throws BookNotFoundException instead of returning an empty Optional. */
    public Book get(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(Book book) {
        return repository.save(new Book(null, book.title(), book.author(), book.year(),
                book.genre(), book.isbn()));
    }

    public Optional<Book> update(long id, Book book) {
        return repository.findById(id)
                .map(existing -> repository.save(new Book(id, book.title(), book.author(),
                        book.year(), book.genre(), book.isbn())));
    }

    public boolean delete(long id) {
        return repository.deleteById(id);
    }
}
