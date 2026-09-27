package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.CategoryRequest;
import com.siva.ecommerce.dto.CategoryResponse;
import com.siva.ecommerce.entity.Category;
import com.siva.ecommerce.exception.ConflictException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.CategoryRepository;
import com.siva.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category electronics;

    @BeforeEach
    void setUp() {
        electronics = new Category("Electronics");
        electronics.setId(1L);
    }

    @Test
    void getAll_returnsAllCategoriesMappedToResponses() {
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));

        List<CategoryResponse> result = categoryService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
    }

    @Test
    void getById_whenExists_returnsCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));

        CategoryResponse result = categoryService.getById(1L);

        assertThat(result.name()).isEqualTo("Electronics");
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_whenNameIsNew_savesAndReturnsCategory() {
        CategoryRequest request = new CategoryRequest("Books");
        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        CategoryResponse result = categoryService.create(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Books");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_whenNameAlreadyExists_throwsConflictException() {
        CategoryRequest request = new CategoryRequest("Electronics");
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Electronics");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_whenCategoryHasProducts_throwsConflictException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existing products");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_whenCategoryHasNoProducts_deletesSuccessfully() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(electronics);
    }
}