package com.ks.pojo;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Permutation implements Serializable {
	private static final long serialVersionUID = 1L;
	private boolean sealed = false;


	private Set standardPermutations = new HashSet();
	private Set nonStandardPermutations = new HashSet();


	public void addStandardPermutation(String permutation) {
		if (!this.nonStandardPermutations.isEmpty())
			throw new IllegalStateException("Already additional non-standard permutations added, so no standard permutation is allowed");
		this.standardPermutations.add(permutation);
	}

	public void setNonStandardPermutations(Set additionalExtremePermutations) {
		this.nonStandardPermutations = additionalExtremePermutations;
	}

	public void addNonStandardPermutation(String permutation) {
		if (!this.standardPermutations.contains(permutation)) this.nonStandardPermutations.add(permutation);
	}

	public int size() {
		return this.standardPermutations.size() + this.nonStandardPermutations.size();
	}

	public void seal() {
		this.sealed = true;
		this.standardPermutations = Collections.unmodifiableSet(this.standardPermutations);
		this.nonStandardPermutations = Collections.unmodifiableSet(this.nonStandardPermutations);
	}

	public Set getStandardPermutations() {
		if (!this.sealed) return new HashSet(this.standardPermutations);
		return this.standardPermutations;
	}

	public Set getNonStandardPermutations() {
		if (!this.sealed) return new HashSet(this.nonStandardPermutations);
		return this.nonStandardPermutations;
	}


	public String toString() {
		return "SP: " + this.standardPermutations + "\nNSP: " + this.nonStandardPermutations;
	}
}


