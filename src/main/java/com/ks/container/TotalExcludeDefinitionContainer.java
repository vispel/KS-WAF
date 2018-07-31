package com.ks.container;

import com.ks.exceptions.IllegalRuleDefinitionFormatException;
import com.ks.loaders.RuleFileLoader;
import com.ks.pojo.SimpleDefinition;
import com.ks.pojo.TotalExcludeDefinition;
import com.ks.pojo.WordDictionary;

import java.util.Properties;
import java.util.regex.Pattern;

public final class TotalExcludeDefinitionContainer extends SimpleDefinitionContainer {
	public TotalExcludeDefinitionContainer(RuleFileLoader ruleFileLoader) {
		super(ruleFileLoader);
	}

	protected SimpleDefinition doCreateSimpleDefinition(boolean enabled, String name, String description, WordDictionary servletPathOrRequestURIPrefilter, Pattern servletPathOrRequestURIPattern) {
		return new TotalExcludeDefinition(enabled, name, description, servletPathOrRequestURIPrefilter, servletPathOrRequestURIPattern);
	}


	protected void doParseSimpleDefinitionDetailsAndRemoveKeys(SimpleDefinition definition, Properties properties)
			throws java.util.regex.PatternSyntaxException, IllegalRuleDefinitionFormatException {
	}

	public final boolean isTotalExclude(String servletPath, String requestURI) {
		return getMatchingSimpleDefinition(servletPath, requestURI) != null;
	}
}

