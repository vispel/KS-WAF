package com.ks.pojo.interfaces;

import com.ks.exceptions.FilterConfigurationException;

import javax.servlet.FilterConfig;

public interface Configurable {

	void setFilterConfig(FilterConfig paramFilterConfig)
				throws FilterConfigurationException;
}
