package com.enterprise.aigateway.feature.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.stereotype.Component;

import com.enterprise.aigateway.constant.CommonConstant;
import com.enterprise.aigateway.feature.gateway.service.AiResponseTransformService;

import reactor.core.publisher.Mono;

@Component
public class AiResponseTransformGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

  private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory;
  private final AiResponseTransformService transformService;

  public AiResponseTransformGatewayFilterFactory(
      ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory,
      AiResponseTransformService transformService) {
    super(Object.class);
    this.modifyResponseBodyFilterFactory = modifyResponseBodyFilterFactory;
    this.transformService = transformService;
  }

  @Override
  public GatewayFilter apply(Object config) {
    ModifyResponseBodyGatewayFilterFactory.Config modifyConfig =
        new ModifyResponseBodyGatewayFilterFactory.Config().setInClass(String.class)
            .setOutClass(String.class)
            .setRewriteFunction(String.class, String.class, (exchange, inBody) -> {
              String provider = exchange.getAttribute(CommonConstant.AI_PROVIDER);
              String userId = exchange.getAttribute("X-User-Id");

              if (provider == null || inBody == null) {
                return Mono.justOrEmpty(inBody);
              }

              if (userId == null) {
                userId = "anonymous";
              }

              return transformService.transform(inBody, provider, userId);
            });

    return modifyResponseBodyFilterFactory.apply(modifyConfig);
  }
}
