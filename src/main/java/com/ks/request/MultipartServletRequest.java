package com.ks.request;

import com.ks.adapters.IteratorEnumerationAdapter;
import com.ks.adapters.ServletInputStreamAdapter;
import com.ks.exceptions.MultipartRequestParsingException;
import com.ks.exceptions.ServerAttackException;
import com.ks.parser.MultipartRequestParser;
import com.ks.parser.ParsedMultipartRequest;
import com.ks.pojo.MultipartFileInfo;
import com.ks.pojo.MultipartSizeLimitDefinition;
import com.ks.utils.ServerUtils;
import com.ks.utils.ZipScannerUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MultipartServletRequest extends HttpServletRequestWrapper {

    private static final boolean DEBUG = false;


    private boolean hasUrlParamsOnFirstAttempt = false;


    private ParsedMultipartRequest parsedRequest;


    /** Map of all the files uploaded in the multipart form.  Only set if
     <code>multiPart</code> is <code>true</code>. */
    private Map filesOfRequest = new HashMap();


    /** Map of all the parameter values uploaded in the multipart form.  Only set if
     <code>multiPart</code> is <code>true</code>. */
    private Map formParametersOfRequest = new HashMap();
    private Map urlParametersOfRequest = new HashMap();
    private Map urlAndFormParametersOfRequestMerged = new HashMap();



    private boolean hideMultipartFormParametersSinceWeAreWithingApplicationAccess = false;


    private final boolean bufferFileUploadsToDisk;

    private final MultipartSizeLimitDefinition multipartSizeLimit;
    private int fileCount;




    public MultipartServletRequest(final MultipartRequestParser parser, final HttpServletRequest request, final boolean bufferFileUploadsToDisk) throws IOException {
        this(parser, request, null, bufferFileUploadsToDisk);
    }

    public MultipartServletRequest(final MultipartRequestParser parser, final HttpServletRequest request, final MultipartSizeLimitDefinition multipartSizeLimit, final boolean bufferFileUploadsToDisk) throws IOException {
        super(request);
        this.bufferFileUploadsToDisk = bufferFileUploadsToDisk;
        this.multipartSizeLimit = multipartSizeLimit; // may be null
        if (DEBUG) {
            System.out.println("Calling constructor for a potentially multipart submitted form: " + request);
        }
        try {
            this.parsedRequest = parser.parse(request, multipartSizeLimit==null?0:multipartSizeLimit.getMaxInputStreamLength(), bufferFileUploadsToDisk);
            extractSubmittedFormValues();
            extractSubmittedUrlValues();
        } catch (MultipartRequestParsingException e) {
            throw new IOException(e.toString());
        } catch (RuntimeException e) {
            throw new IOException(e.toString());
        } finally {
            if (this.parsedRequest != null) this.parsedRequest.clearAllButCapturedInputStream();
        }
    }






    public ServletInputStream getInputStream() throws IOException {
        // multipart handling:
        try {
            return this.parsedRequest == null ? null : new ServletInputStreamAdapter(this.parsedRequest.replayCapturedInputStream());
        } catch (MultipartRequestParsingException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Map getParameterMap() {
        return ServerUtils.convertMapOfCollectionsToMapOfStringArrays( this.hideMultipartFormParametersSinceWeAreWithingApplicationAccess ? this.urlParametersOfRequest : this.urlAndFormParametersOfRequestMerged );
    }

    public Enumeration getParameterNames() {
        return new IteratorEnumerationAdapter((this.hideMultipartFormParametersSinceWeAreWithingApplicationAccess ? this.urlParametersOfRequest : this.urlAndFormParametersOfRequestMerged).keySet().iterator());
    }

    public String getParameter(String name) {
        // multipart handling:
        final List values = (List) (this.hideMultipartFormParametersSinceWeAreWithingApplicationAccess ? this.urlParametersOfRequest : this.urlAndFormParametersOfRequestMerged).get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        final Object result = values.get(0);
        return result == null ? null : result.toString();
    }

    public String[] getParameterValues(String name) {
        // multipart handling:
        final List values = (List) (this.hideMultipartFormParametersSinceWeAreWithingApplicationAccess ? this.urlParametersOfRequest : this.urlAndFormParametersOfRequestMerged).get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return ServerUtils.convertCollectionToStringArray(values);
    }


    public Iterator/*<MultipartFileInfo>*/ getSubmittedFiles(String name) {
        // multipart handling:
        final List/*<MultipartFileInfo>*/ files = (List) this.filesOfRequest.get(name);
        if (files == null || files.isEmpty()) {
            return Collections.EMPTY_SET.iterator();
        }
        return files.iterator();
    }





    public boolean isHideMultipartFormParametersSinceWeAreWithingApplicationAccess() {
        return hideMultipartFormParametersSinceWeAreWithingApplicationAccess;
    }
    public void setHideMultipartFormParametersSinceWeAreWithingApplicationAccess(boolean hideMultipartFormParametersSinceWeAreWithingApplicationAccess) {
        this.hideMultipartFormParametersSinceWeAreWithingApplicationAccess = hideMultipartFormParametersSinceWeAreWithingApplicationAccess;
    }




    public void clear() {
        if (this.parsedRequest != null) {
            this.parsedRequest.clearAll();
            this.parsedRequest = null;
        }
        if (this.filesOfRequest != null) {
            this.filesOfRequest.clear();
        }
    }



    // added to also include URL params of the form submit... yes, that's all possible...
    private void extractSubmittedFormValues() throws MultipartRequestParsingException, IOException {
        int numElements = this.parsedRequest.getElementCount();

        String formParamName = null;
        String value = null;

        String filename = null;
        String contenttype = null;
        long size = 0;
        MultipartFileInfo mpFileInfo = null;

        for (int i = 0; i < numElements; ++i) {
            formParamName = this.parsedRequest.getFormFieldName(i);
            filename = this.parsedRequest.getSubmittedFileName(i);
            if (filename == null) { // THIS PARAMETER VALUE IS A REGULAR STRING PARAMETER
                value = this.parsedRequest.getFormFieldContent(i);
                addToMapOfCollections(this.formParametersOfRequest, formParamName, value);
                addToMapOfCollections(this.urlAndFormParametersOfRequestMerged, formParamName, value);
            } else { // THIS PARAMETER VALUE IS A FILE
                fileCount++;
                // check size limits (stream size limit is checked in datasource adapter)
                if (multipartSizeLimit != null && multipartSizeLimit.getMaxFileUploadCount() > 0) {
                    if (fileCount > multipartSizeLimit.getMaxFileUploadCount()) throw new ServerAttackException("maxFileUploadCount threshold for multipart file uploads exceeded");
                }
                size = this.parsedRequest.getSubmittedFileSize(i);
                if (multipartSizeLimit != null) {
                    if (multipartSizeLimit.getMaxFileNameLength() > 0 && filename.length() > multipartSizeLimit.getMaxFileNameLength()) throw new ServerAttackException("maxFileNameLength threshold for multipart file uploads exceeded");
                    if (multipartSizeLimit.getMaxFileUploadSize() > 0 && size > multipartSizeLimit.getMaxFileUploadSize()) throw new ServerAttackException("maxFileUploadSize threshold for multipart file uploads exceeded");
                }
                contenttype = this.parsedRequest.getSubmittedFileContentType(i); // set by the client -- can't be trusted
                final InputStream in = this.parsedRequest.getSubmittedFileInputStream(i);
                mpFileInfo = (size > 0)
                        ? new MultipartFileInfo(formParamName, contenttype, filename, in, size, bufferFileUploadsToDisk)
                        : new MultipartFileInfo(formParamName, contenttype, filename, in, bufferFileUploadsToDisk);
                in.close();
                // check against ZIP bombs
                if (multipartSizeLimit != null && (multipartSizeLimit.getZipBombThresholdTotalSize() > 0 || multipartSizeLimit.getZipBombThresholdFileCount() > 0)) {
                    if (ZipScannerUtils.isZipBomb(mpFileInfo.getFile(), multipartSizeLimit.getZipBombThresholdTotalSize(), multipartSizeLimit.getZipBombThresholdFileCount())) throw new ServerAttackException("Potential ZIP bomb detected");
                }
                addToMapOfCollections(this.filesOfRequest, formParamName, mpFileInfo);
            }
        }
    }

    private void extractSubmittedUrlValues() {
        if (hasUrlParamsOnFirstAttempt) {
            return;
        }
        // NOW THE URL PARAMS ALSO (they can be safely taken from the underlying original request, which holds all URL params BUT NO form params since we're havig a multipart form submit here...)
        if (DEBUG) {
            System.out.println("==> in delegate: " + getRequest().getParameterMap());
        }
        for (final Enumeration urlParamNames = getRequest().getParameterNames(); urlParamNames.hasMoreElements();) {
            final String urlParamName = (String) urlParamNames.nextElement();
            if (DEBUG) {
                System.out.println("---> URL PARAM IN MULTIPART FORM: " + urlParamName);
            }
            final String[] values = getRequest().getParameterValues(urlParamName);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    addToMapOfCollections(this.urlParametersOfRequest, urlParamName, values[i]);
                    addToMapOfCollections(this.urlAndFormParametersOfRequestMerged, urlParamName, values[i]);
                    hasUrlParamsOnFirstAttempt = true;
                    if (DEBUG) {
                        System.out.println("           ---> with value: " + values[i]);
                    }
                }
            }
        }
    }


    public void reextractSubmittedUrlValues() {
        extractSubmittedUrlValues();
    }


    private static void addToMapOfCollections(final Map/*<String,List<String>>*/ map, final String key, final String value) {
        List/*<String>*/ col = (List) map.get(key);
        if (col == null) {
            col = new ArrayList/*<String>*/();
            map.put(key, col);
        }
        col.add(value);
    }
    private static void addToMapOfCollections(final Map/*<String,List<MultipartFileInfo>>*/ map, final String key, final MultipartFileInfo value) {
        List/*<MultipartFileInfo>*/ col = (List) map.get(key);
        if (col == null) {
            col = new ArrayList/*<MultipartFileInfo>*/();
            map.put(key, col);
        }
        col.add(value);
    }
}
