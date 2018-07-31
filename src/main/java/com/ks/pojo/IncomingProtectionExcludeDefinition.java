package com.ks.pojo;
import com.ks.pojo.interfaces.CustomRequestMatcher;

import java.util.regex.Pattern;

public final class IncomingProtectionExcludeDefinition extends RequestDefinition {
	private static final long serialVersionUID = 1L;
	private boolean excludeForceEntranceProtection;
	private boolean excludeParameterAndFormProtection;
	private boolean excludeSelectboxFieldProtection;

	public IncomingProtectionExcludeDefinition(boolean enabled, String identification, String description, WordDictionary servletPathPrefilter, Pattern servletPathPattern, boolean servletPathPatternNegated) {
		super(enabled, identification, description, servletPathPrefilter, servletPathPattern, servletPathPatternNegated);
	}

	public IncomingProtectionExcludeDefinition(boolean enabled, String identification, String description, CustomRequestMatcher customRequestMatcher) {
		super(enabled, identification, description, customRequestMatcher);
	}


	public boolean isExcludeForceEntranceProtection() {
		return this.excludeForceEntranceProtection;
	}

	public void setExcludeForceEntranceProtection(boolean excludeForceEntranceProtection) {
		this.excludeForceEntranceProtection = excludeForceEntranceProtection;
	}

	public boolean isExcludeParameterAndFormProtection() {
		return this.excludeParameterAndFormProtection;
	}

	public void setExcludeParameterAndFormProtection(boolean excludeParameterAndFormProtection) {
		this.excludeParameterAndFormProtection = excludeParameterAndFormProtection;
	}

	public boolean isExcludeSelectboxFieldProtection() {
		return this.excludeSelectboxFieldProtection;
	}

	public void setExcludeSelectboxFieldProtection(boolean excludeSelectboxFieldProtection) {
		this.excludeSelectboxFieldProtection = excludeSelectboxFieldProtection;
	}

	public boolean isExcludeCheckboxFieldProtection() {
		return this.excludeCheckboxFieldProtection;
	}

	public void setExcludeCheckboxFieldProtection(boolean excludeCheckboxFieldProtection) {
		this.excludeCheckboxFieldProtection = excludeCheckboxFieldProtection;
	}

	public boolean isExcludeRadiobuttonFieldProtection() {
		return this.excludeRadiobuttonFieldProtection;
	}

	public void setExcludeRadiobuttonFieldProtection(boolean excludeRadiobuttonFieldProtection) {
		this.excludeRadiobuttonFieldProtection = excludeRadiobuttonFieldProtection;
	}

	private boolean excludeCheckboxFieldProtection;
	private boolean excludeRadiobuttonFieldProtection;
	private boolean excludeReferrerProtection;
	private boolean excludeSecretTokenProtection;
	private boolean excludeSessionToHeaderBindingProtection;

	public boolean isExcludeReferrerProtection() {
		return this.excludeReferrerProtection;
	}

	public void setExcludeReferrerProtection(boolean excludeReferrerProtection) {
		this.excludeReferrerProtection = excludeReferrerProtection;
	}

	public boolean isExcludeSecretTokenProtection() {
		return this.excludeSecretTokenProtection;
	}

	public void setExcludeSecretTokenProtection(boolean excludeSecretTokenProtection) {
		this.excludeSecretTokenProtection = excludeSecretTokenProtection;
	}


	public boolean isExcludeSessionToHeaderBindingProtection() {
		return this.excludeSessionToHeaderBindingProtection;
	}

	public void setExcludeSessionToHeaderBindingProtection(boolean excludeSessionToHeaderBindingProtection) {
		this.excludeSessionToHeaderBindingProtection = excludeSessionToHeaderBindingProtection;
	}
}


