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

  public Mono<String> transform(String responseBody, String provider, String userId) {
    try {
      if (CommonConstant.GEMINI.equalsIgnoreCase(provider)) {
        GeminiResponse geminiRes = objectMapper.readValue(responseBody, GeminiResponse.class);

        String content = "";
        if (geminiRes.getCandidates() != null && !geminiRes.getCandidates().isEmpty()
            && geminiRes.getCandidates().get(0).getContent() != null
            && geminiRes.getCandidates().get(0).getContent().getParts() != null
            && !geminiRes.getCandidates().get(0).getContent().getParts().isEmpty()) {
          content = geminiRes.getCandidates().get(0).getContent().getParts().get(0).getText();
        }

        AiGatewayResponse.Usage usage = AiGatewayResponse.Usage.builder().build();
        int totalTokens = 0;
        if (geminiRes.getUsageMetadata() != null) {
          usage.setPromptTokens(geminiRes.getUsageMetadata().getPromptTokenCount());
          usage.setCompletionTokens(geminiRes.getUsageMetadata().getCandidatesTokenCount());
          usage.setTotalTokens(geminiRes.getUsageMetadata().getTotalTokenCount());
          totalTokens = geminiRes.getUsageMetadata().getTotalTokenCount();
        }

        AiGatewayResponse unified = AiGatewayResponse.builder().id(geminiRes.getResponseId())
            .model(geminiRes.getModelVersion()).content(content).usage(usage).build();

        String finalResponse = objectMapper.writeValueAsString(unified);
        final int deductedTokens = totalTokens;
        return rateLimiterService.deductTokens(userId, deductedTokens).doOnSuccess(success -> {
          if (Boolean.TRUE.equals(success)) {
            log.info(LogConstant.LOG_USER_TOKENS_DEDUCTED, userId, deductedTokens);
          }
        }).map(success -> finalResponse);

      } else {
        OpenAiResponse openaiRes = objectMapper.readValue(responseBody, OpenAiResponse.class);

        String content = "";
        if (openaiRes.getChoices() != null && !openaiRes.getChoices().isEmpty()
            && openaiRes.getChoices().get(0).getMessage() != null) {
          content = openaiRes.getChoices().get(0).getMessage().getContent();
        }

        AiGatewayResponse.Usage usage = AiGatewayResponse.Usage.builder().build();
        int totalTokens = 0;
        if (openaiRes.getUsage() != null) {
          usage.setPromptTokens(openaiRes.getUsage().getPromptTokens());
          usage.setCompletionTokens(openaiRes.getUsage().getCompletionTokens());
          usage.setTotalTokens(openaiRes.getUsage().getTotalTokens());
          totalTokens = openaiRes.getUsage().getTotalTokens();
        }

        AiGatewayResponse unified = AiGatewayResponse.builder().id(openaiRes.getId())
            .model(openaiRes.getModel()).content(content).usage(usage).build();

        String finalResponse = objectMapper.writeValueAsString(unified);
        final int deductedTokens = totalTokens;
        return rateLimiterService.deductTokens(userId, deductedTokens).doOnSuccess(success -> {
          if (Boolean.TRUE.equals(success)) {
            log.info(LogConstant.LOG_USER_TOKENS_DEDUCTED, userId, deductedTokens);
          }
        }).map(success -> finalResponse);
      }
    } catch (Exception e) {
      log.error(LogConstant.LOG_TRANSFORM_PROVIDER_RESPONSE, provider, e.getMessage(), e);
      return Mono.just(responseBody);
    }
  }
}
