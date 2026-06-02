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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    Object result = joinPoint.proceed();

    if (result instanceof Mono<?> mono) {
      return wrapMono(joinPoint, mono);
    }
    if (result instanceof Flux<?> flux) {
      return wrapFlux(joinPoint, flux);
    }
    if (result instanceof Publisher<?> publisher) {
      return wrapPublisher(joinPoint, publisher);
    }

    long start = System.nanoTime();
    try {
      return result;
    } finally {
      recordTimer(joinPoint, System.nanoTime() - start);
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

  private void recordTimer(ProceedingJoinPoint joinPoint, long durationNanos) {
    Timer.builder("db.query.execution.time").description("Time taken to execute database queries")
        .tag("method", joinPoint.getSignature().getName())
        .tag("type", joinPoint.getSignature().getDeclaringType().getSimpleName())
        .register(meterRegistry).record(durationNanos, TimeUnit.NANOSECONDS);

    long executionMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
    if (executionMs >= executionLimitMs) {
      log.warn("{} exec in {} ms : SLOW QUERY", joinPoint.getSignature(), executionMs);
    }
  }
}
