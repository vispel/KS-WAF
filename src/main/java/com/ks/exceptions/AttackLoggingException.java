package com.ks.exceptions;

public final class AttackLoggingException extends RuntimeException {

	public AttackLoggingException() {
	}

	public AttackLoggingException(String msg) {
		super(msg);
	}

	public AttackLoggingException(Throwable cause) {
		super(cause);
	}

	public AttackLoggingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
