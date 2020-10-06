package com.github.couchversion.exception;

/**
 * @author deniswsrosa
 */
public class CouchVersionConfigurationVersionException extends CouchVersionException {
  public CouchVersionConfigurationVersionException(String message) {
    super(message);
  }

  public CouchVersionConfigurationVersionException(String message, Throwable e) {
    super(message, e);
  }
}
