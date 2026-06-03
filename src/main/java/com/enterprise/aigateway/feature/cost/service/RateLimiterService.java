package com.enterprise.aigateway.feature.cost.service;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

  private final ReactiveStringRedisTemplate stringRedisTemplate;

  @Value("${application.aigateway.cost.rate-limit:10000}")
  private int dailyQuota;

  public Mono<Boolean> isAllowed(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    return stringRedisTemplate.opsForValue().get(key).map(Integer::parseInt)
        .defaultIfEmpty(dailyQuota).map(remaining -> remaining >= tokenCost);
  }

  public Mono<Boolean> deductTokens(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    // 1. setIfAbsent: Khởi tạo giá trị mặc định nếu user chưa gọi lần nào trong
    // ngày
    // (Atomic: Nếu 2 luồng gọi cùng lúc, chỉ 1 luồng set thành công)
    // 2. decrement: Trừ nguyên tử (Atomic decrement) số tokenCost vào Redis
    return stringRedisTemplate.opsForValue()
        .setIfAbsent(key, String.valueOf(dailyQuota), Duration.ofDays(1))
        .then(stringRedisTemplate.opsForValue().decrement(key, tokenCost)).thenReturn(true);
  }
}
