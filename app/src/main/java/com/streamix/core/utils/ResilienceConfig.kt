package com.streamix.core.utils

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import java.time.Duration

object ResilienceConfig {

    fun createCircuitBreaker(name: String): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(10)
            .build()
        return CircuitBreaker.of(name, config)
    }

    fun createRetry(name: String): Retry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1000))
            .retryOnException { it is Exception }
            .build()
        return Retry.of(name, config)
    }
}
