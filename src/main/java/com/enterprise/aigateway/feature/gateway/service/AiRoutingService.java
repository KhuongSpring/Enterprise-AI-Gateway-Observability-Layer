package com.enterprise.aigateway.feature.gateway.service;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ xác định tuyến đường (Routing). Nhiệm vụ chính: Phân tích tên Model để quyết định xem
 * request nên được chuyển tiếp đến URL đích nào. Đồng thời nhúng các API Key bảo mật tương ứng vào
 * cấu hình Header.
 */
@Service
public class AiRoutingService {
  @Value("${OPENAI_API_KEY:}")
  private String openaiKey;

  @Value("${GEMINI_API_KEY:}")
  private String geminiKey;

  /**
   * Trả về Cấu hình Định tuyến (URL đích và Header xác thực) dựa trên tên Model đầu vào.
   */
  public AiRouteConfig determineRoute(String model) {
    // Model Gemini
    if (model != null && model.toLowerCase().startsWith("gemini")) {
      return new AiRouteConfig(
          URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model
              + ":generateContent"),
          // Header xác thực của Google API sử dụng từ khóa "x-goog-api-key"
          Map.of("x-goog-api-key", geminiKey));
    }

    // Mặc định (fallback) nếu không nhận diện được sẽ định tuyến sang OpenAI API
    return new AiRouteConfig(URI.create("https://api.openai.com/v1/chat/completions"),
        // Header xác thực của OpenAI tuân theo chuẩn OAuth Bearer Token
        Map.of("Authorization", "Bearer " + openaiKey));
  }

  /**
   * Lớp chứa thông tin cấu hình mạng trung gian để Gateway thực hiện Forward Request.
   */
  public static class AiRouteConfig {
    private final URI uri;
    private final Map<String, String> headers;

    public AiRouteConfig(URI uri, Map<String, String> headers) {
      this.uri = uri;
      this.headers = headers;
    }

    public URI getUri() {
      return uri;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }
  }
}
