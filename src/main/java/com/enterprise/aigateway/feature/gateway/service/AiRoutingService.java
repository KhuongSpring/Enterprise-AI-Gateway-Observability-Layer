package com.enterprise.aigateway.feature.gateway.service;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRoutingService {
  @Value("${OPENAI_API_KEY}")
  private String openaiKey;

  @Value("${GEMINI_API_KEY}")
  private String geminiKey;

  public AiRouteConfig determineRoute(String model) {
    if (model != null && model.toLowerCase().startsWith("gemini")) {
      return new AiRouteConfig(
          URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"),
          Map.of("x-goog-api-key", geminiKey));
    }
    return new AiRouteConfig(
        URI.create("https://api.openai.com/v1/chat/completions"),
        Map.of("Authorization", "Bearer " + openaiKey));
  }

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
