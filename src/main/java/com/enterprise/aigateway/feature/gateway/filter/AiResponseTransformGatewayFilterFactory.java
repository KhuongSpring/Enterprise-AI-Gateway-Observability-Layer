package com.enterprise.aigateway.feature.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.stereotype.Component;

import com.enterprise.aigateway.constant.CommonConstant;
import com.enterprise.aigateway.feature.gateway.service.AiResponseTransformService;

import reactor.core.publisher.Mono;

/**
 * Filter dùng để chặn kết quả trả về từ các nhà cung cấp LLM và chuẩn hóa lại thành một cấu trúc
 * JSON chung của hệ thống trước khi trả về cho Client.
 */
@Component
public class AiResponseTransformGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

  // Sử dụng Filter hỗ trợ sẵn của Spring Cloud Gateway để ghi đè (rewrite)
  // Response Body
  private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory;

  // Dịch vụ chứa logic parse JSON thực tế
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
    // Cấu hình việc chỉnh sửa Response Body
    ModifyResponseBodyGatewayFilterFactory.Config modifyConfig =
        new ModifyResponseBodyGatewayFilterFactory.Config().setInClass(String.class) // Kiểu dữ liệu
                                                                                     // nhận vào
                                                                                     // (chuỗi JSON
                                                                                     // từ Provider)
            .setOutClass(String.class) // Kiểu dữ liệu xuất ra (chuỗi JSON chuẩn hóa)
            .setRewriteFunction(String.class, String.class, (exchange, inBody) -> {
              String provider = exchange.getAttribute(CommonConstant.AI_PROVIDER);
              String userId = exchange.getAttribute("X-User-Id");

              // Nếu thiếu thông tin hoặc body rỗng thì giữ nguyên kết quả gốc
              if (provider == null || inBody == null) {
                return Mono.justOrEmpty(inBody);
              }

              // KHÔNG thực hiện transform nếu Provider trả về lỗi HTTP (ví dụ: 401
              // Unauthorized, 429 Too Many Requests, 500...)
              if (exchange.getResponse().getStatusCode() != null
                  && exchange.getResponse().getStatusCode().isError()) {
                return Mono.justOrEmpty(inBody);
              }

              // Gắn user mặc định nếu không có truyền lên
              if (userId == null) {
                userId = "anonymous";
              }

              return transformService.transform(inBody, provider, userId);
            });

    return modifyResponseBodyFilterFactory.apply(modifyConfig);
  }
}
