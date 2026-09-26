package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.dto.PageResponse;
import com.siva.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/orders")
@Tag(name = "Orders", description = "View orders and move them through their lifecycle")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "List this user's orders (paginated, newest first)")
    @GetMapping
    public PageResponse<OrderResponse> getOrders(@PathVariable Long userId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return orderService.getOrdersForUser(userId, page, size);
    }

    @Operation(summary = "Get one order belonging to this user")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.getOrderForUser(userId, orderId);
    }

    @Operation(summary = "Mark an order as PAID (simulated payment confirmation)")
    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.markAsPaid(userId, orderId);
    }

    @Operation(summary = "Cancel an order (only while CREATED) — restores stock")
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.cancelOrder(userId, orderId);
    }
}