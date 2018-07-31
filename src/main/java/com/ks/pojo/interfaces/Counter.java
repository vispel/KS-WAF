package com.ks.pojo.interfaces;

import java.io.Serializable;

public  interface Counter extends Serializable
{
  void increment();
  
  int getCounter();
  
  boolean isOveraged();
  
  long getResetPeriodMillis();
  
  void setResetPeriodMillis(long paramLong);
}
