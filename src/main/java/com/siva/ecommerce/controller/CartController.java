package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.CartItemRequest;
import com.siva.ecommerce.dto.CartResponse;
import com.siva.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/cart")
@Tag(name = "Cart", description = "Manage a user's shopping cart (no login yet — userId passed directly)")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "View the current cart")
    @GetMapping
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @Operation(summary = "Add a product to the cart (checks stock availability)")
    @PostMapping("/items")
    public CartResponse addItem(@PathVariable Long userId,
                                @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(userId, request);
    }

    @Operation(summary = "Update the quantity of a cart item")
    @PutMapping("/items/{itemId}")
    public CartResponse updateItemQuantity(@PathVariable Long userId,
                                           @PathVariable Long itemId,
                                           @Parameter(example = "3")
                                           @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
        return cartService.updateItemQuantity(userId, itemId, quantity);
    }

    @Operation(summary = "Remove one item from the cart")
    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long userId, @PathVariable Long itemId) {
        return cartService.removeItem(userId, itemId);
    }

    @Operation(summary = "Remove all items from the cart")
    @DeleteMapping
    public CartResponse clearCart(@PathVariable Long userId) {
        return cartService.clearCart(userId);
    }
}