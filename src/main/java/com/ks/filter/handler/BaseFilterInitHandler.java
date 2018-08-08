package com.ks.filter.handler;

import com.ks.filter.FilterInitData;

public class BaseFilterInitHandler  extends FilterInitHandler{


    @Override
    public boolean check(FilterInitData filterInitData) {
        return false;
    }

    @Override
    public void init(FilterInitData filterInitData) {

    }
}
