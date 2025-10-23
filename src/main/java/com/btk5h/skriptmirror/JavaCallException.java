package com.btk5h.skriptmirror;

public class JavaCallException extends RuntimeException {
	public JavaCallException(String message) {
		super(message);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
