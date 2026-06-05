package com.enterprise.aigateway.constant;

public final class LogConstant {

  private LogConstant() {}

  // ==========================================
  // Nhóm log chung toàn hệ thống
  // ==========================================
  public static final String LOG_DB_QUERY_EXECUTION_TIME = "{} exec in {} ms : SLOW QUERY";

  // ==========================================
  // Nhóm log liên quan đến tính toán và trừ token
  // ==========================================
  public static final class TokenCostLog {
    private TokenCostLog() {}

    public static final String LOG_TOKEN_COST_REQUEST_COST =
        "Request từ user {} tiêu tốn khoảng {} tokens";
    public static final String LOG_TOKEN_COST_USER_TOKENS_DEDUCTED =
        "User {} đã bị trừ khoảng {} tokens";

    public static final String LOG_TOKEN_COST_USER_EXCEEDED_TOKEN =
        "User {} đã vượt quá ngân sách token!";
    public static final String LOG_TOKEN_COST_USER_EXCEEDED_MAX_PROMPT_TOKENS =
        "User {} đã vượt quá số token tối đa cho prompt. Cost: {}, Max: {}";
    public static final String LOG_TOKEN_COST_USER_TOKENS_FAILED =
        "User {} không đủ token hoặc Redis lỗi {}";
  }

  // ==========================================
  // Nhóm log liên quan đến quá trình biến đổi dữ liệu (Transform)
  // ==========================================
  public static final class TransformLog {
    private TransformLog() {}

    public static final String LOG_TRANSFORM_PROVIDER_RESPONSE =
        "Lỗi khi chuẩn hóa response của provider {}: {}";
    public static final String LOG_TRANSFORM_REQUEST_BODY_PARSE_FAIL =
        "Lỗi khi parse request body: {}";
  }

  // ==========================================
  // Nhóm log liên quan đến bảo vệ hệ thống (Circuit Breaker)
  // ==========================================
  public static final class CircuitBreakerLog {
    private CircuitBreakerLog() {}

    public static final String LOG_CIRCUIT_BREAKER_STATE_CHANGED =
        "[CIRCUIT BREAKER] STATE CHANGED: {} -> {}";
    public static final String LOG_CIRCUIT_BREAKER_CALL_REJECTED =
        "[CIRCUIT BREAKER] CALL REJECTED: Mạch đang MỞ (OPEN). Không thể gọi LLM Provider.";
    public static final String LOG_CIRCUIT_BREAKER_LLM_CALL_FAILED =
        "[CIRCUIT BREAKER] LLM CALL FAILED: {}, Duration: {}ms";
  }

}
