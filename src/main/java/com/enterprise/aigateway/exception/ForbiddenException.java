package com.enterprise.aigateway.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ForbiddenException extends RuntimeException {

  private Object errMessage;

  private HttpStatus status;

  private String[] params;

  public ForbiddenException(String errMessage) {
    super(errMessage);
    this.status = HttpStatus.FORBIDDEN;
    this.errMessage = errMessage;
  }

  public ForbiddenException(HttpStatus status, Object errMessage) {
    super(String.valueOf(errMessage));
    this.status = status;
    this.errMessage = errMessage;
  }

  public ForbiddenException(String errMessage, String[] params) {
    super(errMessage);
    this.status = HttpStatus.FORBIDDEN;
    this.errMessage = errMessage;
    this.params = params;
  }

  public ForbiddenException(HttpStatus status, Object errMessage, String[] params) {
    super(String.valueOf(errMessage));
    this.status = status;
    this.errMessage = errMessage;
    this.params = params;
  }

}