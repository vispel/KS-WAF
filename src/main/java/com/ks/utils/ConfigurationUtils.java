package com.ks.utils;

import com.ks.config.ConfigurationManager;
import com.ks.exceptions.FilterConfigurationException;

import javax.servlet.FilterConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigurationUtils {

	public static final String extractMandatoryConfigValue(ConfigurationManager configurationManager, String key)
			throws FilterConfigurationException
	{
		return extractMandatoryConfigValue(configurationManager, key, null);
	}

	public static final String extractMandatoryConfigValue(ConfigurationManager configurationManager, String key, Pattern syntaxPattern)
			throws FilterConfigurationException
	{
		if (configurationManager == null) {
			throw new IllegalArgumentException("configurationManager must not be null");
		}
		String value = configurationManager.getConfigurationValue(key);
		if (value == null) {
			throw new FilterConfigurationException("Missing mandatory filter init-param: " + key);
		}
		value = value.trim();
		checkSyntax(key, syntaxPattern, value);
		return value;
	}

	public static final String extractOptionalConfigValue(ConfigurationManager configurationManager, String key, String defaultValue)
			throws FilterConfigurationException
	{
		return extractOptionalConfigValue(configurationManager, key, defaultValue, null);
	}

	public static final String extractOptionalConfigValue(ConfigurationManager configurationManager, String key, String defaultValue, Pattern syntaxPattern)
			throws FilterConfigurationException
	{
		if (configurationManager == null) {
			throw new IllegalArgumentException("configurationManager must not be null");
		}
		String value = configurationManager.getConfigurationValue(key);
		if (value == null) {
			value = defaultValue;
		}
		if (value != null) {
			value = value.trim();
		}
		checkSyntax(key, syntaxPattern, value);
		return value;
	}

	public static final String extractMandatoryConfigValue(FilterConfig filterConfig, String key)
			throws FilterConfigurationException
	{
		return extractMandatoryConfigValue(filterConfig, key, null);
	}

	public static final String extractMandatoryConfigValue(FilterConfig filterConfig, String key, Pattern syntaxPattern)
			throws FilterConfigurationException
	{
		if (filterConfig == null) {
			throw new IllegalArgumentException("filterConfig must not be null");
		}
		String value = filterConfig.getInitParameter(key);
		if (value == null) {
			throw new FilterConfigurationException("Missing mandatory filter init-param: " + key);
		}
		value = value.trim();
		checkSyntax(key, syntaxPattern, value);
		return value;
	}

	public static final String extractOptionalConfigValue(FilterConfig filterConfig, String key, String defaultValue)
			throws FilterConfigurationException
	{
		return extractOptionalConfigValue(filterConfig, key, defaultValue, null);
	}

	public static final String extractOptionalConfigValue(FilterConfig filterConfig, String key, String defaultValue, Pattern syntaxPattern)
			throws FilterConfigurationException
	{
		if (filterConfig == null) {
			throw new IllegalArgumentException("filterConfig must not be null");
		}
		String value = filterConfig.getInitParameter(key);
		if (value == null) {
			value = defaultValue;
		}
		if (value != null) {
			value = value.trim();
		}
		checkSyntax(key, syntaxPattern, value);
		return value;
	}

	public static void checkSyntax(String key, Pattern syntaxPattern, String value)
			throws FilterConfigurationException
	{
		if (syntaxPattern != null)
		{
			Matcher matcher = syntaxPattern.matcher(value);
			if (!matcher.matches()) {
				throw new FilterConfigurationException("Filter init-param does not validate against syntax pattern (" + syntaxPattern + "): " + key);
			}
		}
	}

	public static ConfigurationManager createConfigurationManager(FilterConfig filterConfig)
			throws FilterConfigurationException
	{
		ConfigurationManager configManager;
		try
		{
			configManager = new ConfigurationManager(filterConfig);
		}
		catch (ClassNotFoundException | InstantiationException e)
		{
			throw new FilterConfigurationException(e);
		} catch (IllegalAccessException e)
		{
			throw new FilterConfigurationException(e);
		}
		catch (FilterConfigurationException e)
		{
			throw new FilterConfigurationException(e);
		}
		catch (RuntimeException e)
		{
			throw new FilterConfigurationException(e);
		}
		assert (configManager != null);
		return configManager;
	}

}
