package com.ks.exceptions;

public final class ProductionModeCheckingException extends Exception {
	public ProductionModeCheckingException() {
	}

	public ProductionModeCheckingException(String msg) {
		super(msg);
	}

	public ProductionModeCheckingException(Throwable cause) {
		super(cause);
	}

	public ProductionModeCheckingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}


