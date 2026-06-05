package com.enterprise.aigateway.feature.gateway.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
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

/**
 * Dịch vụ chịu trách nhiệm 2 việc: 1. Kiểm tra Token 2. Biến đổi Payload
 */
@Slf4j
@Service
public class AiPayloadTransformService {

  private final TokenCounterService tokenCounterService;
  private final RateLimiterService rateLimiterService;
  private final ObjectMapper objectMapper;

  // Cấu hình giới hạn tối đa token cho 1 lần hỏi
  @Value("${application.aigateway.cost.max-prompt-tokens:4000}")
  private int maxPromptTokens;

  public AiPayloadTransformService(TokenCounterService tokenCounterService,
      RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
    this.tokenCounterService = tokenCounterService;
    this.rateLimiterService = rateLimiterService;
    this.objectMapper = objectMapper;
  }

  /**
   * Biến đổi từ Request ban đầu thành dạng mảng byte chứa cấu trúc JSON đích của AI Provider.
   */
  public Mono<byte[]> transformPayload(AiGatewayRequest inRequest, String userId) {
    String model = inRequest.getModel();

    // BƯỚC 1: Đếm số lượng Token dự kiến của câu hỏi
    int tokenCost = tokenCounterService.countTokens(model, inRequest.getPrompt());
    log.info(LogConstant.TokenCostLog.LOG_TOKEN_COST_REQUEST_COST, userId, tokenCost);

    // BƯỚC 2: Chặn ngay lập tức nếu câu hỏi vượt quá giới hạn
    if (tokenCost > maxPromptTokens) {
      log.warn(LogConstant.TokenCostLog.LOG_TOKEN_COST_USER_EXCEEDED_MAX_PROMPT_TOKENS, userId,
          tokenCost, maxPromptTokens);
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
          ErrorMessage.GatewayError.ERR_PROMPT_EXCEEDED_MAX_TOKENS));
    }

    // BƯỚC 3: Kiểm tra hạn mức của User trong Redis
    // Hàm này chạy bất đồng bộ nên trả về kiểu Mono<Boolean>
    return rateLimiterService.isAllowed(userId, tokenCost).flatMap(allowed -> {
      if (allowed) {
        try {
          if (model.toLowerCase().startsWith(CommonConstant.GEMINI)) {
            // Build JSON theo chuẩn của Google Gemini API
            GeminiRequest geminiReq = GeminiRequest.builder()
                .contents(Collections.singletonList(GeminiRequest.Content.builder()
                    .parts(Collections.singletonList(
                        GeminiRequest.Part.builder().text(inRequest.getPrompt()).build()))
                    .build()))
                .build();
            // Serialize thành mảng byte để gán ngược lại vào HTTP Body của Gateway
            return Mono.just(objectMapper.writeValueAsBytes(geminiReq));

          } else {
            // Build JSON theo chuẩn của OpenAI API
            OpenAiRequest outRequest = OpenAiRequest.builder().model(model)
                .messages(Collections.singletonList(OpenAiRequest.Message.builder().role("user")
                    .content(inRequest.getPrompt()).build()))
                .build();
            // Serialize thành mảng byte
            return Mono.just(objectMapper.writeValueAsBytes(outRequest));
          }
        } catch (JsonProcessingException e) {
          // Lỗi này xảy ra khi parse Object thành String JSON thất bại
          return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
              ErrorMessage.GatewayError.ERR_TRANSFORM_JSON_SERIALIZATION));
        }
      } else {
        // User đã hết hạn mức Token trong ngày
        log.warn(LogConstant.TokenCostLog.LOG_TOKEN_COST_USER_EXCEEDED_TOKEN, userId);
        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
            ErrorMessage.GatewayError.ERR_RATE_LIMIT_EXCEEDED));
      }
    });
  }
}
