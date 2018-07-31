package com.ks.exceptions;

public final class ClientIpDeterminationException extends Exception {
	public ClientIpDeterminationException() {
	}

	public ClientIpDeterminationException(String msg) {
		super(msg);
	}

	public ClientIpDeterminationException(Throwable cause) {
		super(cause);
	}

	public ClientIpDeterminationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

