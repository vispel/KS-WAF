package com.ks.exceptions;

public final class IllegalRuleDefinitionFormatException extends RuleLoadingException {
	public IllegalRuleDefinitionFormatException() {
	}

	public IllegalRuleDefinitionFormatException(String msg) {
		super(msg);
	}

	public IllegalRuleDefinitionFormatException(Throwable cause) {
		super(cause);
	}

	public IllegalRuleDefinitionFormatException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

