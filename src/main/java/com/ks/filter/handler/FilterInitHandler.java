package com.ks.filter.handler;

import com.ks.filter.FilterInitData;

public abstract class FilterInitHandler {

    private FilterInitHandler nextFilterInitHandler;
    protected FilterInitData initData;

    public FilterInitHandler linkWith(FilterInitHandler nextFilterInitHandler){
        this.nextFilterInitHandler = nextFilterInitHandler;
        return this.nextFilterInitHandler;
    }

    public FilterInitData checkNext(){
        return nextFilterInitHandler == null ? this.initData : check();
    }

    public abstract FilterInitData check();

    public void initFilterData(FilterInitData filterInitData){
        this.initData = filterInitData;
    }

    protected void logLocal(final String msg) {
        logLocal(msg, null);
    }

    protected void logLocal(final String msg, final Exception e) {
        if (e != null) {
            if (filterConfig != null && filterConfig.getServletContext() != null) filterConfig.getServletContext().log(msg, e);
            else {
                System.out.println(msg+": "+e);
            }
        } else {
            if (filterConfig != null && filterConfig.getServletContext() != null) filterConfig.getServletContext().log(msg);
            else {
                System.out.println(msg);
            }
        }
    }


}
