package ru.dimension.ui.exception;

public class TimeoutConnectionException extends RuntimeException {

  public TimeoutConnectionException(String message) {
    super(message);
  }
}
