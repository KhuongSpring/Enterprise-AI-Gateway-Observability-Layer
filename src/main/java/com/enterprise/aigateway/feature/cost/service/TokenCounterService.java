package com.enterprise.aigateway.feature.cost.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.stereotype.Service;

@Service
public class TokenCounterService {
  private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
  private final Encoding defaultEncoding = registry.getEncoding(EncodingType.CL100K_BASE);

  public int countTokens(String modelName, String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }

    Encoding encoding = registry.getEncodingForModel(modelName).orElse(defaultEncoding);

    int baseTokens = encoding.countTokensOrdinary(text);

    int chatMessageOverhead = 7;

    return baseTokens + chatMessageOverhead;
  }
}
