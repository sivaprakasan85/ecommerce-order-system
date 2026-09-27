package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.entity.*;
import com.siva.ecommerce.exception.InvalidOrderStateException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.OrderRepository;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User owner;
    private User otherUser;
    private Product keyboard;
    private Order order;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);

        otherUser = new User();
        otherUser.setId(2L);

        keyboard = new Product();
        keyboard.setId(2L);
        keyboard.setName("Keyboard");
        keyboard.setStockQuantity(38); // already reduced by a prior checkout

        order = new Order(owner, new BigDecimal("1998.00"));
        order.addItem(new OrderItem(keyboard, 2, new BigDecimal("999.00")));
    }

    @Test
    void markAsPaid_whenOrderIsCreated_movesToPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.markAsPaid(1L, 1L);

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void markAsPaid_whenOrderIsNotCreated_throwsInvalidOrderStateException() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markAsPaid(1L, 1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("SHIPPED");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_whenOrderIsCreated_restoresStockAndCancels() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(1L, 1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        // Stock must go back up by exactly the quantity that was ordered (2)
        assertThat(keyboard.getStockQuantity()).isEqualTo(40);
        verify(productRepository).save(keyboard);
    }

    @Test
    void cancelOrder_whenOrderIsAlreadyPaid_throwsInvalidOrderStateException() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PAID");

        // Stock must NOT have been touched
        assertThat(keyboard.getStockQuantity()).isEqualTo(38);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shipOrder_whenOrderIsPaid_movesToShipped() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.shipOrder(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shipOrder_whenOrderIsNotPaid_throwsInvalidOrderStateException() {
        // order is still CREATED (default from setUp)
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.shipOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("CREATED");
    }

    @Test
    void deliverOrder_whenOrderIsShipped_movesToDelivered() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.deliverOrder(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void getOrderForUser_whenOrderBelongsToDifferentUser_throwsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // otherUser (id 2) tries to access owner's (id 1) order
        assertThatThrownBy(() -> orderService.getOrderForUser(2L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderForUser_whenOrderBelongsToRequester_returnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderForUser(1L, 1L);

        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    void markAsPaid_whenOrderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.markAsPaid(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}