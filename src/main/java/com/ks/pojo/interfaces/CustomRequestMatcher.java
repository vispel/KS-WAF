package com.ks.pojo.interfaces;

import com.ks.exceptions.CustomRequestMatchingException;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

public interface CustomRequestMatcher
{
  void setCustomRequestMatcherProperties(Properties paramProperties)
    throws CustomRequestMatchingException;
  
  boolean isRequestMatching(HttpServletRequest paramHttpServletRequest, String paramString1, String paramString2)
    throws CustomRequestMatchingException;
}

