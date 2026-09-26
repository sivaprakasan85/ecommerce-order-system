package com.siva.ecommerce.dto;

public record LoginResponse(String token, Long userId, String email, String role) {
}
