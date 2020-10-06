package com.github.couchversion.exception;

public class CouchVersionStartupException extends RuntimeException {
  public CouchVersionStartupException(String message, Throwable t){
    super(message, t);
  }

  public CouchVersionStartupException(String message){
    super(message);
  }
}
