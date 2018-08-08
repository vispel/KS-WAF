package com.ks.filter;

public class FilterInitData <T> {

    private String paramName;
    private String defaultValue;
    private T paramObject;


    public FilterInitData(String paramName, String defaultValue, T paramObject) {
        this.paramName = paramName;
        this.defaultValue = defaultValue;
        this.paramObject = paramObject;
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

    public void setParamObject(T paramObject) {
        this.paramObject = paramObject;
    }

}
