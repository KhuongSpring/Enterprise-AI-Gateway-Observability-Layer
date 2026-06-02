package com.enterprise.aigateway.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class NotFoundException extends RuntimeException {

  private Object errMessage;

  private HttpStatus status;

  private String[] params;

  public NotFoundException(String errMessage) {
    super(errMessage);
    this.status = HttpStatus.NOT_FOUND;
    this.errMessage = errMessage;
  }

  public NotFoundException(HttpStatus status, Object errMessage) {
    super(String.valueOf(errMessage));
    this.status = status;
    this.errMessage = errMessage;
  }

  public NotFoundException(String errMessage, String[] params) {
    super(errMessage);
    this.status = HttpStatus.NOT_FOUND;
    this.errMessage = errMessage;
    this.params = params;
  }

  public NotFoundException(HttpStatus status, Object errMessage, String[] params) {
    super(String.valueOf(errMessage));
    this.status = status;
    this.errMessage = errMessage;
    this.params = params;
  }

}
