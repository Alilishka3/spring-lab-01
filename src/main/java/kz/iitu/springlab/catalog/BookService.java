package kz.iitu.springlab.catalog;

import kz.iitu.springlab.service.BookNotFoundException;
import kz.iitu.springlab.service.DuplicateTitleException;
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

    /** Catalogue page: author, title fragment and genre combined; every argument is optional. */
    public List<Book> search(String author, String title, Genre genre) {
        String titleNeedle = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        return findAll(author).stream()
                .filter(b -> b.title().toLowerCase(Locale.ROOT).contains(titleNeedle))
                .filter(b -> genre == null || genre == b.genre())
                .toList();
    }

    /** Lab 5, variant 1: case-insensitive search by a fragment of the title. */
    public List<Book> searchByTitle(String fragment) {
        String needle = fragment == null ? "" : fragment.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(b -> b.title().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /** Lab 7, variant 1: one page of the collection (page numbers start at 0). */
    public List<Book> findPage(int page, int size) {
        return repository.findAll().stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    public long count() {
        return repository.count();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book get(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(Book book) {
        ensureUnique(book, null);
        return repository.save(new Book(null, book.title(), book.author(), book.year(),
                book.genre(), book.isbn(), book.createdAt()));
    }

    /** PUT semantics: the whole record is replaced, createdAt is kept from the original. */
    public Book update(long id, Book book) {
        Book existing = get(id);                                   // 404 if absent
        ensureUnique(book, id);
        return repository.save(new Book(id, book.title(), book.author(), book.year(),
                book.genre(), book.isbn(), existing.createdAt()));
    }

    /** A second DELETE of the same id is answered with 404: the resource is gone. */
    public void delete(long id) {
        if (!repository.deleteById(id)) {
            throw new BookNotFoundException(id);
        }
    }

    private void ensureUnique(Book candidate, Long ownId) {
        boolean taken = repository.findAll().stream()
                .filter(b -> !b.id().equals(ownId))
                .anyMatch(b -> b.title().equalsIgnoreCase(candidate.title())
                        && b.author().equalsIgnoreCase(candidate.author()));
        if (taken) {
            throw new DuplicateTitleException(candidate.title());
        }
    }
}
