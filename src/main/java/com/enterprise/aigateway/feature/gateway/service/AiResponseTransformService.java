package com.enterprise.aigateway.feature.gateway.service;

import org.springframework.stereotype.Service;

import com.enterprise.aigateway.constant.CommonConstant;
import com.enterprise.aigateway.constant.LogConstant;
import com.enterprise.aigateway.feature.gateway.dto.response.AiGatewayResponse;
import com.enterprise.aigateway.feature.gateway.dto.response.GeminiResponse;
import com.enterprise.aigateway.feature.gateway.dto.response.OpenAiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiResponseTransformService {
  private final ObjectMapper objectMapper;

  public AiResponseTransformService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String transform(String responseBody, String provider) {
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
        if (geminiRes.getUsageMetadata() != null) {
          usage.setPromptTokens(geminiRes.getUsageMetadata().getPromptTokenCount());
          usage.setCompletionTokens(geminiRes.getUsageMetadata().getCandidatesTokenCount());
          usage.setTotalTokens(geminiRes.getUsageMetadata().getTotalTokenCount());
        }

        AiGatewayResponse unified = AiGatewayResponse.builder()
            .id(geminiRes.getResponseId())
            .model(geminiRes.getModelVersion())
            .content(content)
            .usage(usage)
            .build();

        return objectMapper.writeValueAsString(unified);
      } else {
        OpenAiResponse openaiRes = objectMapper.readValue(responseBody, OpenAiResponse.class);

        String content = "";
        if (openaiRes.getChoices() != null && !openaiRes.getChoices().isEmpty()
            && openaiRes.getChoices().get(0).getMessage() != null) {
          content = openaiRes.getChoices().get(0).getMessage().getContent();
        }

        AiGatewayResponse.Usage usage = AiGatewayResponse.Usage.builder().build();
        if (openaiRes.getUsage() != null) {
          usage.setPromptTokens(openaiRes.getUsage().getPromptTokens());
          usage.setCompletionTokens(openaiRes.getUsage().getCompletionTokens());
          usage.setTotalTokens(openaiRes.getUsage().getTotalTokens());
        }

        AiGatewayResponse unified = AiGatewayResponse.builder()
            .id(openaiRes.getId())
            .model(openaiRes.getModel())
            .content(content)
            .usage(usage)
            .build();

        return objectMapper.writeValueAsString(unified);
      }
    } catch (Exception e) {
      log.error(LogConstant.LOG_TRANSFORM_PROVIDER_RESPONSE, provider, e.getMessage());
      return responseBody;
    }
  }
}
