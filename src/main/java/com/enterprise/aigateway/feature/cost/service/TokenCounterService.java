package com.enterprise.aigateway.feature.cost.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.stereotype.Service;

@Service
public class TokenCounterService {
  private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
  private final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

  public int countTokens(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return encoding.encode(text).size();
  }
}