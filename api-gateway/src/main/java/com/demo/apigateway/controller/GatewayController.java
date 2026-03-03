package com.demo.apigateway.controller;

import com.demo.apigateway.client.OrderCommandServiceClient;
import com.demo.apigateway.client.OrderQueryServiceClient;
import com.demo.apigateway.client.UserServiceClient;
import com.demo.apigateway.dto.OrderSummaryDto;
import com.demo.apigateway.dto.UserDto;
import com.demo.apigateway.dto.UserOrderSummaryResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * API Gateway Controller - demonstrates API Composition pattern.
 *
 * === Distributed Query Problem ===
 * In a microservices / SOA architecture with "database per service":
 * - user_db lives in user-service
 * - order_read_db lives in order-query-service
 *
 * Problem: How do we get a combined view like "user + their order stats"?
 *
 * Traditional monolith: SELECT u.*, COUNT(o.id), SUM(o.total)
 *                       FROM users u LEFT JOIN orders o ON u.id = o.user_id
 *                       WHERE u.id = ?
 * → Single query, single transaction, fast.
 *
 * Microservices: CANNOT do cross-DB JOIN!
 * Solution: API Composition = call each service, aggregate in gateway.
 *
 * === API Composition Drawbacks ===
 * 1. Latency: total time = time(user-service) + time(order-service) [sequential]
 *             or max(times) [parallel] but adds complexity
 * 2. Availability: if either service is down, the whole query fails
 * 3. Data consistency: results from each service are from different moments
 * 4. Complexity: gateway must handle partial failures, timeouts, retries
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final UserServiceClient userServiceClient;
    private final OrderQueryServiceClient orderQueryServiceClient;
    private final OrderCommandServiceClient orderCommandServiceClient;

    /**
     * === Main Demo Endpoint: Distributed Query via API Composition ===
     *
     * GET /users/{id}/order-summary
     *
     * This endpoint demonstrates the DISTRIBUTED QUERY PROBLEM and its solution:
     * Instead of one DB join, we call two services and merge the results.
     *
     * Flow:
     * 1. Gateway calls user-service → GET /users/{id}
     * 2. Gateway calls order-query-service → GET /order-summary/{id}
     * 3. Gateway merges both responses into UserOrderSummaryResponse
     * 4. Logs total latency
     */
    @GetMapping("/users/{id}/order-summary")
    public ResponseEntity<UserOrderSummaryResponse> getUserOrderSummary(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        log.info("[API COMPOSITION] Starting distributed query for userId={}", id);
        log.info("[DISTRIBUTED QUERY PROBLEM] Cannot do DB JOIN across user_db and order_read_db!");
        log.info("[API COMPOSITION] Step 1: Calling user-service...");

        // ---- Network Call 1: user-service ----
        long t1 = System.currentTimeMillis();
        UserDto user;
        try {
            user = userServiceClient.getUserById(id);
        } catch (FeignException.NotFound e) {
            log.warn("[API COMPOSITION] user-service returned 404 for userId={}", id);
            return ResponseEntity.notFound().build();
        }
        long userServiceLatency = System.currentTimeMillis() - t1;
        log.info("[API COMPOSITION] user-service responded in {} ms. User: {}", userServiceLatency, user.getUsername());

        // ---- Network Call 2: order-query-service ----
        log.info("[API COMPOSITION] Step 2: Calling order-query-service...");
        long t2 = System.currentTimeMillis();
        OrderSummaryDto orderSummary = null;
        try {
            orderSummary = orderQueryServiceClient.getOrderSummary(id);
        } catch (FeignException.NotFound e) {
            log.info("[API COMPOSITION] No order summary found for userId={} (no orders yet)", id);
        }
        long orderServiceLatency = System.currentTimeMillis() - t2;
        log.info("[API COMPOSITION] order-query-service responded in {} ms", orderServiceLatency);

        // ---- Merge results ----
        long totalLatency = System.currentTimeMillis() - startTime;
        log.info("[API COMPOSITION] Step 3: Merging data from {} services", 2);
        log.info("[API COMPOSITION] Total composition latency: {} ms (user={}ms + order={}ms + overhead)",
                totalLatency, userServiceLatency, orderServiceLatency);

        System.out.println("============================================");
        System.out.println("[API COMPOSITION] Distributed Query Result");
        System.out.println("[API COMPOSITION] user-service latency: " + userServiceLatency + " ms");
        System.out.println("[API COMPOSITION] order-query-service latency: " + orderServiceLatency + " ms");
        System.out.println("[API COMPOSITION] Total latency: " + totalLatency + " ms");
        System.out.println("[API COMPOSITION] vs Monolith DB JOIN: ~2-5 ms");
        System.out.println("[DRAWBACK] API Composition adds ~" + totalLatency + "ms overhead vs DB join");
        System.out.println("============================================");

        UserOrderSummaryResponse response = UserOrderSummaryResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .totalOrders(orderSummary != null ? orderSummary.getTotalOrders() : 0L)
                .totalAmount(orderSummary != null ? orderSummary.getTotalAmount() : BigDecimal.ZERO)
                .orderSummaryLastUpdated(orderSummary != null ? orderSummary.getLastUpdated() : null)
                .compositionLatencyMs(totalLatency)
                .consistencyNote("Data may be up to a few hundred ms stale due to eventual consistency in CQRS")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * === Benchmark: Compare Write DB Count vs Read Model ===
     *
     * GET /benchmark/compare/{userId}
     *
     * This endpoint calls BOTH methods and shows the performance difference.
     * Run this after seeding many orders to see a dramatic difference.
     */
    @GetMapping("/benchmark/compare/{userId}")
    public ResponseEntity<Map<String, Object>> benchmarkCompare(@PathVariable Long userId) {
        log.info("[BENCHMARK] Running comparison benchmark for userId={}", userId);

        // Benchmark 1: Write DB Count
        long t1 = System.currentTimeMillis();
        Map<String, Object> writeCountResult;
        try {
            writeCountResult = orderCommandServiceClient.benchmarkWriteCount(userId);
        } catch (Exception e) {
            writeCountResult = Map.of("error", "order-command-service unavailable: " + e.getMessage());
        }
        long writeCountLatency = System.currentTimeMillis() - t1;

        // Benchmark 2: Read Model
        long t2 = System.currentTimeMillis();
        Map<String, Object> readModelResult;
        try {
            readModelResult = orderQueryServiceClient.benchmarkReadModel(userId);
        } catch (Exception e) {
            readModelResult = Map.of("error", "order-query-service unavailable: " + e.getMessage());
        }
        long readModelLatency = System.currentTimeMillis() - t2;

        System.out.println("============================================");
        System.out.println("[BENCHMARK COMPARISON]");
        System.out.println("[BENCHMARK] Write DB COUNT(*): " + writeCountLatency + " ms (gateway round-trip)");
        System.out.println("[BENCHMARK] Read Model lookup: " + readModelLatency + " ms (gateway round-trip)");
        long diff = writeCountLatency - readModelLatency;
        System.out.println("[BENCHMARK] Performance diff: " + diff + " ms");
        if (diff > 0) {
            System.out.println("[BENCHMARK] Read Model is " + diff + " ms FASTER!");
        }
        System.out.println("============================================");

        Map<String, Object> result = new HashMap<>();
        result.put("writeDbCount", writeCountResult);
        result.put("readModelLookup", readModelResult);
        result.put("comparison", Map.of(
                "writeCountGatewayLatencyMs", writeCountLatency,
                "readModelGatewayLatencyMs", readModelLatency,
                "differenceMs", diff,
                "winner", diff > 0 ? "READ_MODEL" : "WRITE_COUNT",
                "explanation", "Read model uses pre-computed aggregates (O(1)), " +
                        "Write DB count uses full table scan (O(n)). " +
                        "Difference grows with number of orders."
        ));
        return ResponseEntity.ok(result);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-gateway",
                "description", "Demonstrates API Composition pattern for Distributed Queries in CQRS"
        ));
    }
}
