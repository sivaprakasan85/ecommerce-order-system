package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.CartItemRequest;
import com.siva.ecommerce.dto.CartResponse;
import com.siva.ecommerce.security.CurrentUser;
import com.siva.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Manage the logged-in user's shopping cart")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    public CartController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "View the current cart")
    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart(currentUser.getId());
    }

    @Operation(summary = "Add a product to the cart (checks stock availability)")
    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(currentUser.getId(), request);
    }

    @Operation(summary = "Update the quantity of a cart item")
    @PutMapping("/items/{itemId}")
    public CartResponse updateItemQuantity(@PathVariable Long itemId,
                                           @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
        return cartService.updateItemQuantity(currentUser.getId(), itemId, quantity);
    }

    @Operation(summary = "Remove one item from the cart")
    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long itemId) {
        return cartService.removeItem(currentUser.getId(), itemId);
    }

    @Operation(summary = "Remove all items from the cart")
    @DeleteMapping
    public CartResponse clearCart() {
        return cartService.clearCart(currentUser.getId());
    }
}