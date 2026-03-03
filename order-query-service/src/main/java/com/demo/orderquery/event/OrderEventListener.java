package com.demo.orderquery.event;

import com.demo.orderquery.entity.OrderSummaryView;
import com.demo.orderquery.repository.OrderSummaryViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * OrderEventListener - Core of the CQRS Read Side.
 *
 * === Event-Driven CQRS Flow ===
 * 1. order-command-service: saveOrder() → publish OrderCreatedEvent
 * 2. RabbitMQ delivers event to this listener (asynchronously)
 * 3. This listener updates the pre-aggregated read model in order_read_db
 *
 * === Eventual Consistency ===
 * There is a time gap between:
 * - Order saved in write DB (order_write_db)
 * - Read model updated here (order_read_db)
 *
 * During this window, if someone queries the read model,
 * they may see stale data (missing the latest order).
 * This is the trade-off of eventual consistency vs strong consistency.
 *
 * In practice, this window is typically < 100ms with RabbitMQ.
 * For many business cases (dashboards, reports), this is acceptable.
 *
 * === Why this is better than polling ===
 * - No polling overhead (no "SELECT * every 1 second")
 * - Near real-time updates driven by actual events
 * - Scales horizontally (multiple listeners for high throughput)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderSummaryViewRepository orderSummaryViewRepository;

    @RabbitListener(queues = "order.created.queue")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[EVENT RECEIVED] OrderCreatedEvent: orderId={}, userId={}, amount={}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        long startTime = System.currentTimeMillis();

        // Find existing summary or create new one
        OrderSummaryView summary = orderSummaryViewRepository
                .findByUserId(event.getUserId())
                .orElse(OrderSummaryView.builder()
                        .userId(event.getUserId())
                        .totalOrders(0L)
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        // Increment the pre-aggregated counters
        // This is the key optimization: UPDATE instead of COUNT(*)
        summary.setTotalOrders(summary.getTotalOrders() + 1);
        summary.setTotalAmount(summary.getTotalAmount().add(event.getTotalAmount()));

        orderSummaryViewRepository.save(summary);

        long endTime = System.currentTimeMillis();
        log.info("[READ MODEL UPDATED] userId={} | totalOrders={} | totalAmount={} | Update time: {} ms",
                event.getUserId(), summary.getTotalOrders(), summary.getTotalAmount(), (endTime - startTime));

        System.out.println("============================================");
        System.out.println("[EVENTUAL CONSISTENCY] Read model updated after event consumption");
        System.out.println("[EVENTUAL CONSISTENCY] userId=" + event.getUserId());
        System.out.println("[EVENTUAL CONSISTENCY] New totalOrders=" + summary.getTotalOrders());
        System.out.println("[EVENTUAL CONSISTENCY] New totalAmount=" + summary.getTotalAmount());
        System.out.println("============================================");
    }
}
