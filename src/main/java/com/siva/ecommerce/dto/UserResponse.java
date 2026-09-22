package com.siva.ecommerce.dto;

import com.siva.ecommerce.entity.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}