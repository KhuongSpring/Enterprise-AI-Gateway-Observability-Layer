package com.enterprise.aigateway.feature.gateway.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
  private List<Candidate> candidates;
  private UsageMetadata usageMetadata;
  private String modelVersion;
  private String responseId;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Candidate {
    private Content content;
    private String finishReason;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Content {
    private List<Part> parts;
    private String role;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Part {
    private String text;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UsageMetadata {
    private int promptTokenCount;
    private int candidatesTokenCount;
    private int totalTokenCount;
  }
}
