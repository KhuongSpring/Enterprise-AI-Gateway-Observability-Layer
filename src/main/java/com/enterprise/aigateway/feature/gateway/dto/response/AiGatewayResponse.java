package com.enterprise.aigateway.feature.gateway.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiGatewayResponse {
  private String id;
  private String model;
  private String content;
  private Usage usage;

  @Data
  @Builder
  public static class Usage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
  }
}
