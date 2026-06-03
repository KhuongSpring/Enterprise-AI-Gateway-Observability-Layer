package com.enterprise.aigateway.feature.cost.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

  private final ReactiveRedisTemplate<String, Object> redisTemplate;
  @Value("${application.aigateway.cost.rate-limit:10000}")
  private int dailyQuota;

  public Mono<Boolean> isAllowed(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    return redisTemplate.opsForValue().get(key).defaultIfEmpty(dailyQuota).map(currentTokens -> {
      int remaining = currentTokens instanceof Number ? ((Number) currentTokens).intValue()
          : Integer.parseInt(currentTokens.toString());
      return remaining >= tokenCost;
    });
  }

  public Mono<Boolean> deductTokens(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    return redisTemplate.opsForValue().get(key).defaultIfEmpty(dailyQuota)
        .flatMap(currentTokens -> {
          int remaining = currentTokens instanceof Number ? ((Number) currentTokens).intValue()
              : Integer.parseInt(currentTokens.toString());
          return redisTemplate.opsForValue().set(key, remaining - tokenCost).thenReturn(true);
        });
  }
}
