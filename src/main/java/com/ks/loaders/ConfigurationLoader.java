package com.ks.loaders;

import com.ks.pojo.interfaces.Configurable;

public  interface ConfigurationLoader
		extends Configurable
{
	String getConfigurationValue(String paramString);
}