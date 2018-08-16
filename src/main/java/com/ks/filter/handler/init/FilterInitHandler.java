package com.ks.filter.handler.init;

import com.ks.filter.handler.FilterLocalLog;

import javax.servlet.FilterConfig;
import javax.servlet.UnavailableException;

public abstract class FilterInitHandler extends FilterLocalLog {

    private FilterConfig filterConfig;
    protected boolean isDebug;

    public FilterInitHandler(FilterConfig filterConfig, boolean isDebug) {
        super(filterConfig,isDebug);
    }

    public abstract Object init(String paramName, Object paramObject) throws UnavailableException;

}
