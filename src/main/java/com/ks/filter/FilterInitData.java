package com.ks.filter;

import com.ks.config.ConfigurationManager;

import javax.servlet.FilterConfig;

public class FilterInitData {

    private ConfigurationManager configurationManager;
    private FilterConfig filterConfig;
    private String paramName;
    private String defaultValue;
    private Object paramObject;


    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getParamObject() {
        return paramObject;
    }

    public void setParamObject(Object paramObject) {
        this.paramObject = paramObject;
    }
}
