package com.example.catalog;

import com.example.catalog.ProductDtos.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) { this.repo = repo; }

    @Transactional
    public ProductResponse create(CreateRequest r) {
        if (repo.existsBySku(r.sku()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        Product p = new Product();
        p.setSku(r.sku());
        p.setName(r.name());
        p.setDescription(r.description());
        p.setCategory(r.category());
        p.setPriceCents(r.priceCents());
        p.setStock(r.stock());
        try {
            return ProductResponse.from(repo.saveAndFlush(p));
        } catch (DataIntegrityViolationException e) {   // race: two creates with same SKU
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        }
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse get(long id) {
        return ProductResponse.from(find(id));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public ProductResponse update(long id, UpdateRequest r) {
        Product p = find(id);
        if (r.name() != null) p.setName(r.name());
        if (r.description() != null) p.setDescription(r.description());
        if (r.category() != null) p.setCategory(r.category());
        if (r.priceCents() != null) p.setPriceCents(r.priceCents());
        if (r.stock() != null) p.setStock(r.stock());
        return ProductResponse.from(repo.saveAndFlush(p));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void delete(long id) {
        repo.delete(find(id));
    }

    /**
     * Keyset pagination ordered by (priceCents, id).
     * Cost is O(log n) at any depth, unlike OFFSET which scans and discards skipped rows.
     */
    @Transactional(readOnly = true)
    public PageResponse list(String category, Long minPrice, Long maxPrice, int limit, String cursor) {
        long[] after = decodeCursor(cursor);

        Specification<Product> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (category != null && !category.isBlank()) ps.add(cb.equal(root.get("category"), category));
            if (minPrice != null) ps.add(cb.greaterThanOrEqualTo(root.get("priceCents"), minPrice));
            if (maxPrice != null) ps.add(cb.lessThanOrEqualTo(root.get("priceCents"), maxPrice));
            if (after != null) {   // (price, id) > (lastPrice, lastId)
                ps.add(cb.or(
                    cb.greaterThan(root.get("priceCents"), after[0]),
                    cb.and(cb.equal(root.get("priceCents"), after[0]),
                           cb.greaterThan(root.get("id"), after[1]))));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        // Fetch limit + 1 to know whether another page exists, with no COUNT(*) query
        List<Product> rows = repo.findAll(spec,
            PageRequest.of(0, limit + 1, Sort.by("priceCents").and(Sort.by("id")))).getContent();

        boolean more = rows.size() > limit;
        List<Product> page = more ? rows.subList(0, limit) : rows;
        String next = more ? encodeCursor(page.get(page.size() - 1)) : null;
        return new PageResponse(page.stream().map(ProductResponse::from).toList(), next);
    }

    private Product find(long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }

    private static String encodeCursor(Product p) {
        String raw = p.getPriceCents() + "," + p.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static long[] decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(",");
            return new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1])};
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }
}
