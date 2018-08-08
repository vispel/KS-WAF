package com.ks.filter.handler;

import com.ks.config.ConfigurationManager;
import com.ks.filter.FilterInitData;
import org.apache.commons.lang3.StringUtils;

public class FlagFilterInitHandler extends FilterInitHandler{

    private FilterInitData flagFilterInitData;

    @Override
    public FilterInitData check() {
        this.flagFilterInitData = super.initData;
        ConfigurationManager configurationManager = flagFilterInitData.getConfigurationManager();
        String paramName = flagFilterInitData.getParamName();
        String defaultValue = flagFilterInitData.getDefaultValue();
        if(!StringUtils.isEmpty(defaultValue) && (Boolean.valueOf(defaultValue) != null || defaultValue.equalsIgnoreCase("false"))) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            flagFilterInitData.setParamObject(paramValue == null ? Boolean.valueOf(defaultValue) : Boolean.valueOf(paramValue));
            return flagFilterInitData;
        }
        return checkNext();
    }
}
