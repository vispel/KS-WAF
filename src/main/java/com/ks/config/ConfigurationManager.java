package com.ks.config;

import com.ks.exceptions.FilterConfigurationException;
import com.ks.loaders.ConfigurationLoader;

import javax.servlet.FilterConfig;

public final class ConfigurationManager {

	private static final String PARAM_CONFIGURATION_LOADER = "ConfigurationLoader";
	private final ConfigurationLoader loader;

	public ConfigurationManager(FilterConfig filterConfig)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, FilterConfigurationException
	{
		this(filterConfig, filterConfig.getInitParameter("ConfigurationLoader") == null ? "com.ks.DefaultConfigurationLoader" : filterConfig.getInitParameter("ConfigurationLoader"));
	}

	public ConfigurationManager(FilterConfig filterConfig, String configurationLoaderClassName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, FilterConfigurationException
	{
		this(filterConfig, Class.forName(configurationLoaderClassName));
	}

	public ConfigurationManager(FilterConfig filterConfig, Class configurationLoaderClass)
			throws InstantiationException, IllegalAccessException, FilterConfigurationException
	{
		this(filterConfig, (ConfigurationLoader)configurationLoaderClass.newInstance());
	}

	public ConfigurationManager(FilterConfig filterConfig, ConfigurationLoader configurationLoader)
			throws FilterConfigurationException
	{
		if (configurationLoader == null) {
			throw new IllegalArgumentException("configurationLoader must not be null");
		}
		this.loader = configurationLoader;
		this.loader.setFilterConfig(filterConfig);
	}

	public String getConfigurationValue(String key)
	{
		return this.loader.getConfigurationValue(key);
	}

	public String toString()
	{
		return "Loading configuration via " + this.loader;
	}
}
