package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.OrderItemResponse;
import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.dto.PageResponse;
import com.siva.ecommerce.entity.Order;
import com.siva.ecommerce.entity.OrderItem;
import com.siva.ecommerce.entity.OrderStatus;
import com.siva.ecommerce.entity.Product;
import com.siva.ecommerce.exception.InvalidOrderStateException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.OrderRepository;
import com.siva.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return PageResponse.from(orders.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long userId, Long orderId) {
        Order order = findOrThrow(orderId);
        verifyOwnership(order, userId);
        return toResponse(order);
    }

    /**
     * CREATED -> PAID. In a real system this would be triggered by a payment
     * gateway webhook; here it's a manual endpoint since payment is simulated.
     */
    @Transactional
    public OrderResponse markAsPaid(Long userId, Long orderId) {
        Order order = findOrThrow(orderId);
        verifyOwnership(order, userId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                    "Only a CREATED order can be marked as PAID. Current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PAID);
        return toResponse(orderRepository.save(order));
    }

    /**
     * CREATED -> CANCELLED. Restores stock for every item, since the order
     * never got as far as being paid or shipped.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = findOrThrow(orderId);
        verifyOwnership(order, userId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                    "Only a CREATED order can be cancelled. Current status: " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    /**
     * PAID -> SHIPPED. Admin-side action (no ownership check — in a later
     * session this will be restricted to ADMIN role via Spring Security).
     */
    @Transactional
    public OrderResponse shipOrder(Long orderId) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException(
                    "Only a PAID order can be shipped. Current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.SHIPPED);
        return toResponse(orderRepository.save(order));
    }

    /**
     * SHIPPED -> DELIVERED. Admin-side action, same note as shipOrder().
     */
    @Transactional
    public OrderResponse deliverOrder(Long orderId) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException(
                    "Only a SHIPPED order can be marked as DELIVERED. Current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.DELIVERED);
        return toResponse(orderRepository.save(order));
    }

    private Order findOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void verifyOwnership(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + order.getId());
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                subtotal
        );
    }
}