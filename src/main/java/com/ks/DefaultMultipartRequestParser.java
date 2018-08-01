package com.ks;

import com.ks.adapters.ServletRequestDataSourceAdapter;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.exceptions.MultipartRequestParsingException;
import com.ks.parser.MultipartRequestParser;
import com.ks.parser.ParsedMultipartRequest;
import com.ks.utils.RequestUtils;

import javax.mail.MessagingException;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class DefaultMultipartRequestParser implements MultipartRequestParser {

    public void setFilterConfig(final FilterConfig filterConfig) throws FilterConfigurationException {
        //final ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
    }

    
    public boolean isMultipartRequest(final HttpServletRequest request) {
        final String contentType = RequestUtils.getContentType(request);
        final int contentLength = request.getContentLength();
        // IE handles redirects using the previous request's content type so we need to 
        // ignore the Content-Type on requests with a Content-Length less than zero.
        return contentType != null && contentType.startsWith("multipart/form-data") && contentLength > -1;
    }

    
    
    public ParsedMultipartRequest parse(final HttpServletRequest request, final int multipartSizeLimit, final boolean bufferFileUploadsToDisk) throws MultipartRequestParsingException {
        try {
            final ServletRequestDataSourceAdapter adapter = new ServletRequestDataSourceAdapter(request, multipartSizeLimit, bufferFileUploadsToDisk);
            final ParsedMultipartRequest parsedRequest = new DefaultParsedMultipartRequest(adapter);
            return parsedRequest;
        } catch (IOException e) { 
            throw new MultipartRequestParsingException(e);
        } catch (MessagingException e) { 
            throw new MultipartRequestParsingException(e);
        } catch (RuntimeException e) { 
            throw new MultipartRequestParsingException(e);
        }
    }


}
