package com.ks.wrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class SessionWrapperTransientBox
		implements Serializable {
	private static final long serialVersionUID = 1L;
	private final transient SessionWrapper sessionWrapper;

	public SessionWrapperTransientBox() {
		this(null);
	}

	public SessionWrapperTransientBox(SessionWrapper sessionWrapper) {
		this.sessionWrapper = sessionWrapper;
	}

	public SessionWrapper getSessionWrapper() {
		return this.sessionWrapper;
	}

	private void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}


	public String toString() {
		return "SWTB";
	}
}


