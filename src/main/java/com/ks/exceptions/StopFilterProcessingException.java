package com.ks.exceptions;


public class StopFilterProcessingException
		extends Exception {
	public StopFilterProcessingException() {
	}

	public StopFilterProcessingException(String msg) {
		super(msg);
	}

	public StopFilterProcessingException(Throwable cause) {
		super(cause);
	}

	public StopFilterProcessingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

