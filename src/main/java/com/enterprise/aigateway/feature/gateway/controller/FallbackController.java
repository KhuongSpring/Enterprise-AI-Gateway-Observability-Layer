package com.enterprise.aigateway.feature.gateway.controller;

import com.enterprise.aigateway.common.RestApiV1;
import com.enterprise.aigateway.common.RestData;
import com.enterprise.aigateway.common.VsResponseUtil;
import com.enterprise.aigateway.constant.ErrorMessage;
import com.enterprise.aigateway.constant.UrlConstant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Slf4j
@RestApiV1
public class FallbackController {

  @RequestMapping(UrlConstant.LLM_URL)
  public Mono<ResponseEntity<RestData<String>>> llmFallback() {
    log.error(ErrorMessage.ERR_CIRCUIT_BREAKER_OPEN);

    return Mono.just(VsResponseUtil.error(HttpStatus.SERVICE_UNAVAILABLE, ErrorMessage.ERR_CIRCUIT_BREAKER_OPEN));
  }
}