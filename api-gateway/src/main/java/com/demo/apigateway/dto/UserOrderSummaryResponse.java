package com.demo.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UserOrderSummaryResponse - the final composed response from API Gateway.
 *
 * === API Composition Pattern ===
 * This DTO combines data from TWO separate services:
 * - UserDto (from user-service on port 8081)
 * - OrderSummaryDto (from order-query-service on port 8084)
 *
 * The gateway acts as the "aggregator" that:
 * 1. Calls user-service to get user details
 * 2. Calls order-query-service to get order summary
 * 3. Merges them into this single response object
 *
 * === Drawback of API Composition ===
 * - Latency = max(userService, orderQueryService) or sum if sequential
 * - Both services must be available (availability = A * B, which is lower)
 * - Network overhead for multiple HTTP calls
 * - Gateway becomes complex as more services are added
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderSummaryResponse {

    // From user-service
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;

    // From order-query-service
    private Long totalOrders;
    private BigDecimal totalAmount;
    private LocalDateTime orderSummaryLastUpdated;

    // Metadata for observability
    private Long compositionLatencyMs;
    private String consistencyNote;
}
