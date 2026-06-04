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

  /**
   * Phương thức dùng để đăng ký lắng nghe các sự kiện của mạch và ghi log.
   */
  @PostConstruct
  public void setupCircuitBreakerEventPublisher() {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiCircuitBreaker");

    circuitBreaker.getEventPublisher()
        // Bắt sự kiện: Trạng thái mạch bị thay đổi (Ví dụ: từ CLOSED (Bình thường) ->
        // OPEN (Ngắt mạch))
        .onStateTransition(event -> {
          log.warn(LogConstant.CircuitBreakerLog.LOG_CIRCUIT_BREAKER_STATE_CHANGED,
              event.getStateTransition().getFromState(), event.getStateTransition().getToState());
        })
        // Bắt sự kiện: Request bị chặn lại từ sớm do mạch đang MỞ (OPEN), không cho gọi
        // sang LLM nữa
        .onCallNotPermitted(event -> {
          log.error(LogConstant.CircuitBreakerLog.LOG_CIRCUIT_BREAKER_CALL_REJECTED);
        })
        // Bắt sự kiện: Cuộc gọi đến LLM Provider thực tế bị lỗi (Timeout, sập
        // server...)
        .onError(event -> {
          log.error(LogConstant.CircuitBreakerLog.LOG_CIRCUIT_BREAKER_LLM_CALL_FAILED,
              event.getThrowable().getMessage(), event.getElapsedDuration().toMillis());
        });
  }
}
