package com.ks.filter.handler.init;

import com.ks.config.ConfigurationManager;

import javax.servlet.FilterConfig;
import javax.servlet.UnavailableException;

public class BaseFilterInitHandler extends FilterInitHandler {

    private ConfigurationManager configurationManager;

    public BaseFilterInitHandler(FilterConfig filterConfig, ConfigurationManager configurationManager, boolean isDebug) {
        super(filterConfig,isDebug);
        this.configurationManager = configurationManager;
    }

    public Object init(String paramName, Object paramObject) throws UnavailableException {
        //set flags
        paramObject = setBooleans(paramName, paramObject);
        //set Strings
        paramObject = setStrings(paramName, paramObject);
        //set Nnumbers
        if(paramObject instanceof Number) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            if(paramValue != null) {
                try {
                    paramObject = isLong(paramName, paramObject, paramValue);
                    paramObject = isInteger(paramName, paramObject, paramValue);
                    paramObject = isShort(paramName, paramObject, paramValue);
                } catch(NumberFormatException e) {
                    throw new UnavailableException("Unable to number-parse configured "+ paramName + ": " + paramValue);
                }
            }
            if(isDebug) logLocal(paramName, paramObject);
        }
        /*//set classes
        if(paramObject instanceof Class) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            paramValue = StringUtils.isEmpty(paramValue)? defaultValue : paramValue;
            try {
                paramObject = Class.forName(paramValue);
            } catch (ClassNotFoundException e) {
                throw new UnavailableException("Unable to find" + paramName + " class ("+paramValue+"): "+e.getMessage());
            }
            if(super.isDebug) super.logLocal(paramName, paramObject);
        }*/
        return paramObject;
    }

    private Object setStrings(String paramName, Object paramObject) {
        if(paramObject instanceof String) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            if(paramValue != null) {
                paramObject = paramValue;
            }
            if(isDebug) logLocal(paramName, paramObject);
        }
        return paramObject;
    }

    private Object setBooleans(String paramName, Object paramObject) {
        if(paramObject instanceof Boolean) {
            String paramValue = configurationManager.getConfigurationValue(paramName);
            if(paramValue != null) {
                paramObject = Boolean.valueOf(paramValue);
            }
            if(isDebug) logLocal(paramName, paramObject);
        }
        return paramObject;
    }

    private Object isShort(String paramName, Object paramObject, String paramValue) throws UnavailableException {
        if(paramObject instanceof Short) {
            paramObject = Short.parseShort(paramValue);
            if ((Short) paramObject < 0) throw new UnavailableException("Configured" + paramName +" must not be negative: " + paramValue);
        }
        return paramObject;
    }

    private Object isInteger(String paramName, Object paramObject, String paramValue) throws UnavailableException {
        if(paramObject instanceof Integer) {
            paramObject = Integer.parseInt(paramValue);
            if ((Integer) paramObject < 0) throw new UnavailableException("Configured" + paramName +" must not be negative: " + paramValue);
        }
        return paramObject;
    }

    private Object isLong(String paramName, Object paramObject, String paramValue) throws UnavailableException {
        if(paramObject instanceof Long) {
            paramObject = Long.parseLong(paramValue);
            if ((Long) paramObject < 0) throw new UnavailableException("Configured" + paramName +" must not be negative: " + paramValue);
        }
        return paramObject;
    }

}
