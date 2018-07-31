package com.ks.pojo;

import java.util.regex.Pattern;

public final class ContentModificationExcludeDefinition extends SimpleDefinition {
	private static final long serialVersionUID = 1L;
	private boolean excludeOutgoingResponsesFromModification;
	private boolean excludeIncomingLinksFromModification;
	private boolean excludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled;

	public ContentModificationExcludeDefinition(boolean enabled, String identification, String description, WordDictionary servletPathOrRequestURIPrefilter, Pattern servletPathOrRequestURIPattern) {
		super(enabled, identification, description, servletPathOrRequestURIPrefilter, servletPathOrRequestURIPattern);
	}


	public boolean isExcludeOutgoingResponsesFromModification() {
		return this.excludeOutgoingResponsesFromModification;
	}

	public void setExcludeOutgoingResponsesFromModification(boolean excludeOutgoingResponsesFromModification) {
		this.excludeOutgoingResponsesFromModification = excludeOutgoingResponsesFromModification;
	}

	public boolean isExcludeIncomingLinksFromModification() {
		return this.excludeIncomingLinksFromModification;
	}

	public void setExcludeIncomingLinksFromModification(boolean excludeIncomingLinksFromModification) {
		this.excludeIncomingLinksFromModification = excludeIncomingLinksFromModification;
	}

	public boolean isExcludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled() {
		return this.excludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled;
	}

	public void setExcludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled(boolean excludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled) {
		this.excludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled = excludeIncomingLinksFromModificationEvenWhenFullPathRemovalEnabled;
	}
}


