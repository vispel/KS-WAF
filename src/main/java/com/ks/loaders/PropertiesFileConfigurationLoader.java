package com.ks.loaders;

import com.ks.exceptions.FilterConfigurationException;
import com.ks.utils.ConfigurationUtils;

import javax.servlet.FilterConfig;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesFileConfigurationLoader implements ConfigurationLoader{

	private static final String PARAM_PROPERTIES_FILE = "PropertiesFileConfigurationLoader_File";
	private static final String PARAM_PROPERTIES_FALLBACK = "PropertiesFileConfigurationLoader_FallbackToWebXml";
	private static final boolean DEBUG = false;
	private Properties properties;
	private String filename;
	private FilterConfig filterConfig;

	public void setFilterConfig(FilterConfig filterConfig)
			throws FilterConfigurationException
	{
		this.filename = ConfigurationUtils.extractMandatoryConfigValue(filterConfig, "PropertiesFileConfigurationLoader_File");
		this.properties = new Properties();
        boolean useWebXmlAsFallback;
        try (InputStream input = new BufferedInputStream(new FileInputStream(this.filename))) {
            this.properties.load(input);
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
            useWebXmlAsFallback = "true".equals(ConfigurationUtils.extractOptionalConfigValue(filterConfig, "PropertiesFileConfigurationLoader_FallbackToWebXml", "true").toLowerCase());
        } catch (IOException e) {
            throw new FilterConfigurationException("Unable to load properties file: " + this.filename, e);
        }
		this.filterConfig = (useWebXmlAsFallback ? filterConfig : null);
	}

	public String getConfigurationValue(String key)
	{
		String result = this.properties.getProperty(key);
		if ((this.filterConfig != null) && (result == null)) {
			result = this.filterConfig.getInitParameter(key);
		}
		return result;
	}

	public String toString()
	{
		return "properties file " + this.filename;
	}
}
