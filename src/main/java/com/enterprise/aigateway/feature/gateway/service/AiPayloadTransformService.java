package com.enterprise.aigateway.feature.gateway.service;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import com.enterprise.aigateway.constant.CommonConstant;
import com.enterprise.aigateway.constant.ErrorMessage;
import com.enterprise.aigateway.constant.LogConstant;
import com.enterprise.aigateway.feature.cost.service.RateLimiterService;
import com.enterprise.aigateway.feature.cost.service.TokenCounterService;
import com.enterprise.aigateway.feature.gateway.dto.request.AiGatewayRequest;
import com.enterprise.aigateway.feature.gateway.dto.request.GeminiRequest;
import com.enterprise.aigateway.feature.gateway.dto.request.OpenAiRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiPayloadTransformService {

  private final TokenCounterService tokenCounterService;
  private final RateLimiterService rateLimiterService;
  private final ObjectMapper objectMapper;

  public AiPayloadTransformService(TokenCounterService tokenCounterService,
      RateLimiterService rateLimiterService,
      ObjectMapper objectMapper) {
    this.tokenCounterService = tokenCounterService;
    this.rateLimiterService = rateLimiterService;
    this.objectMapper = objectMapper;
  }

  public Mono<byte[]> transformPayload(AiGatewayRequest inRequest, String userId) {
    String model = inRequest.getModel();

    int tokenCost = tokenCounterService.countTokens(inRequest.getPrompt());
    log.info(LogConstant.LOG_REQUEST_TOKEN, userId, tokenCost);

    return rateLimiterService.isAllowed(userId, tokenCost).flatMap(allowed -> {
      if (allowed) {
        try {
          if (model.toLowerCase().startsWith(CommonConstant.GEMINI)) {
            GeminiRequest geminiReq = GeminiRequest.builder()
                .contents(Collections.singletonList(
                    GeminiRequest.Content.builder()
                        .parts(Collections.singletonList(
                            GeminiRequest.Part.builder().text(inRequest.getPrompt()).build()))
                        .build()))
                .build();
            return Mono.just(objectMapper.writeValueAsBytes(geminiReq));
          } else {
            OpenAiRequest outRequest = OpenAiRequest.builder()
                .model(model)
                .messages(Collections.singletonList(
                    OpenAiRequest.Message.builder()
                        .role("user")
                        .content(inRequest.getPrompt())
                        .build()))
                .build();
            return Mono.just(objectMapper.writeValueAsBytes(outRequest));
          }
        } catch (JsonProcessingException e) {
          return Mono.error(
              new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessage.JSON_SERIALIZATION_ERROR));
        }
      } else {
        log.warn(LogConstant.LOG_USER_EXCEEDED_TOKEN, userId);
        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ErrorMessage.RATE_LIMIT_EXCEEDED));
      }
    });
  }
}
