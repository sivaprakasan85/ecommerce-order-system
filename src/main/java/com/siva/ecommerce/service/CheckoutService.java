package com.siva.ecommerce.service;

import com.siva.ecommerce.dto.OrderItemResponse;
import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.entity.*;
import com.siva.ecommerce.exception.EmptyCartException;
import com.siva.ecommerce.exception.InsufficientStockException;
import com.siva.ecommerce.exception.ResourceNotFoundException;
import com.siva.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public CheckoutService(CartRepository cartRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           PaymentRepository paymentRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * THE CENTERPIECE METHOD.
     * Single transaction: validate stock for every item -> deduct stock ->
     * create Order + OrderItems -> create Payment.
     * If ANY step fails (e.g. stock ran out on item 3 of 5), Spring rolls back
     * the ENTIRE transaction: no stock is deducted, no order is created, no
     * payment is created. The cart itself is untouched until everything succeeds.
     */
    @Transactional
    public OrderResponse checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout an empty cart");
        }

        // Step 1: validate stock for EVERY item first, before changing anything.
        // This means a failure on item 3 never leaves items 1-2 partially deducted.
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + product.getName() + "'. Available: "
                                + product.getStockQuantity() + ", requested: " + cartItem.getQuantity());
            }
        }

        // Step 2: deduct stock and build order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = new Order(cart.getUser(), BigDecimal.ZERO);

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal priceAtPurchase = product.getPrice();
            OrderItem orderItem = new OrderItem(product, cartItem.getQuantity(), priceAtPurchase);
            order.addItem(orderItem);

            totalAmount = totalAmount.add(priceAtPurchase.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Step 3: create the (simulated) payment record
        Payment payment = new Payment(savedOrder, totalAmount);
        paymentRepository.save(payment);

        // Step 4: clear the cart now that checkout succeeded
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(savedOrder);
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