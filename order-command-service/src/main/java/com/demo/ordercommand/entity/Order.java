package com.demo.ordercommand.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order entity stored in order_write_db (CQRS Write Side).
 *
 * === CQRS Write Model ===
 * This is the "Command Model" - optimized for WRITE operations.
 * Schema is normalized to ensure data integrity.
 * Every create/update goes through this model first.
 *
 * === Why separate from Read Model? ===
 * Write model: normalized, ACID compliant, optimized for consistency
 * Read model: denormalized, optimized for fast queries, eventual consistency
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
