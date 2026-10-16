package kz.iitu.springlab.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.config.AppProperties;
import kz.iitu.springlab.web.dto.BookMapper;
import kz.iitu.springlab.web.dto.BookRequest;
import kz.iitu.springlab.web.dto.BookResponse;
import kz.iitu.springlab.web.dto.PageResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Books", description = "The catalogue of books")
@RestController
@RequestMapping("/api/books")
public class BookApiController {

    private final BookService service;
    private final BookMapper mapper;
    private final AppProperties props;

    public BookApiController(BookService service, BookMapper mapper, AppProperties props) {
        this.service = service;
        this.mapper = mapper;
        this.props = props;
    }

    @Operation(summary = "List all books, optionally filtered by author")
    @GetMapping                                                          // 200
    public List<BookResponse> list(@RequestParam(required = false) String author) {
        return mapper.toResponseList(service.findAll(author));
    }

    @Operation(summary = "One page of the catalogue (variant 1)",
            description = "Selected when the page parameter is present. "
                    + "The default and the maximum size come from app.pagination (lab 3).")
    @GetMapping(params = "page")                                         // 200 or 400
    public PageResponse<BookResponse> page(
            @Parameter(description = "Page number, starting at 0", example = "0")
            @RequestParam @Min(0) int page,
            @Parameter(description = "Page size", example = "5")
            @RequestParam(required = false) @Min(1) @Max(500) Integer size) {
        int effectiveSize = Math.min(size != null ? size : props.pagination().defaultSize(),
                props.pagination().maxSize());
        long total = service.count();
        int totalPages = (int) Math.ceil((double) total / effectiveSize);
        return new PageResponse<>(
                mapper.toResponseList(service.findPage(page, effectiveSize)),
                page, effectiveSize, total, totalPages);
    }

    @Operation(summary = "Search by a fragment of the title (lab 5, variant 1)")
    @GetMapping("/search")                                               // 200, [] if nothing
    public List<BookResponse> search(@RequestParam(defaultValue = "") String title) {
        return mapper.toResponseList(service.searchByTitle(title));
    }

    @Operation(summary = "Find a book by its identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The book was found"),
            @ApiResponse(responseCode = "404", description = "No book with this id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")                                                 // 200 or 404
    public BookResponse find(
            @Parameter(description = "Identifier of the book", example = "42")
            @PathVariable long id) {
        return mapper.toResponse(service.get(id));
    }

    @Operation(summary = "Create a book")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created; the Location header holds the new URI"),
            @ApiResponse(responseCode = "400", description = "The body breaks a validation rule"),
            @ApiResponse(responseCode = "409", description = "The same title by the same author exists")
    })
    @PostMapping                                                         // 201 + Location
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        Book saved = service.create(mapper.toDomain(request));
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.id()))
                .body(mapper.toResponse(saved));
    }

    @Operation(summary = "Replace a book")
    @PutMapping("/{id}")                                                 // 200 or 404
    public BookResponse update(@PathVariable long id,
                               @Valid @RequestBody BookRequest request) {
        return mapper.toResponse(service.update(id, mapper.toDomain(request)));
    }

    @Operation(summary = "Delete a book")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "No book with this id (also on a repeated DELETE)")
    })
    @DeleteMapping("/{id}")                                              // 204 or 404
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
