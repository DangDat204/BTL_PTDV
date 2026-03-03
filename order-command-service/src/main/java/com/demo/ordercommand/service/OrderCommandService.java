package com.demo.ordercommand.service;

import com.demo.ordercommand.config.RabbitMQConfig;
import com.demo.ordercommand.entity.Order;
import com.demo.ordercommand.event.OrderCreatedEvent;
import com.demo.ordercommand.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * OrderCommandService - handles all WRITE operations (Commands).
 *
 * === CQRS Concept ===
 * CQRS = Command Query Responsibility Segregation
 * - Commands = operations that CHANGE state (Create, Update, Delete)
 * - Queries = operations that READ state (Get, List, Search)
 *
 * By separating them:
 * - Each side can be scaled independently
 * - Write side optimized for consistency and integrity
 * - Read side optimized for speed and availability
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Create an order and publish an event.
     *
     * Transaction boundary: We save to write DB within @Transactional.
     * If save fails, the event is NOT published (no inconsistency).
     * If event publish fails after save, the read model may lag
     * but will be fixed via retry/dead-letter-queue mechanisms.
     */
    @Transactional
    public Order createOrder(Order order) {
        // Step 1: Save to write DB (order_write_db)
        Order savedOrder = orderRepository.save(order);
        log.info("[COMMAND] Order saved to write DB. orderId={}, userId={}", savedOrder.getId(), savedOrder.getUserId());

        // Step 2: Build and publish OrderCreatedEvent to RabbitMQ
        // This is the bridge between Write side and Read side in CQRS
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .productId(savedOrder.getProductId())
                .quantity(savedOrder.getQuantity())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus())
                .createdAt(savedOrder.getCreatedAt())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
        log.info("[EVENT] OrderCreatedEvent published to RabbitMQ. orderId={}", savedOrder.getId());
        log.info("[EVENTUAL CONSISTENCY] Read model will be updated asynchronously. " +
                "There may be a brief window where query results are stale.");

        return savedOrder;
    }

    /**
     * === Benchmark: Direct Write DB Count ===
     * Demonstrates performance of COUNT(*) on the write table.
     * This gets slower as orders accumulate.
     *
     * Benchmark Result Example:
     * 1,000 orders: ~5ms
     * 100,000 orders: ~50ms
     * 10,000,000 orders: ~500ms+ (full table scan)
     */
    public Map<String, Object> benchmarkWriteCount(Long userId) {
        long startTime = System.currentTimeMillis();

        Long count = orderRepository.countByUserId(userId);
        BigDecimal totalAmount = orderRepository.sumAmountByUserId(userId);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        log.info("[BENCHMARK - WRITE COUNT] UserId={} | Count={} | TotalAmount={} | Execution time: {} ms",
                userId, count, totalAmount, executionTime);
        System.out.println("============================================");
        System.out.println("[BENCHMARK] Write DB Count Query");
        System.out.println("[BENCHMARK] UserId: " + userId);
        System.out.println("[BENCHMARK] Execution time: " + executionTime + " ms");
        System.out.println("[BENCHMARK] Result: count=" + count + ", total=" + totalAmount);
        System.out.println("[BENCHMARK] Method: COUNT(*) + SUM() on normalized orders table");
        System.out.println("============================================");

        Map<String, Object> result = new HashMap<>();
        result.put("method", "WRITE_DB_COUNT");
        result.put("userId", userId);
        result.put("totalOrders", count);
        result.put("totalAmount", totalAmount);
        result.put("executionTimeMs", executionTime);
        result.put("note", "Direct COUNT(*) on write DB - slower for large datasets");
        return result;
    }

    public void seedOrders(Long userId, int count) {
        for (int i = 0; i < count; i++) {
            Order order = Order.builder()
                    .userId(userId)
                    .productId((long) (i % 3 + 1))
                    .quantity(i % 5 + 1)
                    .totalAmount(new BigDecimal((i + 1) * 100000))
                    .status("COMPLETED")
                    .build();
            createOrder(order);
        }
        log.info("[SEED] Created {} orders for userId={}", count, userId);
    }
}
