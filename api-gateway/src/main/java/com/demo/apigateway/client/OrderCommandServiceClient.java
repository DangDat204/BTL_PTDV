package com.demo.apigateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "order-command-service", url = "${feign.client.config.order-command-service.url}")
public interface OrderCommandServiceClient {

    @GetMapping("/benchmark/write-count/{userId}")
    Map<String, Object> benchmarkWriteCount(@PathVariable("userId") Long userId);
}
