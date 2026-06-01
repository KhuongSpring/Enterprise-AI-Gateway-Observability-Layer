package com.enterprise.aigateway.feature.gateway.filter;

import java.net.URI;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.enterprise.aigateway.constant.CommonConstant;

import reactor.core.publisher.Mono;

@Component
public class DynamicRoutingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return 10001;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI dynamicUri = exchange.getAttribute(CommonConstant.TARGET_AI_URI);
        if (dynamicUri != null) {
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, dynamicUri);
        }
        return chain.filter(exchange);
    }
}
