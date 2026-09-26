package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.security.CurrentUser;
import com.siva.ecommerce.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@Tag(name = "Checkout", description = "Convert the logged-in user's cart into an order")
@SecurityRequirement(name = "bearerAuth")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CurrentUser currentUser;

    public CheckoutController(CheckoutService checkoutService, CurrentUser currentUser) {
        this.checkoutService = checkoutService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Checkout the current cart into an order")
    @PostMapping
    public ResponseEntity<OrderResponse> checkout() {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(currentUser.getId()));
    }
}