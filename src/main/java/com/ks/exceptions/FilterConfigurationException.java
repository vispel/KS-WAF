package com.ks.exceptions;

public class FilterConfigurationException extends Exception {

	public FilterConfigurationException() {}

	public FilterConfigurationException(String msg)
	{
		super(msg);
	}

	public FilterConfigurationException(Throwable cause)
	{
		super(cause);
	}

	public FilterConfigurationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
