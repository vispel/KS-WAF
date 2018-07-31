package com.ks.adapters;

import java.util.Iterator;

public final class IteratorEnumerationAdapter implements java.util.Enumeration {
	private final Iterator iterator;

	public IteratorEnumerationAdapter(Iterator iter) {
		this.iterator = iter;
	}

	public boolean hasMoreElements() {
		return this.iterator.hasNext();
	}

	public Object nextElement() {
		return this.iterator.next();
	}
}

