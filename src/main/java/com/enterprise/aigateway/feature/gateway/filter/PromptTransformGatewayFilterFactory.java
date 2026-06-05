package com.enterprise.aigateway.feature.gateway.filter;

import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.enterprise.aigateway.constant.CommonConstant;
import com.enterprise.aigateway.constant.ErrorMessage;
import com.enterprise.aigateway.constant.LogConstant;
import com.enterprise.aigateway.feature.gateway.dto.request.AiGatewayRequest;
import com.enterprise.aigateway.feature.gateway.service.AiPayloadTransformService;
import com.enterprise.aigateway.feature.gateway.service.AiRoutingService;
import com.enterprise.aigateway.feature.gateway.service.AiRoutingService.AiRouteConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Filter chặn Request đầu vào từ người dùng. Chức năng chính: Đọc nội dung Request ban đầu, xác
 * định Provider, cấu hình Routing và biến đổi Body sang định dạng phù hợp với LLM Provider đích.
 */
@Slf4j
@Component
public class PromptTransformGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

  private final AiRoutingService aiRoutingService;
  private final AiPayloadTransformService aiPayloadTransformService;
  private final ObjectMapper objectMapper;

  public PromptTransformGatewayFilterFactory(AiRoutingService aiRoutingService,
      AiPayloadTransformService aiPayloadTransformService, ObjectMapper objectMapper) {
    super(Object.class);
    this.aiRoutingService = aiRoutingService;
    this.aiPayloadTransformService = aiPayloadTransformService;
    this.objectMapper = objectMapper;
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      // Bắt buộc phải cache lại Request Body vì trong luồng WebFlux (Reactive), Body
      // stream chỉ được đọc 1 lần duy nhất
      return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
        DataBuffer cachedBody =
            exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
        if (cachedBody == null) {
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
              ErrorMessage.GatewayError.ERR_TRANSFORM_REQUEST_BODY_IS_EMPTY));
        }

        try {
          // BƯỚC 1. Đọc chuỗi JSON từ Body của Request do người dùng gửi lên
          byte[] bytes = new byte[cachedBody.readableByteCount()];
          cachedBody.read(bytes);
          // Đưa con trỏ (position) về 0 để hệ thống có thể đọc lại mảng byte nếu cần
          cachedBody.readPosition(0);

          // Convert JSON string thành AiGatewayRequest
          AiGatewayRequest inRequest = objectMapper.readValue(bytes, AiGatewayRequest.class);

          // Validate thông tin model và prompt
          if (inRequest == null || inRequest.getModel() == null || inRequest.getModel().isBlank()
              || inRequest.getPrompt() == null || inRequest.getPrompt().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                ErrorMessage.GatewayError.ERR_TRANSFORM_REQUEST_BODY_IS_INVALID));
          }

          // BƯỚC 2. Chuyển cho AiRoutingService xử lý xác định URL và Header đích
          AiRouteConfig routeConfig = aiRoutingService.determineRoute(inRequest.getModel());

          // Ghi lại URL đích vào thuộc tính trung gian để DynamicRoutingFilter (chạy sau
          // cùng) đè lại URL thực tế
          exchange.getAttributes().put(CommonConstant.TARGET_AI_URI, routeConfig.getUri());

          // Ghi lại loại Provider để AiResponseTransformGatewayFilterFactory nhận diện
          // được
          String aiProvider = inRequest.getModel().toLowerCase().startsWith(CommonConstant.GEMINI)
              ? CommonConstant.GEMINI
              : CommonConstant.OPENAI;
          exchange.getAttributes().put(CommonConstant.AI_PROVIDER, aiProvider);

          // Trích xuất User ID từ Header
          String rawUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
          String userId = (rawUserId != null) ? rawUserId : "anonymous";
          exchange.getAttributes().put("X-User-Id", userId);

          // BƯỚC 3. Gọi AiPayloadTransformService để biến đổi JSON và tính toán token
          return aiPayloadTransformService.transformPayload(inRequest, userId)
              .flatMap(newBodyBytes -> {

                // BƯỚC 4. Đóng gói lại Request với Header mới và Body JSON đã biến đổi xong
                ServerHttpRequest mutatedRequest =
                    serverHttpRequest.mutate().uri(routeConfig.getUri()).build();

                // Tạo Decorator để bọc Request hiện tại, ghi đè lại nội dung Body và Header
                ServerHttpRequestDecorator requestDecorator =
                    new ServerHttpRequestDecorator(mutatedRequest) {
                      @Override
                      public Flux<DataBuffer> getBody() {
                        // Trả về Body mới
                        return Flux.just(exchange.getResponse().bufferFactory().wrap(newBodyBytes));
                      }

                      @Override
                      public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.putAll(super.getHeaders());
                        // Đưa thêm các Header bắt buộc vào request
                        for (Map.Entry<String, String> entry : routeConfig.getHeaders()
                            .entrySet()) {
                          headers.set(entry.getKey(), entry.getValue());
                        }
                        // Phải gỡ bỏ và tính toán lại CONTENT_LENGTH vì độ dài Body JSON đã thay
                        // đổi
                        headers.remove(HttpHeaders.CONTENT_LENGTH);
                        headers.setContentLength(newBodyBytes.length);
                        return headers;
                      }
                    };

                // Chạy tiếp luồng Spring Cloud Gateway với Request mới
                return chain.filter(exchange.mutate().request(requestDecorator).build());
              });

        } catch (Exception e) {
          // Bắt các lỗi trong quá trình Parsing
          log.error(LogConstant.TransformLog.LOG_TRANSFORM_REQUEST_BODY_PARSE_FAIL, e.getMessage(),
              e);
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
              ErrorMessage.GatewayError.ERR_TRANSFORM_REQUEST_BODY_PARSE_FAIL));
        }
      });
    };
  }
}
