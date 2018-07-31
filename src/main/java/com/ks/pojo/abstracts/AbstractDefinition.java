package com.ks.pojo.abstracts;

import java.io.Serializable;
import java.util.Objects;

public abstract class AbstractDefinition implements Serializable, Comparable {
	private static final long serialVersionUID = 1L;
	protected final boolean enabled;
	protected final String identification;
	protected final String description;

	public AbstractDefinition(boolean enabled, String identification, String description) {
		if (identification == null) throw new IllegalArgumentException("identification must not be null");
		if (description == null) throw new IllegalArgumentException("description must not be null");
		this.enabled = enabled;
		this.identification = identification;
		this.description = description;
	}


	public final boolean isEnabled() {
		return this.enabled;
	}

	public final String getIdentification() {
		return this.identification;
	}

	public final String getDescription() {
		return this.description;
	}


	public final String toString() {
        String result = "Definition:" + " identification=" + this.identification +
                " description=" + this.description;
        return result;
	}

	public final int hashCode() {
		int hash = 7;
		hash = 31 * hash + (this.identification != null ? this.identification.hashCode() : 0);
		return hash;
	}

	public final boolean equals(Object obj) {
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != getClass())) return false;
		AbstractDefinition other = (AbstractDefinition) obj;

		return (Objects.equals(this.identification, other.identification));
	}


	public final int compareTo(Object obj) {
		AbstractDefinition other = (AbstractDefinition) obj;
		String identLeft = this.identification;
		String identRight = other.identification;
		if (identLeft != null) return identRight == null ? -1 : identLeft.compareTo(identRight);
		return identRight == null ? 0 : 1;
	}
}
