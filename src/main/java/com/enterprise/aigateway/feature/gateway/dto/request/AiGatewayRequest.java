package com.enterprise.aigateway.feature.gateway.dto.request;

import com.enterprise.aigateway.constant.ErrorMessage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiGatewayRequest {
  @NotBlank(message = ErrorMessage.ERR_MODEL_NOT_BLANK)
  private String model;

  @NotBlank(message = ErrorMessage.ERR_PROMPT_NOT_BLANK)
  private String prompt;
}
