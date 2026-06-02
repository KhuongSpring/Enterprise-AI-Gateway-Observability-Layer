package com.enterprise.aigateway.feature.gateway.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiResponse {
  private String id;
  private String model;
  private List<Choice> choices;
  private Usage usage;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Choice {
    private Message message;
    @JsonProperty("finish_reason")
    private String finishReason;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Message {
    private String role;
    private String content;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Usage {
    @JsonProperty("prompt_tokens")
    private int promptTokens;
    @JsonProperty("completion_tokens")
    private int completionTokens;
    @JsonProperty("total_tokens")
    private int totalTokens;
  }
}
