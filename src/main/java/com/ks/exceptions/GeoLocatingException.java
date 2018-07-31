package com.ks.exceptions;

public class GeoLocatingException extends Exception {
	public GeoLocatingException() {
	}

	public GeoLocatingException(String msg) {
		super(msg);
	}

	public GeoLocatingException(Throwable cause) {
		super(cause);
	}

	public GeoLocatingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}


