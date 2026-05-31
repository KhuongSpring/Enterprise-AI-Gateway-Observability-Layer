package com.enterprise.aigateway.feature.gateway.controller;

import com.enterprise.aigateway.common.RestData;
import com.enterprise.aigateway.common.VsResponseUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @RequestMapping("/llm")
  public Mono<ResponseEntity<RestData<String>>> llmFallback() {
    log.error("Circuit Breaker OPEN: Dịch vụ LLM (OpenAI/Gemini) đang gặp sự cố hoặc quá tải.");

    return Mono.just(VsResponseUtil.error(HttpStatus.SERVICE_UNAVAILABLE,
        "Dịch vụ AI hiện đang quá tải hoặc gặp sự cố. Vui lòng thử lại sau ít phút."));
  }
}