package com.siva.ecommerce.controller;

import com.siva.ecommerce.dto.OrderResponse;
import com.siva.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin Orders", description = "Warehouse-side actions (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Ship an order (only while PAID)")
    @PostMapping("/{orderId}/ship")
    public OrderResponse ship(@PathVariable Long orderId) {
        return orderService.shipOrder(orderId);
    }

    @Operation(summary = "Mark an order as delivered (only while SHIPPED)")
    @PostMapping("/{orderId}/deliver")
    public OrderResponse deliver(@PathVariable Long orderId) {
        return orderService.deliverOrder(orderId);
    }
}