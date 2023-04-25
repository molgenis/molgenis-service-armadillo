package org.molgenis.r;

public abstract class RServerException extends Exception {

  public RServerException(String msg) {
    super(msg);
  }

  public RServerException(String msg, Throwable throwable) {
    super(msg, throwable);
  }

  public RServerException(Throwable throwable) {
    super(throwable);
  }
}
