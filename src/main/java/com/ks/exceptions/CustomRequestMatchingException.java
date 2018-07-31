package com.ks.exceptions;

public final class CustomRequestMatchingException extends Exception {
	public CustomRequestMatchingException() {
	}

	public CustomRequestMatchingException(String msg) {
		super(msg);
	}

	public CustomRequestMatchingException(Throwable cause) {
		super(cause);
	}

	public CustomRequestMatchingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}


