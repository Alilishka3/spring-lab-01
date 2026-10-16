package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class BookRepository {

    private final Map<Long, Book> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public BookRepository() {                       // the first three come from the lab 5 listing
        save(new Book(null, "Effective Java", "Joshua Bloch", 2018, Genre.PROGRAMMING, "9780134685991", null));
        save(new Book(null, "Clean Code", "Robert Martin", 2008, Genre.PROGRAMMING, "9780132350884", null));
        save(new Book(null, "Spring in Action", "Craig Walls", 2022, Genre.PROGRAMMING, "9781617297571", null));
        save(new Book(null, "Java Concurrency in Practice", "Brian Goetz", 2006, Genre.PROGRAMMING, "9780321349606", null));
        save(new Book(null, "Design Patterns", "Erich Gamma", 1994, Genre.PROGRAMMING, "9780201633610", null));
        save(new Book(null, "Introduction to Algorithms", "Thomas Cormen", 2009, Genre.SCIENCE, "9780262033848", null));
        save(new Book(null, "A Brief History of Time", "Stephen Hawking", 1998, Genre.SCIENCE, "9780553380163", null));
        save(new Book(null, "The Hobbit", "J. R. R. Tolkien", 1937, Genre.FANTASY, "9780547928227", null));
        save(new Book(null, "The Alchemist", "Paulo Coelho", 1988, Genre.NOVEL, "9780062316097", null));
        save(new Book(null, "Great Expectations", "Charles Dickens", 1861, Genre.NOVEL, "9780141439518", null));
        save(new Book(null, "Head First Java", "Kathy Sierra", 2005, Genre.PROGRAMMING, "9780596009205", null));
    }

    public List<Book> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(Book::id))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Book save(Book book) {
        long id = book.id() != null ? book.id() : sequence.incrementAndGet();
        Instant createdAt = book.createdAt() != null ? book.createdAt() : Instant.now();
        Book stored = new Book(id, book.title(), book.author(), book.year(),
                book.genre(), book.isbn(), createdAt);
        storage.put(id, stored);
        return stored;
    }

    public long count() {
        return storage.size();
    }

    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }
}
