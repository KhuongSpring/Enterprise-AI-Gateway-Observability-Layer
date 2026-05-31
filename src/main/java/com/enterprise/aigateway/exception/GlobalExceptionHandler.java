package com.enterprise.aigateway.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.enterprise.aigateway.common.RestData;
import com.enterprise.aigateway.common.VsResponseUtil;
import com.enterprise.aigateway.constant.ErrorMessage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  // Error validate for param
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<RestData<Map<String, String>>> handleConstraintViolationException(
      ConstraintViolationException ex) {
    Map<String, String> result = new LinkedHashMap<>();
    ex.getConstraintViolations().forEach((error) -> {
      String fieldName = ((PathImpl) error.getPropertyPath()).getLeafNode().getName();
      String errorMessage = messageSource.getMessage(Objects.requireNonNull(error.getMessage()), null,
          LocaleContextHolder.getLocale());
      result.put(fieldName, errorMessage);
    });
    return VsResponseUtil.error(HttpStatus.BAD_REQUEST, result);
  }

  // Error validate for body
  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<RestData<Map<String, String>>> handleValidException(WebExchangeBindException ex) {
    Map<String, String> result = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = messageSource.getMessage(Objects.requireNonNull(error.getDefaultMessage()), null,
          LocaleContextHolder.getLocale());
      result.put(fieldName, errorMessage);
    });
    return VsResponseUtil.error(HttpStatus.BAD_REQUEST, result);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<RestData<String>> handlerInternalServerError(Exception ex) {
    log.error(ex.getMessage(), ex);
    String message = messageSource.getMessage(ErrorMessage.ERR_EXCEPTION_GENERAL, null,
        LocaleContextHolder.getLocale());
    return VsResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  // Exception custom
  @ExceptionHandler(VsException.class)
  public ResponseEntity<RestData<Object>> handleVsException(VsException ex) {
    String message = ex.getMessage();
    try {
      message = messageSource.getMessage(Objects.requireNonNull(message), ex.getParams(),
          LocaleContextHolder.getLocale());
    } catch (Exception e) {
      // Keep original message if it's not a translation key
    }
    log.error("VsException: {}", message);
    return VsResponseUtil.error(ex.getStatus(), message);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<RestData<String>> handlerNotFoundException(NotFoundException ex) {
    String message = ex.getMessage();
    try {
      message = messageSource.getMessage(Objects.requireNonNull(message), ex.getParams(),
          LocaleContextHolder.getLocale());
    } catch (Exception e) {
      // Keep original message if it's not a translation key
    }
    log.error(message, ex);
    return VsResponseUtil.error(ex.getStatus(), message);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<RestData<String>> handlerForbiddenException(ForbiddenException ex) {
    String message = ex.getMessage();
    try {
      message = messageSource.getMessage(Objects.requireNonNull(message), ex.getParams(),
          LocaleContextHolder.getLocale());
    } catch (Exception e) {
      // Keep original message if it's not a translation key
    }
    log.error(message, ex);
    return VsResponseUtil.error(ex.getStatus(), message);
  }

}