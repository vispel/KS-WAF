package com.ks.parser;

import com.ks.exceptions.MultipartRequestParsingException;

import java.io.InputStream;

public  interface ParsedMultipartRequest
{
  InputStream replayCapturedInputStream()
    throws MultipartRequestParsingException;
  
  int getElementCount()
    throws MultipartRequestParsingException;
  
  String getFormFieldName(int paramInt)
    throws MultipartRequestParsingException;
  
  String getFormFieldContent(int paramInt)
    throws MultipartRequestParsingException;
  
  String getSubmittedFileName(int paramInt)
    throws MultipartRequestParsingException;
  
  long getSubmittedFileSize(int paramInt)
    throws MultipartRequestParsingException;
  
  String getSubmittedFileContentType(int paramInt)
    throws MultipartRequestParsingException;
  
  InputStream getSubmittedFileInputStream(int paramInt)
    throws MultipartRequestParsingException;
  
  void clearAllButCapturedInputStream();
  
  void clearAll();
}
