package com.ks.request;

import com.ks.pojo.Permutation;

import java.io.Serializable;
import java.util.Map;

public final class RequestDetails implements Serializable {
	private static final long serialVersionUID = 1L;
	public String clientAddress;
	public String agent;
	public String servletPath;
	public String queryString;
	public Permutation queryStringVariants;
	public String requestedSessionId;
	public boolean sessionCameFromCookie;
	public boolean sessionCameFromURL;
	public String referrer;
	public String url;
	public String uri;
	public String method;
	public String protocol;
	public String mimeType;
	public String remoteHost;
	public String remoteUser;
	public Map headerMap;
	public Map headerMapVariants;
	public Map cookieMap;
	public Map cookieMapVariants;
	public String encoding;
	public int contentLength;
	public String scheme;
	public String serverName;
	public int serverPort;
	public String authType;
	public String contextPath;
	public String pathInfo;
	public String pathTranslated;
	public int remotePort;
	public int localPort;
	public String localAddr;
	public String localName;
	public Map requestParameterMap;
	public Map requestParameterMapVariants;
	public String country;
	public boolean somethingHasBeenUncovered;
	public boolean nonStandardPermutationsRequired;
	public byte decodingPermutationLevel;
}
