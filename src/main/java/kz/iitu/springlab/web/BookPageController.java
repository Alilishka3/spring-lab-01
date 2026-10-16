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

@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService service;

    public BookPageController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String author, Model model) {
        model.addAttribute("books", service.findAll(author));
        model.addAttribute("author", author);
        model.addAttribute("title", "Catalogue");
        return "books/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("book", service.get(id));     // 404 page via PageExceptionHandler
        return "books/details";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());          // an empty object for th:object
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,                // immediately after!
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());     // the model again
            return "books/form";
        }
        Book saved;
        try {
            saved = service.create(new Book(null, form.getTitle(), form.getAuthor(),
                    form.getYear(), form.getGenre(), form.getIsbn(), null));
        } catch (DuplicateTitleException ex) {
            binding.reject("book.duplicate", ex.getMessage());   // a global error
            model.addAttribute("genres", Genre.values());
            return "books/form";
        }
        redirect.addFlashAttribute("message",
                "Book «" + saved.title() + "» has been added");
        return "redirect:/books";
    }
}
