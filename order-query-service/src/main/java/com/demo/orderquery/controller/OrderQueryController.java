package com.demo.orderquery.controller;

import com.demo.orderquery.entity.OrderSummaryView;
import com.demo.orderquery.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OrderQueryController - handles all READ (Query) endpoints.
 *
 * === CQRS API Design ===
 * Only GET endpoints here - no mutations allowed.
 * All mutations are handled by order-command-service.
 */
@RestController
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    /**
     * Get pre-computed order summary for a user.
     * Called by api-gateway as part of the API Composition pattern.
     */
    @GetMapping("/order-summary/{userId}")
    public ResponseEntity<OrderSummaryView> getOrderSummary(@PathVariable Long userId) {
        return orderQueryService.getOrderSummary(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * === Benchmark Endpoint ===
     * GET /benchmark/read-model/{userId}
     * Demonstrates O(1) lookup from pre-computed read model.
     * Compare with: GET /benchmark/write-count/{userId} in order-command-service
     */
    @GetMapping("/benchmark/read-model/{userId}")
    public ResponseEntity<Map<String, Object>> benchmarkReadModel(@PathVariable Long userId) {
        return ResponseEntity.ok(orderQueryService.benchmarkReadModel(userId));
    }
}
