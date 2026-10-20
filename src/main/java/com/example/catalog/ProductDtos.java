package com.example.catalog;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 64) String category,
        @Min(0) long priceCents,
        @Min(0) int stock) {}

    // All fields optional: partial update (PATCH semantics)
    public record UpdateRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 4000) String description,
        @Size(min = 1, max = 64) String category,
        @Min(0) Long priceCents,
        @Min(0) Integer stock) {}

    public record ProductResponse(Long id, String sku, String name, String description,
                                  String category, long priceCents, int stock,
                                  Instant updatedAt, long version) {
        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getCategory(), p.getPriceCents(), p.getStock(), p.getUpdatedAt(), p.getVersion());
        }
    }

    public record PageResponse(List<ProductResponse> items, String nextCursor) {}
}
