package com.enterprise.aigateway.feature.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import com.enterprise.aigateway.constant.LogConstant;

@Slf4j
@Configuration
public class CircuitBreakerConfig {

  private final CircuitBreakerRegistry circuitBreakerRegistry;

  public CircuitBreakerConfig(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
  }

  @PostConstruct
  public void setupCircuitBreakerEventPublisher() {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiCircuitBreaker");

    circuitBreaker.getEventPublisher().onStateTransition(event -> {
      log.warn(LogConstant.LOG_CIRCUIT_BREAKER_STATE_CHANGED,
          event.getStateTransition().getFromState(), event.getStateTransition().getToState());
    }).onCallNotPermitted(event -> {
      log.error(LogConstant.LOG_CIRCUIT_BREAKER_CALL_REJECTED);
    }).onError(event -> {
      log.error(LogConstant.LOG_CIRCUIT_BREAKER_LLM_CALL_FAILED, event.getThrowable().getMessage(),
          event.getElapsedDuration().toMillis());
    });
  }
}
