package com.enterprise.aigateway.feature.cost.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

  private final ReactiveRedisTemplate<String, Object> redisTemplate;
  private static final int DAILY_QUOTA = 10000;

  public Mono<Boolean> isAllowed(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    return redisTemplate.opsForValue().get(key)
        .defaultIfEmpty(DAILY_QUOTA)
        .flatMap(currentTokens -> {
          int remaining = (int) currentTokens;
          if (remaining >= tokenCost) {
            return redisTemplate.opsForValue()
                .set(key, remaining - tokenCost)
                .thenReturn(true);
          } else {
            return Mono.just(false);
          }
        });
  }
}