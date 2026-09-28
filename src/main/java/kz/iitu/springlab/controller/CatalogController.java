package kz.iitu.springlab.controller;

import kz.iitu.springlab.service.CatalogService;
import org.springframework.web.bind.annotation.*;

import org.springframework.aop.support.AopUtils;
import java.util.Map;

import java.util.List;

@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/lab4/item/{id}")
    public String item(@PathVariable long id) {
        return catalogService.findById(id);
    }
    @GetMapping("/api/lab4/remove-twice/{id}")
    public String removeTwice(@PathVariable long id) {
        return catalogService.removeTwice(id);
    }
    @GetMapping("/api/lab4/items")
    public List<String> items(@RequestParam(defaultValue = "5") int limit) {
        return catalogService.findAll(limit);
    }

    @DeleteMapping("/api/lab4/item/{id}")
    public String remove(@PathVariable long id) {
        return catalogService.remove(id);
    }
    @GetMapping("/api/lab4/proxy")
    public Map<String, String> proxyInfo() {
        return Map.of(
                "className", catalogService.getClass().getName(),
                "superClass", catalogService.getClass().getSuperclass().getSimpleName(),
                "isAopProxy", String.valueOf(AopUtils.isAopProxy(catalogService)),
                "isCglib", String.valueOf(AopUtils.isCglibProxy(catalogService)));
    }
}