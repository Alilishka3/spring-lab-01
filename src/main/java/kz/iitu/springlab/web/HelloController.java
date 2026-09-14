package kz.iitu.springlab.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "world") String name) {
        return new Greeting("Hello, " + name + "!", owner, LocalDateTime.now());
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(owner,
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }

    @GetMapping("/reverse")
    public ReverseResult reverse(@RequestParam(required = false) String text) {
        if (text == null || text.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Parameter 'text' is required");
        }
        String reversed = new StringBuilder(text).reverse().toString();
        return new ReverseResult(text, reversed, text.length());
    }

    public record Greeting(String message, String owner, LocalDateTime timestamp) { }
    public record Info(String owner, String javaVersion, int cpuCores) { }
    public record ReverseResult(String original, String reversed, int length) { }
}