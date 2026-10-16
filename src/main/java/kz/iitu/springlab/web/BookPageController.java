package kz.iitu.springlab.web;

import jakarta.validation.Valid;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.catalog.Genre;
import kz.iitu.springlab.service.DuplicateTitleException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** The HTML half of the application: catalogue, book page, add/edit form, delete. */
@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService service;

    public BookPageController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String author,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Genre genre,
                       Model model) {
        List<Book> books = service.search(author, q, genre);
        model.addAttribute("books", books);
        model.addAttribute("author", author);
        model.addAttribute("q", q);
        model.addAttribute("genre", genre);
        model.addAttribute("genres", Genre.values());
        model.addAttribute("total", service.count());
        model.addAttribute("filtered", author != null || q != null || genre != null);
        return "books/list";                                    // -> templates/books/list.html
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model) {
        Book book = service.get(id);                            // 404 page via PageExceptionHandler
        model.addAttribute("book", book);
        model.addAttribute("createdAt", LocalDateTime.ofInstant(book.createdAt(), ZoneId.systemDefault()));
        return "books/details";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());              // an empty object for th:object
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,                    // immediately after!
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());         // the model again
            return "books/form";
        }
        try {
            Book saved = service.create(toBook(form));
            flash(redirect, "flash.added", saved.title());
            return "redirect:/books/" + saved.id();               // Post/Redirect/Get
        } catch (DuplicateTitleException ex) {
            binding.reject("book.duplicate");                     // a global error
            model.addAttribute("genres", Genre.values());
            return "books/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Book book = service.get(id);
        BookForm form = new BookForm();
        form.setTitle(book.title());
        form.setAuthor(book.author());
        form.setYear(book.year());
        form.setGenre(book.genre());
        form.setIsbn(book.isbn());
        form.setAvailable(true);
        model.addAttribute("form", form);
        model.addAttribute("bookId", id);
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                Book saved = service.update(id, toBook(form));
                flash(redirect, "flash.updated", saved.title());
                return "redirect:/books/" + id;
            } catch (DuplicateTitleException ex) {
                binding.reject("book.duplicate");
            }
        }
        model.addAttribute("bookId", id);
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        Book book = service.get(id);
        service.delete(id);
        flash(redirect, "flash.deleted", book.title());
        return "redirect:/books";
    }

    private static Book toBook(BookForm form) {
        String isbn = form.getIsbn() == null || form.getIsbn().isBlank() ? null : form.getIsbn().trim();
        return new Book(null, form.getTitle().trim(), form.getAuthor().trim(),
                form.getYear(), form.getGenre(), isbn, null);
    }

    private static void flash(RedirectAttributes redirect, String key, String title) {
        redirect.addFlashAttribute("messageKey", key);
        redirect.addFlashAttribute("messageArg", title);
    }
}
