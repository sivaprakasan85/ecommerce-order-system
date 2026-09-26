package com.siva.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(example = "siva@example.com")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(example = "password123")
        @NotBlank(message = "Password is required")
        String password
) {
}