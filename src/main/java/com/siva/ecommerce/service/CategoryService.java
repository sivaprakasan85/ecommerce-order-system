package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.CategoryRequest;
import com.siva.ecommerce.dto.CategoryResponse;
import com.siva.ecommerce.entity.Category;
import com.siva.ecommerce.exception.ConflictException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.CategoryRepository;
import com.siva.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Category already exists: " + name);
        }
        Category saved = categoryRepository.save(new Category(name));
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        String name = request.name().trim();

        categoryRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Category already exists: " + name);
            }
        });

        category.setName(name);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete category with existing products");
        }
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}