package com.github.couchversion.exception;

/**
 * @author deniswsrosa
 */
public class CouchVersionException extends Exception {
  public CouchVersionException(String message) {
    super(message);
  }

  public CouchVersionException(String message, Throwable cause) {
    super(message, cause);
  }
}
