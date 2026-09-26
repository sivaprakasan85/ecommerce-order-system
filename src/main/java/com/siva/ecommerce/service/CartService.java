package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.CartItemRequest;
import com.siva.ecommerce.dto.CartItemResponse;
import com.siva.ecommerce.dto.CartResponse;
import com.siva.ecommerce.entity.Cart;
import com.siva.ecommerce.entity.CartItem;
import com.siva.ecommerce.entity.Product;
import com.siva.ecommerce.exception.InsufficientStockException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.CartItemRepository;
import com.siva.ecommerce.repository.CartRepository;
import com.siva.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return toResponse(findCartOrThrow(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = findCartOrThrow(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.productId()));

        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int currentQtyInCart = (existing != null) ? existing.getQuantity() : 0;
        int newTotalQty = currentQtyInCart + request.quantity();

        if (product.getStockQuantity() < newTotalQty) {
            throw new InsufficientStockException(
                    "Insufficient stock for '" + product.getName() + "'. Available: "
                            + product.getStockQuantity() + ", requested total: " + newTotalQty);
        }

        if (existing != null) {
            existing.setQuantity(newTotalQty);
        } else {
            cart.addItem(new CartItem(product, request.quantity()));
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long itemId, int quantity) {
        Cart cart = findCartOrThrow(userId);
        CartItem item = findItemInCartOrThrow(cart, itemId);

        if (item.getProduct().getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for '" + item.getProduct().getName() + "'. Available: "
                            + item.getProduct().getStockQuantity() + ", requested: " + quantity);
        }

        item.setQuantity(quantity);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = findCartOrThrow(userId);
        CartItem item = findItemInCartOrThrow(cart, itemId);
        cart.removeItem(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = findCartOrThrow(userId);
        cart.getItems().clear();
        return toResponse(cartRepository.save(cart));
    }

    private Cart findCartOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
    }

    private CartItem findItemInCartOrThrow(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + itemId + " in this cart"));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), cart.getUser().getId(), itemResponses, total);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                subtotal
        );
    }
}