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

  /**
   * Kiểm tra xem user có còn đủ token để thực hiện request hay không.
   *
   * @param userId ID của người dùng
   * @param tokenCost Số token ước tính sẽ bị tiêu hao
   * @return true nếu còn đủ ngân sách, false nếu vượt quá
   */
  public Mono<Boolean> isAllowed(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    return stringRedisTemplate.opsForValue().get(key).map(Integer::parseInt)
        // Nếu user chưa có data trên Redis (ví dụ: ngày mới), mặc định là còn đủ
        // dailyQuota
        .defaultIfEmpty(dailyQuota)
        // So sánh: Nếu số token còn lại >= số token cần dùng thì cho phép (true)
        .map(remaining -> remaining >= tokenCost);
  }

  /**
   * Thực hiện trừ số lượng token của user sau khi đã có phản hồi từ AI Provider.
   *
   * @param userId ID của người dùng
   * @param tokenCost Số lượng token thực tế đã sử dụng
   * @return Mono<Boolean> trả về true khi thao tác trừ hoàn tất
   */
  public Mono<Boolean> deductTokens(String userId, int tokenCost) {
    String key = "rate_limit:user:" + userId;

    // Bước 1 (setIfAbsent): Khởi tạo ngân sách nếu user chưa gọi lần nào trong ngày
    // hôm nay.
    // - Tính chất Atomic: Nếu có nhiều request cùng lúc, Redis đảm bảo chỉ 1 thao
    // tác set thành công.
    // - TTL (Duration.ofDays(1)): Dữ liệu sẽ tự động reset (xóa) sau 1 ngày.
    return stringRedisTemplate.opsForValue()
        .setIfAbsent(key, String.valueOf(dailyQuota), Duration.ofDays(1))

        // Bước 2 (decrement): Trừ thẳng số token đã dùng.
        // - Tính chất Atomic: Trừ trực tiếp trên Redis mà không cần lấy về cộng trừ thủ
        // công (giúp tránh lỗi Race Condition).
        .then(stringRedisTemplate.opsForValue().decrement(key, tokenCost))

        .thenReturn(true);
  }
}
