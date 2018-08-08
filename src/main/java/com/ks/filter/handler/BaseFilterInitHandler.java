package com.ks.filter.handler;

import com.ks.config.ConfigurationManager;
import com.ks.filter.FilterInitData;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.FilterConfig;
import javax.servlet.UnavailableException;

public class BaseFilterInitHandler extends FilterInitHandler{

    private FilterInitData initData;
    private ConfigurationManager configurationManager;

    public BaseFilterInitHandler(FilterInitData initData, FilterConfig filterConfig, ConfigurationManager configurationManager, boolean isDebug) {
        super(filterConfig,isDebug);
        this.initData = initData;
        this.configurationManager = configurationManager;
    }

    @Override
    public boolean check() throws UnavailableException {
        String paramName = initData.getParamName();
        String defaultValue = initData.getDefaultValue();
        //set flags
        if(initData.getParamObject() instanceof Boolean) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            initData.setParamObject(paramValue == null ? Boolean.valueOf(defaultValue) : Boolean.valueOf(paramValue));
            if(super.isDebug) super.logLocal(paramName, initData.getParamObject());
        }

        if(initData.getParamObject() instanceof String) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            initData.setParamObject(StringUtils.isEmpty(paramValue) ? defaultValue : paramValue);
            if(super.isDebug) super.logLocal(paramName, initData.getParamObject());
        }
        //set classes
        if(initData.getParamObject() instanceof Class) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            paramValue = StringUtils.isEmpty(paramValue)? defaultValue : paramValue;
            try {
                initData.setParamObject(Class.forName(paramValue));
            } catch (ClassNotFoundException e) {
                throw new UnavailableException("Unable to find" + paramName + " class ("+paramValue+"): "+e.getMessage());
            }
            if(super.isDebug) super.logLocal(paramName, initData.getParamObject());
        }


        return checkNext();
    }
}
