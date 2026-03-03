package com.demo.apigateway.client;

import com.demo.apigateway.dto.OrderSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign Client for order-query-service.
 *
 * === Distributed Query via API Composition ===
 * Since we CANNOT do a DB join across user_db and order_read_db,
 * we use HTTP calls to compose data from two services.
 *
 * Drawback: Two network round trips instead of one DB join.
 * Advantage: Services remain decoupled and independently scalable.
 */
@FeignClient(name = "order-query-service", url = "${feign.client.config.order-query-service.url}")
public interface OrderQueryServiceClient {

    @GetMapping("/order-summary/{userId}")
    OrderSummaryDto getOrderSummary(@PathVariable("userId") Long userId);

    @GetMapping("/benchmark/read-model/{userId}")
    Map<String, Object> benchmarkReadModel(@PathVariable("userId") Long userId);
}
