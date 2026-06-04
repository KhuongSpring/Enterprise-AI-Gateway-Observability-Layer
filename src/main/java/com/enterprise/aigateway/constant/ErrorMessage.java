package com.enterprise.aigateway.constant;

public final class ErrorMessage {
  private ErrorMessage() {}

  // ==========================================
  // Nhóm lỗi chung toàn hệ thống (HTTP Status)
  // ==========================================
  public static final String ERR_GENERAL = "exception.general";
  public static final String ERR_UNAUTHORIZED = "exception.unauthorized";
  public static final String ERR_FORBIDDEN = "exception.forbidden";
  public static final String ERR_BAD_REQUEST = "exception.bad.request";

  // ==========================================
  // Nhóm lỗi liên quan đến Validation DTO
  // ==========================================
  public static final String INVALID_GENERAL = "invalid.general";
  public static final String INVALID_FIELD_FORMAT = "invalid.field.format";
  public static final String INVALID_FIELD_REQUIRED = "invalid.field.required";
  public static final String INVALID_FIELD_NOT_BLANK = "invalid.field.not_blank";

  // ==========================================
  // Nhóm lỗi liên quan đến Feature Gateway
  // ==========================================
  public static final class GatewayError {
    private GatewayError() {}

    // --- Lỗi giới hạn truy cập (Rate Limit & Token) ---
    public static final String ERR_RATE_LIMIT_EXCEEDED = "exception.gateway.rate.limit.exceeded";
    public static final String ERR_PROMPT_EXCEEDED_MAX_TOKENS =
        "exception.gateway.prompt.exceeded.max.tokens";

    // --- Lỗi đầu vào chung của Gateway ---
    public static final String ERR_MODEL_NOT_BLANK = "exception.gateway.model.not_blank";
    public static final String ERR_PROMPT_NOT_BLANK = "exception.gateway.prompt.not_blank";

    // --- Lỗi kiểm tra dữ liệu riêng cho Provider Gemini ---
    public static final String ERR_GEMINI_CONTENTS_NOT_EMPTY =
        "exception.gateway.gemini.contents.not_empty";
    public static final String ERR_GEMINI_PARTS_NOT_EMPTY =
        "exception.gateway.gemini.parts.not_empty";
    public static final String ERR_GEMINI_PARTS_TEXT_NOT_BLANK =
        "exception.gateway.gemini.parts.text_not_blank";

    // --- Lỗi kiểm tra dữ liệu riêng cho Provider OpenAI ---
    public static final String ERR_OPENAI_MODEL_NOT_BLANK =
        "exception.gateway.openai.model.not_blank";
    public static final String ERR_OPENAI_MESSAGES_NOT_EMPTY =
        "exception.gateway.openai.messages.not.empty";
    public static final String ERR_OPENAI_MESSAGES_ROLE_NOT_BLANK =
        "exception.gateway.openai.messages.role.not.blank";
    public static final String ERR_OPENAI_MESSAGES_CONTENT_NOT_BLANK =
        "exception.gateway.openai.messages.content.not.blank";

    // --- Lỗi liên quan đến dự phòng (Fallback) khi có sự cố hệ thống/mạng ---
    public static final String ERR_FALLBACK_CIRCUIT_BREAKER_OPEN =
        "exception.gateway.fallback.circuit.breaker.open";

    // --- Lỗi trong quá trình biến đổi yêu cầu (Prompt Transform) ---
    public static final String ERR_TRANSFORM_REQUEST_BODY_IS_EMPTY =
        "exception.gateway.transform.request.body.is.empty";
    public static final String ERR_TRANSFORM_REQUEST_BODY_IS_INVALID =
        "exception.gateway.transform.request.body.is.invalid";
    public static final String ERR_TRANSFORM_REQUEST_BODY_PARSE_FAIL =
        "exception.gateway.transform.request.body.parse.fail";

    // --- Lỗi trong quá trình đóng gói dữ liệu (AI Payload Transform) ---
    public static final String ERR_TRANSFORM_JSON_SERIALIZATION =
        "exception.gateway.transform.json.serialization";
    public static final String ERR_TRANSFORM_JSON_PARSE_FAIL =
        "exception.gateway.transform.json.parse.fail";
  }

}
