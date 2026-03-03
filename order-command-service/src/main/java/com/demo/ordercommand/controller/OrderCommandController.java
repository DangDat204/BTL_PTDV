package com.demo.ordercommand.controller;

import com.demo.ordercommand.entity.Order;
import com.demo.ordercommand.service.OrderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OrderCommandController - handles all Write (Command) endpoints.
 *
 * === CQRS API Design ===
 * Command endpoints are separate from Query endpoints.
 * This controller only handles mutations (POST, PUT, DELETE).
 * Queries are handled by order-query-service.
 */
@RestController
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandService orderCommandService;

    /**
     * Create a new order.
     * Flow: Save to write DB → Publish OrderCreatedEvent → Read model updated
     * asynchronously
     */
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderCommandService.createOrder(order));
    }

    /**
     * === Benchmark Endpoint ===
     * GET /benchmark/write-count/{userId}
     * Demonstrates performance of direct COUNT(*) on write DB.
     * Compare with: GET /benchmark/read-model/{userId} in order-query-service
     */
    @GetMapping("/benchmark/write-count/{userId}")
    public ResponseEntity<Map<String, Object>> benchmarkWriteCount(@PathVariable Long userId) {
        return ResponseEntity.ok(orderCommandService.benchmarkWriteCount(userId));
    }

    /**
     * Seed demo orders for benchmark testing
     */
    @PostMapping("/orders/seed/{userId}/{count}")
    public ResponseEntity<String> seedOrders(
            @PathVariable("userId") Long userId,
            @PathVariable("count") int count) {

        orderCommandService.seedOrders(userId, count);
        return ResponseEntity.ok("Seeded " + count + " orders for user " + userId);
    }
}
