package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.PageResponse;
import com.siva.ecommerce.dto.ProductRequest;
import com.siva.ecommerce.dto.ProductResponse;
import com.siva.ecommerce.entity.Category;
import com.siva.ecommerce.entity.Product;
import com.siva.ecommerce.exception.BadRequestException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.CategoryRepository;
import com.siva.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ProductService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "createdAt");
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(Long categoryId, String keyword,
                                                int page, int size,
                                                String sortBy, String direction) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);

        Page<Product> result;
        if (categoryId != null) {
            result = productRepository.findByCategoryId(categoryId, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            result = productRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            result = productRepository.findAll(pageable);
        }
        return PageResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = findCategoryOrThrow(request.categoryId());

        Product product = new Product();
        applyRequest(product, request, category);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        Category category = findCategoryOrThrow(request.categoryId());

        applyRequest(product, request, category);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Cannot sort by '" + sortBy
                    + "'. Allowed fields: " + SORTABLE_FIELDS);
        }
        Sort.Direction dir;
        if ("asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new BadRequestException("Direction must be 'asc' or 'desc'");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(dir, sortBy));
    }

    private void applyRequest(Product product, ProductRequest request, Category category) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getImageUrl(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}