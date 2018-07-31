package com.ks.pojo;

import com.ks.pojo.interfaces.CustomRequestMatcher;

import java.util.List;
import java.util.regex.Pattern;

public final class ResponseModificationDefinition extends RequestDefinition {
	private static final long serialVersionUID = 1L;

	private boolean matchesScripts, matchesTags;
	private WordDictionary urlCapturingPrefilter, urlExclusionPrefilter, tagExclusionPrefilter, scriptExclusionPrefilter;
	private Pattern urlCapturingPattern, urlExclusionPattern, tagExclusionPattern, scriptExclusionPattern;
	private int[] capturingGroupNumbers;

	public ResponseModificationDefinition(final boolean enabled, final String identification, final String description,    final WordDictionary servletPathPrefilter, final Pattern servletPathPattern, final boolean servletPathPatternNegated) {
		super(enabled, identification, description, servletPathPrefilter, servletPathPattern, servletPathPatternNegated);
	}
	public ResponseModificationDefinition(final boolean enabled, final String identification, final String description,    final CustomRequestMatcher customRequestMatcher) {
		super(enabled, identification, description, customRequestMatcher);
	}

	public boolean isMatchesScripts() {
		return matchesScripts;
	}
	public void setMatchesScripts(boolean matchesScripts) {
		this.matchesScripts = matchesScripts;
	}

	public boolean isMatchesTags() {
		return matchesTags;
	}
	public void setMatchesTags(boolean matchesTags) {
		this.matchesTags = matchesTags;
	}


	public WordDictionary getScriptExclusionPrefilter() {
		return scriptExclusionPrefilter;
	}

	public void setScriptExclusionPrefilter(WordDictionary scriptExclusionPrefilter) {
		this.scriptExclusionPrefilter = scriptExclusionPrefilter;
	}

	public WordDictionary getTagExclusionPrefilter() {
		return tagExclusionPrefilter;
	}

	public void setTagExclusionPrefilter(WordDictionary tagExclusionPrefilter) {
		this.tagExclusionPrefilter = tagExclusionPrefilter;
	}

	public WordDictionary getUrlCapturingPrefilter() {
		return urlCapturingPrefilter;
	}

	public void setUrlCapturingPrefilter(WordDictionary urlCapturingPrefilter) {
		this.urlCapturingPrefilter = urlCapturingPrefilter;
	}

	public WordDictionary getUrlExclusionPrefilter() {
		return urlExclusionPrefilter;
	}

	public void setUrlExclusionPrefilter(WordDictionary urlExclusionPrefilter) {
		this.urlExclusionPrefilter = urlExclusionPrefilter;
	}

	public Pattern getUrlExclusionPattern() {
		return urlExclusionPattern;
	}
	public void setUrlExclusionPattern(Pattern urlExclusionPattern) {
		this.urlExclusionPattern = urlExclusionPattern;
	}

	public Pattern getScriptExclusionPattern() {
		return scriptExclusionPattern;
	}
	public void setScriptExclusionPattern(Pattern scriptExclusionPattern) {
		this.scriptExclusionPattern = scriptExclusionPattern;
	}

	public Pattern getTagExclusionPattern() {
		return tagExclusionPattern;
	}
	public void setTagExclusionPattern(Pattern tagExclusionPattern) {
		this.tagExclusionPattern = tagExclusionPattern;
	}

	public Pattern getUrlCapturingPattern() {
		return urlCapturingPattern;
	}
	public void setUrlCapturingPattern(Pattern urlCapturingPattern) {
		this.urlCapturingPattern = urlCapturingPattern;
	}

	public int[] getCapturingGroupNumbers() {
		return capturingGroupNumbers;
	}
	public void setCapturingGroupNumbers(final int[] capturingGroupNumbers) {
		this.capturingGroupNumbers = capturingGroupNumbers;
	}
	public void setCapturingGroupNumbers(final List capturingGroupNumbers) {
		if (capturingGroupNumbers == null) {
			this.capturingGroupNumbers = null;
			return;
		}
		this.capturingGroupNumbers = new int[capturingGroupNumbers.size()];
		int i=0;
        for (Object capturingGroupNumber : capturingGroupNumbers) {
            this.capturingGroupNumbers[i++] = (Integer) capturingGroupNumber;
        }
	}


}