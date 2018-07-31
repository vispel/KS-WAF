package com.ks.container;

import com.ks.exceptions.IllegalRuleDefinitionFormatException;
import com.ks.exceptions.RuleLoadingException;
import com.ks.loaders.RuleFileLoader;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;


public abstract class AbstractDefinitionContainer
		implements Serializable {
	private static final long serialVersionUID = 1L;
	protected static final String KEY_DESCRIPTION = "description";
	protected static final String KEY_ENABLED = "enabled";
	protected final RuleFileLoader ruleFileLoader;
	protected SortedSet definitions = new TreeSet();


	protected boolean hasEnabledDefinitions = false;


	public AbstractDefinitionContainer(RuleFileLoader ruleFileLoader) {
		if (ruleFileLoader == null) throw new IllegalArgumentException("ruleFileLoader must not be null");
		this.ruleFileLoader = ruleFileLoader;
	}


	public abstract String parseDefinitions()
			throws RuleLoadingException, IllegalRuleDefinitionFormatException;


	public final boolean hasEnabledDefinitions() {
		return this.hasEnabledDefinitions;
	}
}


