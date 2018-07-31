package com.ks.loaders;

public abstract class AbstractRuleFileLoader {

	protected String path;

	public final void setPath(String path)
	{
		if (path == null) {
			throw new IllegalArgumentException("Path must not be null");
		}
		this.path = path.trim();
	}

	public final String getPath()
	{
		return this.path;
	}
}
