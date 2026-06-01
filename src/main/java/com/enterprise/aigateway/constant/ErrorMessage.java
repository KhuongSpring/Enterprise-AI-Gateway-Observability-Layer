package com.enterprise.aigateway.constant;

public class ErrorMessage {
  public static final String ERR_EXCEPTION_GENERAL = "exception.general";
  public static final String UNAUTHORIZED = "exception.unauthorized";
  public static final String FORBIDDEN = "exception.forbidden";
  public static final String BAD_REQUEST = "exception.bad.request";
  public static final String FORBIDDEN_UPDATE_DELETE = "exception.forbidden.update-delete";
  public static final String ERR_UPLOAD_IMAGE_FAIL = "exception.upload.image.fail";

  // error validation dto
  public static final String INVALID_SOME_THING_FIELD = "invalid.general";
  public static final String INVALID_FORMAT_SOME_THING_FIELD = "invalid.general.format";
  public static final String INVALID_SOME_THING_FIELD_IS_REQUIRED = "invalid.general.required";
  public static final String NOT_BLANK_FIELD = "invalid.general.not-blank";
  public static final String INVALID_FORMAT_PASSWORD = "invalid.password-format";
  public static final String INVALID_DATE = "invalid.date-format";

  public static final String JSON_SERIALIZATION_ERROR = "exception.json.serialization";
  public static final String RATE_LIMIT_EXCEEDED = "exception.rate.limit.exceeded";

  public static final String ERR_CIRCUIT_BREAKER_OPEN = "Dịch vụ AI hiện đang quá tải hoặc gặp sự cố. Vui lòng thử lại sau ít phút";

  public static final String ERR_MODEL_NOT_BLANK = "Model không được để trống";
  public static final String ERR_PROMPT_NOT_BLANK = "Prompt không được để trống";

  public static final String ERR_GEMINI_CONTENTS_NOT_EMPTY = "Contents không được để trống";
  public static final String ERR_GEMINI_PARTS_NOT_EMPTY = "Parts không được để trống";
  public static final String ERR_GEMINI_PARTS_TEXT_NOT_BLANK = "Parts text không được để trống";

  public static final String ERR_OPENAI_MESSAGES_NOT_EMPTY = "Messages không được để trống";
  public static final String ERR_OPENAI_MESSAGES_ROLE_NOT_BLANK = "Role không được để trống";
  public static final String ERR_OPENAI_MESSAGES_CONTENT_NOT_BLANK = "Content không được để trống";
}
