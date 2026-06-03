package com.enterprise.aigateway.feature.gateway.controller;

import com.enterprise.aigateway.common.RestApiV1;
import com.enterprise.aigateway.common.RestData;
import com.enterprise.aigateway.common.VsResponseUtil;
import com.enterprise.aigateway.constant.ErrorMessage;
import com.enterprise.aigateway.constant.UrlConstant;
import com.enterprise.aigateway.feature.gateway.dto.response.AiGatewayResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Slf4j
@RestApiV1
public class FallbackController {

  @RequestMapping(UrlConstant.LLM_URL)
  public Mono<ResponseEntity<RestData<AiGatewayResponse>>> llmFallback() {
    log.error(ErrorMessage.ERR_CIRCUIT_BREAKER_OPEN);

    AiGatewayResponse response = AiGatewayResponse.builder().id(null).model("system-fallback")
        .content(
            "System is experiencing high load or LLM provider is unavailable. Please try again later.")
        .usage(null).build();

    return Mono.just(VsResponseUtil.error(response));
  }
}
