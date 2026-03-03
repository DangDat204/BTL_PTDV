package com.demo.orderquery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderSummaryView - Pre-computed Read Model (CQRS Read Side)
 *
 * === Read Model Optimization ===
 * Instead of running COUNT(*) and SUM() every time a query comes in,
 * we maintain a pre-computed summary per user.
 *
 * When an order is created (event consumed):
 *   total_orders += 1
 *   total_amount += order.totalAmount
 *
 * Query time: O(1) - just lookup by user_id with index
 * vs Write DB COUNT: O(n) - full table scan
 *
 * === Denormalized Structure ===
 * This is intentionally denormalized (violates 3NF) for performance.
 * In CQRS, read models are allowed to be denormalized because
 * they are never directly modified by commands - only by events.
 */
@Entity
@Table(name = "order_summary_view", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    /**
     * Pre-aggregated: total number of orders for this user.
     * Updated via event, not computed at query time.
     */
    @Column(name = "total_orders")
    private Long totalOrders;

    /**
     * Pre-aggregated: total monetary value of all orders.
     * Updated via event, not computed at query time.
     */
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
}
