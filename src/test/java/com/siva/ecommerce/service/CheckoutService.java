package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.entity.*;
import com.siva.ecommerce.exception.EmptyCartException;
import com.siva.ecommerce.exception.InsufficientStockException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.CartRepository;
import com.siva.ecommerce.repository.OrderRepository;
import com.siva.ecommerce.repository.PaymentRepository;
import com.siva.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    private User user;
    private Product keyboard;
    private Product mouse;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        keyboard = new Product();
        keyboard.setId(2L);
        keyboard.setName("Keyboard");
        keyboard.setPrice(new BigDecimal("999.00"));
        keyboard.setStockQuantity(40);

        mouse = new Product();
        mouse.setId(3L);
        mouse.setName("Mouse");
        mouse.setPrice(new BigDecimal("499.00"));
        mouse.setStockQuantity(1); // deliberately low stock — this is the item that will fail

        cart = new Cart(user);
    }

    @Test
    void checkout_withSufficientStockForAllItems_createsOrderAndDeductsStock() {
        cart.addItem(new CartItem(keyboard, 2));
        cart.addItem(new CartItem(mouse, 1));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = checkoutService.checkout(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.totalAmount()).isEqualByComparingTo("2497.00"); // (999*2) + (499*1)
        assertThat(response.items()).hasSize(2);

        // Stock must have been deducted for BOTH products
        assertThat(keyboard.getStockQuantity()).isEqualTo(38);
        assertThat(mouse.getStockQuantity()).isEqualTo(0);

        // Order, payment, and product saves must all have happened
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(productRepository, times(2)).save(any(Product.class));

        // The cart must be empty after a successful checkout
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void checkout_whenOneItemHasInsufficientStock_rollsBackWithNoSideEffects() {
        // Keyboard (plenty of stock) is added FIRST, Mouse (only 1 in stock) SECOND.
        // Requesting 2 of Mouse should fail — proving the failure on the
        // SECOND item prevents ANY side effect, including on the FIRST item.
        cart.addItem(new CartItem(keyboard, 2));
        cart.addItem(new CartItem(mouse, 2)); // only 1 in stock — this must fail

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> checkoutService.checkout(1L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Mouse");

        // THE CRITICAL ASSERTIONS: proving no partial state occurred.
        // Keyboard's stock must be UNCHANGED, even though it alone was valid
        // and was processed first in the loop.
        assertThat(keyboard.getStockQuantity()).isEqualTo(40);
        assertThat(mouse.getStockQuantity()).isEqualTo(1);

        // Nothing should have been saved at all — no order, no payment, no product update.
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(productRepository, never()).save(any());

        // The cart must be untouched, ready for the user to retry.
        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void checkout_withEmptyCart_throwsEmptyCartException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart)); // cart has no items

        assertThatThrownBy(() -> checkoutService.checkout(1L))
                .isInstanceOf(EmptyCartException.class)
                .hasMessageContaining("empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_whenCartNotFound_throwsResourceNotFoundException() {
        when(cartRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void checkout_copiesCurrentPriceIntoOrderItem_asPriceAtPurchase() {
        cart.addItem(new CartItem(keyboard, 1));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = checkoutService.checkout(1L);

        // Confirms priceAtPurchase is captured from the product's CURRENT price,
        // independent of whatever the product's price might become later.
        assertThat(response.items().get(0).priceAtPurchase()).isEqualByComparingTo("999.00");
    }
}