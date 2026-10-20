package com.example.catalog;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "products", indexes = {
    // category + price range, sorted by (price, id): serves the main list query
    @Index(name = "ix_products_cat_price_id", columnList = "category, priceCents, id"),
    @Index(name = "ix_products_price_id", columnList = "priceCents, id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"))
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)  private String sku;
    @Column(nullable = false, length = 255) private String name;
    @Column(length = 4000)                  private String description;
    @Column(nullable = false, length = 64)  private String category;
    @Column(nullable = false)               private long priceCents;   // money as integer, never float
    @Column(nullable = false)               private int stock;
    @Column(nullable = false)               private Instant updatedAt = Instant.now();

    @Version private long version;   // optimistic locking, also used for ETag

    @PrePersist @PreUpdate void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getPriceCents() { return priceCents; }
    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
