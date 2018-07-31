package com.ks.parser;

import com.ks.exceptions.MultipartRequestParsingException;
import com.ks.pojo.interfaces.Configurable;

import javax.servlet.http.HttpServletRequest;

public interface MultipartRequestParser
  extends Configurable
{
  boolean isMultipartRequest(HttpServletRequest paramHttpServletRequest);
  
  ParsedMultipartRequest parse(HttpServletRequest paramHttpServletRequest, int paramInt, boolean paramBoolean)
    throws MultipartRequestParsingException;
}

