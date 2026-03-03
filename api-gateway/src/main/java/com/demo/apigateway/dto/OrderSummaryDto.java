package com.demo.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderSummaryDto - data returned from order-query-service via Feign
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
    private Long id;
    private Long userId;
    private Long totalOrders;
    private BigDecimal totalAmount;
    private LocalDateTime lastUpdated;
}
