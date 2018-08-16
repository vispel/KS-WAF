package com.ks;

import com.ks.config.ConfigurationManager;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.exceptions.ProductionModeCheckingException;
import com.ks.pojo.interfaces.ProductionModeChecker;
import com.ks.utils.ConfigurationUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.FilterConfig;

public final class DefaultProductionModeChecker implements ProductionModeChecker {

    public static final String PARAM_PRODUCTION_MODE = "ProductionMode";
    
    private boolean isProductionMode = true;
    
    
    public void setFilterConfig(final FilterConfig filterConfig) throws FilterConfigurationException { // TODO: use  ConfigurationUtils.extractOptionalConfigValue
        if (filterConfig == null) throw new NullPointerException("filterConfig must not be null");
        final ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
        String value = configManager.getConfigurationValue(PARAM_PRODUCTION_MODE);
        if (StringUtils.isEmpty(value)) value = "true";
        this.isProductionMode = StringUtils.isEmpty(value)? Boolean.TRUE : Boolean.valueOf(value.trim());
    }

    public boolean isProductionMode() throws ProductionModeCheckingException {
        return this.isProductionMode;
    }

    
}
