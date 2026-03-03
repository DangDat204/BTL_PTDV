package com.demo.apigateway.client;

import com.demo.apigateway.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for user-service.
 *
 * === OpenFeign in API Composition ===
 * Feign generates an HTTP client implementation at runtime.
 * We define the interface, Feign handles the actual HTTP call.
 *
 * url is configured in application.yml under feign.client.config.user-service
 */
@FeignClient(name = "user-service", url = "${feign.client.config.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
