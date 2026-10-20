package com.example.catalog;

import com.example.catalog.ProductDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/products")
@Validated
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateRequest req) {
        ProductResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/products/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable long id) {
        ProductResponse p = service.get(id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())  // CDN/browser cacheable
            .eTag("\"" + p.id() + "-" + p.version() + "\"")   // Spring answers 304 on If-None-Match
            .body(p);
    }

    @GetMapping
    public PageResponse list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @Min(0) Long minPrice,
            @RequestParam(required = false) @Min(0) Long maxPrice,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String cursor) {
        return service.list(category, minPrice, maxPrice, limit, cursor);
    }

    @PatchMapping("/{id}")
    public ProductResponse update(@PathVariable long id, @Valid @RequestBody UpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
