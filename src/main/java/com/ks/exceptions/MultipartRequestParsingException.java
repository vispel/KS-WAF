package com.ks.exceptions;

public final class MultipartRequestParsingException extends Exception {
	public MultipartRequestParsingException() {
	}

	public MultipartRequestParsingException(String msg) {
		super(msg);
	}

	public MultipartRequestParsingException(Throwable cause) {
		super(cause);
	}

	public MultipartRequestParsingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
