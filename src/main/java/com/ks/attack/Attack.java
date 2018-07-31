package com.ks.attack;

import java.io.Serializable;

public class Attack implements Serializable {

	private final String message;
	private String logReferenceId;

	public Attack(String message)
	{
		if (message == null) {
			throw new IllegalArgumentException("Message must not be null");
		}
		this.message = message;
	}

	public Attack(String message, String logReferenceId)
	{
		this(message);
		this.logReferenceId = logReferenceId;
	}

	public String getLogReferenceId()
	{
		return this.logReferenceId;
	}

	public String getMessage()
	{
		return this.message;
	}

	public String toString()
	{
		return this.message;
	}
}
