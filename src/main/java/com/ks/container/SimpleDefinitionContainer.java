package com.ks.container;

import com.ks.exceptions.IllegalRuleDefinitionFormatException;
import com.ks.exceptions.RuleLoadingException;
import com.ks.loaders.RuleFileLoader;
import com.ks.pojo.RuleFile;
import com.ks.pojo.SimpleDefinition;
import com.ks.pojo.WordDictionary;
import com.ks.utils.ServerUtils;
import com.ks.utils.WordMatchingUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class SimpleDefinitionContainer extends AbstractDefinitionContainer {
	protected static final String KEY_SERVLET_PATH_OR_REQUEST_URI_PATTERN = "servletPathOrRequestURI";
	protected static final String KEY_SERVLET_PATH_OR_REQUEST_URI_PREFILTER = "servletPathOrRequestURI@prefilter";

	public SimpleDefinitionContainer(RuleFileLoader ruleFileLoader) {
		super(ruleFileLoader);
	}


	public final String parseDefinitions()
			throws RuleLoadingException, IllegalRuleDefinitionFormatException {
		RuleFile[] ruleFiles = this.ruleFileLoader.loadRuleFiles();
		String message = "KsWaf loaded " + (ruleFiles.length < 10 ? " " : "") + ruleFiles.length + " security rule" + (ruleFiles.length == 1 ? ":  " : "s: ") + this.ruleFileLoader.getPath() + " (via " + this.ruleFileLoader.getClass().getName() + ")";
		SortedSet newDefinitions = new TreeSet();
		boolean newHasEnabledDefinitions = false;
		for (RuleFile ruleFile : ruleFiles) {
			Properties properties = ruleFile.getProperties();


			boolean enabled = "true".equals(properties.getProperty("enabled", "true").trim().toLowerCase());
			if (enabled) newHasEnabledDefinitions = true;
			String description = properties.getProperty("description");
			if (description == null)
				throw new IllegalRuleDefinitionFormatException("Description property (description) not found in rule file: " + ruleFile);
			String servletPathOrRequestURI = properties.getProperty("servletPathOrRequestURI");
			if (servletPathOrRequestURI == null)
				throw new IllegalRuleDefinitionFormatException("Servlet path or request URI property (servletPathOrRequestURI) not found in rule file: " + ruleFile);
			String prefilter = properties.getProperty("servletPathOrRequestURI@prefilter");
			try {
				Pattern servletPathOrRequestURIPattern = Pattern.compile(servletPathOrRequestURI);
				WordDictionary servletPathOrRequestURIPrefilter = prefilter == null ? null : new WordDictionary(prefilter);

				SimpleDefinition definition = doCreateSimpleDefinition(enabled, ruleFile.getName(), description, servletPathOrRequestURIPrefilter, servletPathOrRequestURIPattern);
				doParseSimpleDefinitionDetailsAndRemoveKeys(definition, properties);


				Set copyOfKeys = properties.keySet();

				copyOfKeys.remove("description");
				copyOfKeys.remove("servletPathOrRequestURI");
				copyOfKeys.remove("servletPathOrRequestURI@prefilter");
				copyOfKeys.remove("enabled");

				if (!copyOfKeys.isEmpty()) {
					throw new IllegalRuleDefinitionFormatException("Unknown keys (" + copyOfKeys + ") found in rule file: " + ruleFile);
				}
				newDefinitions.add(definition);
			} catch (PatternSyntaxException e) {
				throw new IllegalRuleDefinitionFormatException("Invalid regular expression syntax in rule file: " + ruleFile, e);
			}
		}


		this.definitions = newDefinitions;
		this.hasEnabledDefinitions = newHasEnabledDefinitions;
		return message;
	}


	protected abstract void doParseSimpleDefinitionDetailsAndRemoveKeys(SimpleDefinition paramSimpleDefinition, Properties paramProperties)
			throws PatternSyntaxException, IllegalRuleDefinitionFormatException;


	protected abstract SimpleDefinition doCreateSimpleDefinition(boolean paramBoolean, String paramString1, String paramString2, WordDictionary paramWordDictionary, Pattern paramPattern);


	protected final SimpleDefinition[] getAllMatchingSimpleDefinitions(String servletPath, String requestURI) {
		return checkMatchingSimpleDefinitions(false, servletPath, requestURI);
	}

	protected final SimpleDefinition getMatchingSimpleDefinition(String servletPath, String requestURI) {
		SimpleDefinition[] results = checkMatchingSimpleDefinitions(true, servletPath, requestURI);
		if (results.length == 0) return null;
		assert (results.length == 1);
		return results[0];
	}


	private SimpleDefinition[] checkMatchingSimpleDefinitions(boolean returnOnlyTheFirstMatchingDefinition, String servletPath, String requestURI) {
		if (!this.hasEnabledDefinitions) {
			return new SimpleDefinition[0];
		}

		String resourceAccessed = (servletPath != null) && (servletPath.trim().length() > 0) ? servletPath : ServerUtils.decodeBrokenValueUrlEncodingOnly(requestURI);
		if (resourceAccessed == null) return new SimpleDefinition[0];
		resourceAccessed = resourceAccessed.trim();
		if (resourceAccessed.length() == 0) {
			return new SimpleDefinition[0];
		}

		List results = new ArrayList();
		for (Object definition : this.definitions) {
			SimpleDefinition simpleDefinition = (SimpleDefinition) definition;
			if (simpleDefinition.isEnabled()) {


				WordDictionary prefilter = simpleDefinition.getServletPathOrRequestURIPrefilter();
				if ((prefilter == null) ||
						(WordMatchingUtils.matchesWord(prefilter, resourceAccessed, 60))) {

					Pattern servletPathOrRequestURIPattern = simpleDefinition.getServletPathOrRequestURIPattern();
					if (servletPathOrRequestURIPattern.matcher(resourceAccessed).find()) {
						results.add(simpleDefinition);
						if (returnOnlyTheFirstMatchingDefinition)
							return (SimpleDefinition[]) results.toArray(new SimpleDefinition[0]);
					}
				}
			}
		}
		return (SimpleDefinition[]) results.toArray(new SimpleDefinition[0]);
	}
}


