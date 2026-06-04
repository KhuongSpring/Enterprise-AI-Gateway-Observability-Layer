package com.enterprise.aigateway.feature.gateway.dto.request;

import java.util.List;

import com.enterprise.aigateway.constant.ErrorMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpenAiRequest {
  @NotBlank(message = ErrorMessage.GatewayError.ERR_OPENAI_MODEL_NOT_BLANK)
  private String model;

  @NotEmpty(message = ErrorMessage.GatewayError.ERR_OPENAI_MESSAGES_NOT_EMPTY)
  private List<Message> messages;

  @Data
  @Builder
  public static class Message {
    @NotBlank(message = ErrorMessage.GatewayError.ERR_OPENAI_MESSAGES_ROLE_NOT_BLANK)
    private String role;

    @NotBlank(message = ErrorMessage.GatewayError.ERR_OPENAI_MESSAGES_CONTENT_NOT_BLANK)
    private String content;
  }
}
