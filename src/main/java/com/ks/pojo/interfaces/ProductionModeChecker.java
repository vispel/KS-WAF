package com.ks.pojo.interfaces;

import com.ks.exceptions.ProductionModeCheckingException;

public  interface ProductionModeChecker
  extends Configurable
{
  boolean isProductionMode()
    throws ProductionModeCheckingException;
}
