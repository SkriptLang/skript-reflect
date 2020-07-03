package com.btk5h.skriptmirror.skript.reflect.trycatch;

public class EmptyException extends RuntimeException {

  public EmptyException() {
    this.setStackTrace(new StackTraceElement[0]);
  }

}
