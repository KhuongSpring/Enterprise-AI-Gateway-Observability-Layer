package com.enterprise.aigateway.constant;

public class LogConstant {
  public static final String LOG_REQUEST_TOKEN = "Request từ user {} tiêu tốn khoảng {} tokens";
  public static final String LOG_USER_EXCEEDED_TOKEN = "User {} đã vượt quá ngân sách token!";
  public static final String LOG_TRANSFORM_PROVIDER_RESPONSE =
      "Lỗi khi chuẩn hóa response của provider {}: {}";
  public static final String LOG_REQUEST_BODY_PARSE_FAIL = "Lỗi khi parse request body: {}";
  public static final String LOG_USER_EXCEEDED_MAX_PROMPT_TOKENS =
      "User {} đã vượt quá số token tối đa cho prompt. Cost: {}, Max: {}";
  public static final String LOG_USER_TOKENS_DEDUCTED = "User {} đã bị trừ khoảng {} tokens";
  public static final String LOG_USER_TOKENS_FAILED = "User {} không đủ token hoặc Redis lỗi {}";
}
