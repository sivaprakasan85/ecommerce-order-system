package com.siva.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @Schema(example = "Wireless Mouse")
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters")
        String name,

        @Schema(example = "Ergonomic 2.4GHz wireless mouse")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Schema(example = "799.00")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @Digits(integer = 8, fraction = 2, message = "Price allows up to 8 digits and 2 decimals")
        BigDecimal price,

        @Schema(example = "50")
        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Schema(example = "https://example.com/mouse.jpg")
        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        @Schema(example = "1")
        @NotNull(message = "Category id is required")
        Long categoryId
) {
}