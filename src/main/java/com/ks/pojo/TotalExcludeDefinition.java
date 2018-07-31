package com.ks.pojo;

import java.util.regex.Pattern;

public final class TotalExcludeDefinition extends SimpleDefinition {
	private static final long serialVersionUID = 1L;

	public TotalExcludeDefinition(boolean enabled, String identification, String description, WordDictionary servletPathOrRequestURIPrefilter, Pattern servletPathOrRequestURIPattern) {
		super(enabled, identification, description, servletPathOrRequestURIPrefilter, servletPathOrRequestURIPattern);
	}
}
