package com.enterprise.aigateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enterprise.aigateway.constant.LogConstant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lớp Aspect dùng để theo dõi (monitor) và ghi log thời gian thực thi của các phương thức
 * Repository. Giúp phát hiện các truy vấn cơ sở dữ liệu chậm (slow queries) và cung cấp metrics cho
 * hệ thống giám sát (như Prometheus).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepositoryAspect {

  private final MeterRegistry meterRegistry;

  @Value("${application.repository.query-limit-warning-ms:1000}")
  private long executionLimitMs;

  @Around("execution(* com.enterprise.aigateway.feature.*.repository.*.*(..))")
  public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.nanoTime();
    try {
      Object result = joinPoint.proceed();

      // Xử lý riêng cho kiểu Mono (1 kết quả trả về bất đồng bộ trong Reactor)
      if (result instanceof Mono<?> mono) {
        return wrapMono(joinPoint, mono);
      }
      // Xử lý riêng cho kiểu Flux (Nhiều kết quả trả về bất đồng bộ trong Reactor)
      if (result instanceof Flux<?> flux) {
        return wrapFlux(joinPoint, flux);
      }
      // Xử lý riêng cho các Publisher khác của Reactive Streams
      if (result instanceof Publisher<?> publisher) {
        return wrapPublisher(joinPoint, publisher);
      }

      // Xử lý cho các hàm đồng bộ bình thường không dùng Reactive
      recordTimer(joinPoint, System.nanoTime() - start);
      return result;
    } catch (Throwable ex) {
      // Ghi lại thời gian thực thi nếu hàm đồng bộ (hoặc quá trình assembly) bị lỗi
      recordTimer(joinPoint, System.nanoTime() - start);
      throw ex;
    }
  }

  private <T> Mono<T> wrapMono(ProceedingJoinPoint joinPoint, Mono<T> mono) {
    AtomicLong startNanos = new AtomicLong();
    return mono.doOnSubscribe(sub -> startNanos.set(System.nanoTime()))
        .doFinally(signalType -> recordTimer(joinPoint, System.nanoTime() - startNanos.get()));
  }

  private <T> Flux<T> wrapFlux(ProceedingJoinPoint joinPoint, Flux<T> flux) {
    AtomicLong startNanos = new AtomicLong();
    return flux.doOnSubscribe(sub -> startNanos.set(System.nanoTime()))
        .doFinally(signalType -> recordTimer(joinPoint, System.nanoTime() - startNanos.get()));
  }

  private <T> Publisher<T> wrapPublisher(ProceedingJoinPoint joinPoint, Publisher<T> publisher) {
    AtomicLong startNanos = new AtomicLong();
    return Flux.from(publisher).doOnSubscribe(sub -> startNanos.set(System.nanoTime()))
        .doFinally(signalType -> recordTimer(joinPoint, System.nanoTime() - startNanos.get()));
  }

  /**
   * Phương thức lưu trữ dữ liệu thời gian thực thi (metrics) và ghi log cảnh báo
   *
   * @param joinPoint Thông tin hàm đang được gọi
   * @param durationNanos Thời gian thực thi tính bằng Nano giây
   */
  private void recordTimer(ProceedingJoinPoint joinPoint, long durationNanos) {
    // Đẩy metric vào MeterRegistry (ví dụ để Prometheus kéo về hiển thị trên
    // Grafana)
    Timer.builder("db.query.execution.time").description("Time taken to execute database queries")
        .tag("method", joinPoint.getSignature().getName())
        .tag("type", joinPoint.getSignature().getDeclaringType().getSimpleName())
        .register(meterRegistry).record(durationNanos, TimeUnit.NANOSECONDS);

    long executionMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

    if (executionMs >= executionLimitMs) {
      log.warn(LogConstant.LOG_DB_QUERY_EXECUTION_TIME, joinPoint.getSignature(), executionMs);
    }
  }
}
