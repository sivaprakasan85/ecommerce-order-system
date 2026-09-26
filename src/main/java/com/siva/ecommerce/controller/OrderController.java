package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.dto.PageResponse;
import com.siva.ecommerce.security.CurrentUser;
import com.siva.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "View the logged-in user's orders and move them through their lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderController(OrderService orderService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List this user's orders (paginated, newest first)")
    @GetMapping
    public PageResponse<OrderResponse> getOrders(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return orderService.getOrdersForUser(currentUser.getId(), page, size);
    }

    @Operation(summary = "Get one order belonging to this user")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrderForUser(currentUser.getId(), orderId);
    }

    @Operation(summary = "Mark an order as PAID (simulated payment confirmation)")
    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(@PathVariable Long orderId) {
        return orderService.markAsPaid(currentUser.getId(), orderId);
    }

    @Operation(summary = "Cancel an order (only while CREATED) — restores stock")
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        return orderService.cancelOrder(currentUser.getId(), orderId);
    }
}