package com.enterprise.aigateway.feature.gateway.filter;

import com.enterprise.aigateway.feature.cost.service.RateLimiterService;
import com.enterprise.aigateway.feature.cost.service.TokenCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiGatewayFilter implements GlobalFilter, Ordered {

  private final TokenCounterService tokenCounterService;
  private final RateLimiterService rateLimiterService;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 1. Giả lập lấy UserID từ Header (sau này sẽ lấy từ Spring Security)
    String rawUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
    String userId = (rawUserId != null) ? rawUserId : "anonymous";

    // 2. Lấy nội dung Prompt (Lưu ý: Trong thực tế bạn cần đọc Body của request
    // Reactive)
    // Để đơn giản hóa trong ví dụ này, ta đếm một chuỗi mẫu.
    String dummyPrompt = "Xin chào, hãy tạo cho tôi một kế hoạch marketing.";

    // 3. Đếm token sử dụng jtokkit
    int tokenCost = tokenCounterService.countTokens(dummyPrompt);
    log.info("Request từ user {} tiêu tốn khoảng {} tokens", userId, tokenCost);

    // 4. Kiểm tra hạn mức từ Redis
    return rateLimiterService.isAllowed(userId, tokenCost)
        .flatMap(allowed -> {
          if (allowed) {
            // 4a. Nếu hợp lệ, cho phép request đi tiếp tới LLM (OpenAI/Gemini)
            return chain.filter(exchange);
          } else {
            // 4b. Nếu hết ngân sách, chặn lại và trả về lỗi 429 (Too Many Requests)
            log.warn("User {} đã vượt quá ngân sách token!", userId);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
          }
        });
  }

  @Override
  public int getOrder() {
    return -1; // Đảm bảo Filter này chạy đầu tiên
  }
}