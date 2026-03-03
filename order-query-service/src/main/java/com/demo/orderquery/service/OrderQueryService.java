package com.demo.orderquery.service;

import com.demo.orderquery.entity.OrderSummaryView;
import com.demo.orderquery.repository.OrderSummaryViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OrderQueryService - handles all READ operations (Queries).
 *
 * === CQRS Read Side ===
 * This service ONLY reads from order_read_db.
 * It never writes directly - all updates come via events from RabbitMQ.
 *
 * Benefits:
 * 1. Read DB can be optimized independently (different indexes, caching)
 * 2. Read DB can be replicated for high availability
 * 3. No lock contention with write operations
 * 4. Pre-aggregated data = extremely fast queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderQueryService {

    private final OrderSummaryViewRepository orderSummaryViewRepository;

    /**
     * Get order summary for a user from the pre-computed read model.
     * This is used by the API Gateway for the /users/{id}/order-summary endpoint.
     */
    public Optional<OrderSummaryView> getOrderSummary(Long userId) {
        log.info("[QUERY] Fetching order summary for userId={} from read model", userId);
        return orderSummaryViewRepository.findByUserId(userId);
    }

    /**
     * === Benchmark Endpoint ===
     * Demonstrates performance of pre-computed read model vs direct COUNT(*).
     *
     * Expected Results (with many orders):
     * - Read Model: ~1-2ms (single row lookup by index)
     * - Write DB Count: ~50-500ms (full table scan + aggregation)
     *
     * This is the core performance argument for CQRS read models.
     */
    public Map<String, Object> benchmarkReadModel(Long userId) {
        long startTime = System.currentTimeMillis();

        Optional<OrderSummaryView> summary = orderSummaryViewRepository.findByUserId(userId);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        Long totalOrders = summary.map(OrderSummaryView::getTotalOrders).orElse(0L);
        BigDecimal totalAmount = summary.map(OrderSummaryView::getTotalAmount).orElse(BigDecimal.ZERO);

        log.info("[BENCHMARK - READ MODEL] UserId={} | TotalOrders={} | TotalAmount={} | Execution time: {} ms",
                userId, totalOrders, totalAmount, executionTime);

        System.out.println("============================================");
        System.out.println("[BENCHMARK] Read Model (Pre-computed) Query");
        System.out.println("[BENCHMARK] UserId: " + userId);
        System.out.println("[BENCHMARK] Execution time: " + executionTime + " ms");
        System.out.println("[BENCHMARK] Result: count=" + totalOrders + ", total=" + totalAmount);
        System.out.println("[BENCHMARK] Method: Single row lookup from order_summary_view");
        System.out.println("[BENCHMARK] Compare with Write DB COUNT to see performance diff!");
        System.out.println("============================================");

        Map<String, Object> result = new HashMap<>();
        result.put("method", "READ_MODEL_LOOKUP");
        result.put("userId", userId);
        result.put("totalOrders", totalOrders);
        result.put("totalAmount", totalAmount);
        result.put("executionTimeMs", executionTime);
        result.put("note", "Pre-computed read model - O(1) lookup, always fast regardless of order count");
        return result;
    }
}
