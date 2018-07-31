package com.ks.pojo;

import com.ks.pojo.interfaces.CustomRequestMatcher;

import java.util.regex.Pattern;

public final class RenewSessionAndTokenPointDefinition extends RequestDefinition {
	private static final long serialVersionUID = 1L;
	private boolean renewSession;
	private boolean renewSecretToken;
	private boolean renewParamAndFormToken;
	private boolean renewCryptoKey;

	public RenewSessionAndTokenPointDefinition(boolean enabled, String identification, String description, WordDictionary servletPathPrefilter, Pattern servletPathPattern, boolean servletPathPatternNegated) {
		super(enabled, identification, description, servletPathPrefilter, servletPathPattern, servletPathPatternNegated);
	}

	public RenewSessionAndTokenPointDefinition(boolean enabled, String identification, String description, CustomRequestMatcher customRequestMatcher) {
		super(enabled, identification, description, customRequestMatcher);
	}


	public boolean isRenewSession() {
		return this.renewSession;
	}

	public void setRenewSession(boolean renewSession) {
		this.renewSession = renewSession;
	}

	public boolean isRenewSecretToken() {
		return this.renewSecretToken;
	}

	public void setRenewSecretToken(boolean renewSecretToken) {
		this.renewSecretToken = renewSecretToken;
	}

	public boolean isRenewParamAndFormToken() {
		return this.renewParamAndFormToken;
	}

	public void setRenewParamAndFormToken(boolean renewParamAndFormToken) {
		this.renewParamAndFormToken = renewParamAndFormToken;
	}

	public boolean isRenewCryptoKey() {
		return this.renewCryptoKey;
	}

	public void setRenewCryptoKey(boolean renewCryptoKey) {
		this.renewCryptoKey = renewCryptoKey;
	}
}


