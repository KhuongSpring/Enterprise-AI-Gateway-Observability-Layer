package com.enterprise.aigateway.feature.cost.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.stereotype.Service;

@Service
public class TokenCounterService {
  private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

  // Mặc định sử dụng bộ mã hóa CL100K_BASE (thường dùng cho GPT-3.5-turbo, GPT-4
  // của OpenAI)
  private final Encoding defaultEncoding = registry.getEncoding(EncodingType.CL100K_BASE);

  /**
   * Tính toán (ước lượng) số lượng token của đoạn text theo cơ chế của từng Model.
   *
   * @param modelName Tên của AI model (ví dụ: gpt-4, gpt-3.5-turbo)
   * @param text Nội dung văn bản (Prompt) cần đếm
   * @return Tổng số token ước lượng
   */
  public int countTokens(String modelName, String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }

    // Tìm bộ mã hóa chính xác cho model tương ứng, nếu thư viện chưa hỗ trợ model
    // này thì dùng mặc định (CL100K_BASE)
    Encoding encoding = registry.getEncodingForModel(modelName).orElse(defaultEncoding);

    int baseTokens = encoding.countTokensOrdinary(text);

    // Overhead (Token hao phí): Khi gọi Chat Completion API, dữ liệu không chỉ có
    // text mà còn bị bọc trong cấu trúc JSON (role, name...).
    // Con số 7 là mức độ hao phí token xấp xỉ phổ biến cho 1 message.
    int chatMessageOverhead = 7;

    return baseTokens + chatMessageOverhead;
  }
}
