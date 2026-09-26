package com.siva.ecommerce.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
) {
}