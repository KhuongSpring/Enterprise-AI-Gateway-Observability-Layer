package com.enterprise.aigateway.feature.gateway.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.enterprise.aigateway.constant.CommonConstant;
import com.enterprise.aigateway.constant.LogConstant;
import com.enterprise.aigateway.feature.gateway.dto.response.AiGatewayResponse;
import com.enterprise.aigateway.feature.gateway.dto.response.GeminiResponse;
import com.enterprise.aigateway.feature.gateway.dto.response.OpenAiResponse;
import com.enterprise.aigateway.feature.cost.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Dịch vụ xử lý kết quả trả về từ các LLM Provider. Chức năng chính: 1. Chuyển đổi JSON đặc thù của
 * Provider về chuẩn JSON chung của hệ thống. 2. Đọc lượng token THỰC TẾ đã sử dụng từ Provider trả
 * lời và gọi Redis để trừ vào ngân sách của User.
 */
@Slf4j
@Service
public class AiResponseTransformService {
  private final ObjectMapper objectMapper;
  private final RateLimiterService rateLimiterService;

  public AiResponseTransformService(ObjectMapper objectMapper,
      RateLimiterService rateLimiterService) {
    this.objectMapper = objectMapper;
    this.rateLimiterService = rateLimiterService;
  }

  /**
   * Biến đổi chuỗi JSON từ Provider thành chuỗi JSON chuẩn thống nhất.
   */
  public Mono<String> transform(String responseBody, String provider, String userId) {
    try {
      if (CommonConstant.GEMINI.equalsIgnoreCase(provider)) {
        // --- XỬ LÝ CHO GOOGLE GEMINI ---
        GeminiResponse geminiRes = objectMapper.readValue(responseBody, GeminiResponse.class);

        // Trích xuất nội dung câu trả lời từ cấu trúc lồng nhau
        String content = "";
        if (geminiRes.getCandidates() != null && !geminiRes.getCandidates().isEmpty()
            && geminiRes.getCandidates().get(0).getContent() != null
            && geminiRes.getCandidates().get(0).getContent().getParts() != null
            && !geminiRes.getCandidates().get(0).getContent().getParts().isEmpty()) {
          content = geminiRes.getCandidates().get(0).getContent().getParts().get(0).getText();
        }

        // Lấy thông tin lượng token đã tiêu thụ từ metadata của Gemini
        AiGatewayResponse.Usage usage = AiGatewayResponse.Usage.builder().build();
        int totalTokens = 0;
        if (geminiRes.getUsageMetadata() != null) {
          usage.setPromptTokens(geminiRes.getUsageMetadata().getPromptTokenCount());
          usage.setCompletionTokens(geminiRes.getUsageMetadata().getCandidatesTokenCount());
          usage.setTotalTokens(geminiRes.getUsageMetadata().getTotalTokenCount());
          totalTokens = geminiRes.getUsageMetadata().getTotalTokenCount();
        }

        // Tạo dữ liệu chuẩn (AiGatewayResponse)
        AiGatewayResponse unified = AiGatewayResponse.builder().id(geminiRes.getResponseId())
            .model(geminiRes.getModelVersion()).content(content).usage(usage).build();

        String finalResponse = objectMapper.writeValueAsString(unified);
        final int deductedTokens = totalTokens;

        // Bỏ qua gọi Redis nếu không thu thập được số token tiêu thụ
        if (deductedTokens <= 0) {
          return Mono.just(finalResponse);
        }

        // Gọi Redis để trừ Token. Sử dụng chiến lược "best-effort" (cố gắng hết sức):
        // Nếu trừ Redis lỗi (onErrorResume) thì in log cảnh báo, KHÔNG quăng Exception
        // làm đứt mạch, vẫn trả về kết quả AI cho Client.
        return rateLimiterService.deductTokens(userId, deductedTokens).doOnSuccess(success -> {
          if (Boolean.TRUE.equals(success)) {
            log.info(LogConstant.TokenCostLog.LOG_TOKEN_COST_USER_TOKENS_DEDUCTED, userId,
                deductedTokens);
          }
        }).onErrorResume(e -> {
          log.warn(LogConstant.TokenCostLog.LOG_TOKEN_COST_USER_TOKENS_FAILED, userId,
              e.getMessage());
          return Mono.just(false);
        }).thenReturn(finalResponse);

      } else {
        // --- XỬ LÝ CHO OPENAI ---
        OpenAiResponse openaiRes = objectMapper.readValue(responseBody, OpenAiResponse.class);

        // Trích xuất nội dung
        String content = "";
        if (openaiRes.getChoices() != null && !openaiRes.getChoices().isEmpty()
            && openaiRes.getChoices().get(0).getMessage() != null) {
          content = openaiRes.getChoices().get(0).getMessage().getContent();
        }

        // Trích xuất thông tin tiêu thụ token
        AiGatewayResponse.Usage usage = AiGatewayResponse.Usage.builder().build();
        int totalTokens = 0;
        if (openaiRes.getUsage() != null) {
          usage.setPromptTokens(openaiRes.getUsage().getPromptTokens());
          usage.setCompletionTokens(openaiRes.getUsage().getCompletionTokens());
          usage.setTotalTokens(openaiRes.getUsage().getTotalTokens());
          totalTokens = openaiRes.getUsage().getTotalTokens();
        }

        // Tạo dữ liệu chuẩn (AiGatewayResponse)
        AiGatewayResponse unified = AiGatewayResponse.builder().id(openaiRes.getId())
            .model(openaiRes.getModel()).content(content).usage(usage).build();

        String finalResponse = objectMapper.writeValueAsString(unified);
        final int deductedTokens = totalTokens;

        // Bỏ qua gọi Redis nếu không thu thập được số token tiêu thụ
        if (deductedTokens <= 0) {
          return Mono.just(finalResponse);
        }

        // Cố gắng trừ token trong Redis ("best-effort")
        return rateLimiterService.deductTokens(userId, deductedTokens).doOnSuccess(success -> {
          if (Boolean.TRUE.equals(success)) {
            log.info(LogConstant.TokenCostLog.LOG_TOKEN_COST_USER_TOKENS_DEDUCTED, userId,
                deductedTokens);
          }
        }).onErrorResume(e -> {
          log.warn(LogConstant.TokenCostLog.LOG_TOKEN_COST_USER_TOKENS_FAILED, userId,
              e.getMessage());
          return Mono.just(false);
        }).thenReturn(finalResponse);
      }
    } catch (Exception e) {
      // Bắt các lỗi parsing
      // Khi đó, in log lỗi và trả nguyên Body gốc từ Provider để Client tự xử lý
      log.error(LogConstant.TransformLog.LOG_TRANSFORM_PROVIDER_RESPONSE, provider, e.getMessage(),
          e);
      return Mono.just(responseBody);
    }
  }
}
