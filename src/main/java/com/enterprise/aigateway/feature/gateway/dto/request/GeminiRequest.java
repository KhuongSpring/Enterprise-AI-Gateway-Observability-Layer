package com.enterprise.aigateway.feature.gateway.dto.request;

import java.util.List;

import com.enterprise.aigateway.constant.ErrorMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeminiRequest {
  @NotEmpty(message = ErrorMessage.GatewayError.ERR_GEMINI_CONTENTS_NOT_EMPTY)
  private List<Content> contents;

  @Data
  @Builder
  public static class Content {
    @NotEmpty(message = ErrorMessage.GatewayError.ERR_GEMINI_PARTS_NOT_EMPTY)
    private List<Part> parts;
  }

  @Data
  @Builder
  public static class Part {
    @NotBlank(message = ErrorMessage.GatewayError.ERR_GEMINI_PARTS_TEXT_NOT_BLANK)
    private String text;
  }
}
