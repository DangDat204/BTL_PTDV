package com.demo.ordercommand.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderCreatedEvent - Domain Event published to RabbitMQ after an order is saved.
 *
 * === Event-Driven Architecture (EDA) in CQRS ===
 * When a command (CreateOrder) is processed:
 *  1. order-command-service saves to order_write_db
 *  2. Publishes this event to RabbitMQ
 *  3. order-query-service consumes the event asynchronously
 *  4. Updates the read model (order_summary_view) in order_read_db
 *
 * === Eventual Consistency ===
 * There is a brief window where order_write_db has the new order
 * but order_read_db has NOT yet been updated.
 * This is called "eventual consistency" - the read model will
 * eventually catch up, but is not immediately consistent.
 * This is an acceptable trade-off in many systems for better scalability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
