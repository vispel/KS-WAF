package com.ks.pojo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class Snapshot implements Serializable {

	private final String type;
	private final String systemIdentifier;
	private Map payload;
	private List removals;
	private long removalTimestamp;

	private Snapshot(String type, String systemIdentifier) {
		this.type = type;
		this.systemIdentifier = systemIdentifier;
	}

	public Snapshot(String type, String systemIdentifier, Map payload) {
		this(type, systemIdentifier);
		this.payload = payload;
	}

	public Snapshot(String type, String systemIdentifier, List removals) {
		this(type, systemIdentifier);
		this.removals = removals;
		this.removalTimestamp = System.currentTimeMillis();
	}


	public Map getPayload() {
		return this.payload;
	}


	public List getRemovals() {
		return this.removals;
	}

	public boolean hasPayload() {
		return (this.payload != null) && (!this.payload.isEmpty());
	}

	public boolean hasRemovals() {
		return (this.removals != null) && (!this.removals.isEmpty());
	}

	public long getRemovalTimestamp() {
		return this.removalTimestamp;
	}

	public boolean isEmpty() {
		return (!hasPayload()) && (!hasRemovals());
	}

	public String getSystemIdentifier() {
		return this.systemIdentifier;
	}

	public String getType() {
		return this.type;
	}
}


