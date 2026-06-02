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
      return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
        DataBuffer cachedBody =
            exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
        if (cachedBody == null) {
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
              ErrorMessage.ERR_REQUEST_BODY_IS_EMPTY));
        }

        try {
          // 1. Đọc Body JSON
          byte[] bytes = new byte[cachedBody.readableByteCount()];
          cachedBody.read(bytes);
          cachedBody.readPosition(0);

          AiGatewayRequest inRequest = objectMapper.readValue(bytes, AiGatewayRequest.class);

          if (inRequest == null || inRequest.getModel() == null || inRequest.getModel().isBlank()
              || inRequest.getPrompt() == null || inRequest.getPrompt().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                ErrorMessage.ERR_REQUEST_BODY_IS_INVALID));
          }

          // 2. Chuyển cho AiRoutingService xử lý bẻ lái (URL & Header)
          AiRouteConfig routeConfig = aiRoutingService.determineRoute(inRequest.getModel());

          // Ghi lại URL đích vào thuộc tính trung gian để DynamicRoutingFilter (chạy sau)
          // ghi đè lại
          exchange.getAttributes().put(CommonConstant.TARGET_AI_URI, routeConfig.getUri());

          // Ghi lại loại Provider để AiResponseTransformGatewayFilterFactory nhận diện
          String aiProvider = inRequest.getModel().toLowerCase().startsWith(CommonConstant.GEMINI)
              ? CommonConstant.GEMINI
              : CommonConstant.OPENAI;
          exchange.getAttributes().put(CommonConstant.AI_PROVIDER, aiProvider);

          // 3. Chuyển cho AiPayloadTransform xử lý biến đổi JSON (Bất đồng bộ do check
          // Rate Limit)
          String rawUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
          String userId = (rawUserId != null) ? rawUserId : "anonymous";

          return aiPayloadTransformService.transformPayload(inRequest, userId)
              .flatMap(newBodyBytes -> {

                // 4. Đóng gói lại Request với Header mới và Body mới
                ServerHttpRequest mutatedRequest =
                    serverHttpRequest.mutate().uri(routeConfig.getUri()).build();

                ServerHttpRequestDecorator requestDecorator =
                    new ServerHttpRequestDecorator(mutatedRequest) {
                      @Override
                      public Flux<DataBuffer> getBody() {
                        return Flux.just(exchange.getResponse().bufferFactory().wrap(newBodyBytes));
                      }

                      @Override
                      public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.putAll(super.getHeaders());
                        for (Map.Entry<String, String> entry : routeConfig.getHeaders()
                            .entrySet()) {
                          headers.set(entry.getKey(), entry.getValue());
                        }
                        headers.remove(HttpHeaders.CONTENT_LENGTH);
                        headers.setContentLength(newBodyBytes.length);
                        return headers;
                      }
                    };

                // Chạy tiếp bộ lọc với Request đã được tân trang hoàn toàn
                return chain.filter(exchange.mutate().request(requestDecorator).build());
              });

        } catch (Exception e) {
          log.error(LogConstant.LOG_REQUEST_BODY_PARSE_FAIL, e.getMessage(), e);
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
              ErrorMessage.ERR_REQUEST_BODY_PARSE_FAIL));
        }
      });
    };
  }
}
