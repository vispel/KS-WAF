package com.ks;

import com.ks.exceptions.FilterConfigurationException;
import com.ks.loaders.ConfigurationLoader;

import javax.servlet.FilterConfig;

public class DefaultConfigurationLoader implements ConfigurationLoader {

	private static final boolean DEBUG = false;
	private FilterConfig filterConfig;

	public void setFilterConfig(FilterConfig filterConfig)
			throws FilterConfigurationException
	{
		this.filterConfig = filterConfig;
	}

	public String getConfigurationValue(String key)
	{
		if (this.filterConfig == null) {
			throw new IllegalStateException("filterConfig must be set before fetching configuration values");
		}
		return this.filterConfig.getInitParameter(key);
	}

	public String toString()
	{
		return "web.xml filter init parameters";
	}


}
