package com.ks.pojo;

import java.io.Serializable;
import java.util.Properties;

public class RuleFile implements Serializable {
	private final String name;
	private final Properties properties;

	public RuleFile(String name, Properties properties)
	{
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		if (properties == null) {
			throw new IllegalArgumentException("properties must not be null");
		}
		this.name = name;
		this.properties = properties;
	}

	public String getName()
	{
		return this.name;
	}

	public Properties getProperties()
	{
		return this.properties;
	}

	public String toString()
	{
		return this.name;
	}
}
