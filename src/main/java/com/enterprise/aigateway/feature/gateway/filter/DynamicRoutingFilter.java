package com.enterprise.aigateway.feature.gateway.filter;

import java.net.URI;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.enterprise.aigateway.constant.CommonConstant;

import reactor.core.publisher.Mono;

/**
 * Filter Toàn cục chịu trách nhiệm định tuyến động. Chuyển hướng request từ URL ảo của Gateway sang
 * URL thực tế của LLM Provider
 */
@Component
public class DynamicRoutingFilter implements GlobalFilter, Ordered {

  /**
   * Định nghĩa thứ tự ưu tiên của Filter. Mức 10001 được đặt cố tình để chạy ngay sau
   * RouteToRequestUrlFilter (mức 10000 mặc định của Spring). Điều này đảm bảo ta có thể ghi đè
   * (override) được URL đích cuối cùng.
   */
  @Override
  public int getOrder() {
    return 10001;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Lấy URI đích đã được xác định trước đó (từ
    // PromptTransformGatewayFilterFactory)
    URI dynamicUri = exchange.getAttribute(CommonConstant.TARGET_AI_URI);

    if (dynamicUri != null) {
      // Ép Spring Cloud Gateway chuyển hướng request đến URL mới này thay vì cấu hình
      // mặc định ban đầu
      exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, dynamicUri);
    }

    // Đẩy luồng xử lý đi tiếp tới Filter tiếp theo
    return chain.filter(exchange);
  }
}
