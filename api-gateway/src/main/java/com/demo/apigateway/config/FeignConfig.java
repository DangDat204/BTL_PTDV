package com.demo.apigateway.config;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign Client Configuration
 *
 * Configures logging and timeout for all Feign clients.
 * Feign logs help us see each HTTP call made during API Composition.
 */
@Configuration
public class FeignConfig {

    /**
     * FULL logging shows: headers, body, request/response - useful for debugging API Composition
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
    }
}
