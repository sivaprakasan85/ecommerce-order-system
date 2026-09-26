package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/checkout")
@Tag(name = "Checkout", description = "Convert cart into an order (transactional, stock-safe)")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(summary = "Checkout the current cart into an order")
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(userId));
    }
}