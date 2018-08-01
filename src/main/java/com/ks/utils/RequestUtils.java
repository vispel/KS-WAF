package com.ks.utils;

import com.ks.KsWafFilter;
import com.ks.container.RequestDefinitionContainer;
import com.ks.crypto.CryptoKeyAndSalt;
import com.ks.exceptions.ClientIpDeterminationException;
import com.ks.exceptions.ServerAttackException;
import com.ks.pojo.interfaces.ClientIpDeterminator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestUtils {

	private RequestUtils() {}


	private static final boolean DEBUG = false;


	private static boolean isOldJavaEE13 = false;


	private static final String SENSITIVE_VALUE_REMOVED = "<SENSITIVE-DATA-REMOVED>";
	private static final String CLIENT_LOGGING_DISABLED = "<CLIENT-LOGGING-DISABLED>";

	private static final String IMAGE_MAP_EXLUDE_EXPRESSION = "(?i).*(_(.y|.x){0,2}\\z)";





	public static String getContentType(final ServletRequest request) {
		String type = request.getContentType();
		if (type == null && request instanceof HttpServletRequest) {
			type = ((HttpServletRequest)request).getHeader("Content-Type");
		}
		return type;
	}


	public static String determineClientIp(final HttpServletRequest request, final ClientIpDeterminator clientIpDeterminator) {
		if (clientIpDeterminator != null) {
			try {
				final String result = clientIpDeterminator.determineClientIp(request);
				return result != null ? result : request.getRemoteAddr();
			} catch (ClientIpDeterminationException e) {
				System.err.println("Unable to determine client IP: "+e.getMessage());
			} catch (RuntimeException e) {
				System.err.println("Unable to determine client IP (RuntimeException): "+e.getMessage());
			}
		}
		return request.getRemoteAddr();
	}


	public static String extractSecurityRelevantRequestContent(final HttpServletRequest request, final String ip, final boolean logSessionValues, final Pattern sensitiveRequestParamNamePattern, final Pattern sensitiveRequestParamNameAndValueUrlPattern, final Pattern sensitiveValuePattern, final boolean logClientUserData) {
		final StringBuilder logMessage = new StringBuilder();
		try {
			appendValueToMessage(logMessage, "client", logClientUserData?ip:CLIENT_LOGGING_DISABLED);
			appendValueToMessage(logMessage, "date", ""+new Date());
			appendValueToMessage(logMessage, "servletPath", request.getServletPath());

			final Matcher sensitiveRequestParamNameMatcherToReuse = sensitiveRequestParamNamePattern.matcher("");
			final Matcher sensitiveRequestParamNameAndValueUrlMatcherToReuse = sensitiveRequestParamNameAndValueUrlPattern.matcher("");
			final Matcher sensitiveValueMatcherToReuse = sensitiveValuePattern.matcher("");

			final String queryString = removeSensitiveData(null,request.getQueryString(),sensitiveRequestParamNameMatcherToReuse,sensitiveRequestParamNameAndValueUrlMatcherToReuse,sensitiveValueMatcherToReuse);
			appendValueToMessage(logMessage, "queryString (sensitive data removed)", queryString);

			appendValueToMessage(logMessage, "requestedSessionId", request.getRequestedSessionId());
			appendValueToMessage(logMessage, "requestedSessionIdValid", ""+request.isRequestedSessionIdValid());
			appendValueToMessage(logMessage, "requestURL", request.getRequestURL());
			appendValueToMessage(logMessage, "requestURI", request.getRequestURI());
			appendValueToMessage(logMessage, "method", request.getMethod());
			appendValueToMessage(logMessage, "protocol", request.getProtocol());
			appendValueToMessage(logMessage, "mimeType", request.getContentType());
			if (logClientUserData) {
				appendValueToMessage(logMessage, "remoteAddr", request.getRemoteAddr());
				appendValueToMessage(logMessage, "remoteHost", request.getRemoteHost());
				appendValueToMessage(logMessage, "remoteUser", request.getRemoteUser());
				appendValueToMessage(logMessage, "userPrincipal", ""+request.getUserPrincipal());
			} else {
				appendValueToMessage(logMessage, "remoteAddr", CLIENT_LOGGING_DISABLED);
				appendValueToMessage(logMessage, "remoteHost", CLIENT_LOGGING_DISABLED);
				appendValueToMessage(logMessage, "remoteUser", CLIENT_LOGGING_DISABLED);
				appendValueToMessage(logMessage, "userPrincipal", CLIENT_LOGGING_DISABLED);
			}
			appendValueToMessage(logMessage, "encoding", request.getCharacterEncoding());
			appendValueToMessage(logMessage, "contentLength", ""+request.getContentLength());
			appendValueToMessage(logMessage, "scheme", request.getScheme());
			appendValueToMessage(logMessage, "secure", ""+request.isSecure());
			appendValueToMessage(logMessage, "serverName", request.getServerName());
			appendValueToMessage(logMessage, "serverPort", ""+request.getServerPort());
			appendValueToMessage(logMessage, "authType", request.getAuthType());
			appendValueToMessage(logMessage, "contextPath", request.getContextPath());
			appendValueToMessage(logMessage, "pathInfo", request.getPathInfo());
			appendValueToMessage(logMessage, "pathTranslated", request.getPathTranslated());
			appendValueToMessage(logMessage, "locale", ""+request.getLocale());

			// session stuff
			final HttpSession session = request.getSession(false);
			appendValueToMessage(logMessage, "hasSession", ""+(session != null));
			if (session != null) {
				appendValueToMessage(logMessage, "sessionNew", ""+session.isNew());
				appendValueToMessage(logMessage, "sessionMaxInactiveInterval", ""+session.getMaxInactiveInterval());
				appendValueToMessage(logMessage, "sessionCreationTime", formatDateTime(session.getCreationTime()));
				appendValueToMessage(logMessage, "sessionLastAccessedTime", formatDateTime(session.getLastAccessedTime()));
				if (logSessionValues) {
					if (DEBUG) { // = show also KsWaf internal values
						for (final Enumeration/*<String>*/ names = ServerUtils.getAttributeNamesIncludingInternal(session); names.hasMoreElements();) {
							final String name = (String) names.nextElement();
							final Object value = ServerUtils.getAttributeIncludingInternal(session,name);
							appendValueToMessage(logMessage, "session (sensitive data removed): "+name, removeSensitiveData(name,value,sensitiveRequestParamNameMatcherToReuse,sensitiveRequestParamNameAndValueUrlMatcherToReuse,sensitiveValueMatcherToReuse));
						}
					} else {
						for (final Enumeration/*<String>*/ names = session.getAttributeNames(); names.hasMoreElements();) {
							final String name = (String) names.nextElement();
							final Object value = session.getAttribute(name);
							appendValueToMessage(logMessage, "session (sensitive data removed): "+name, removeSensitiveData(name,value,sensitiveRequestParamNameMatcherToReuse,sensitiveRequestParamNameAndValueUrlMatcherToReuse,sensitiveValueMatcherToReuse));
						}
					}
				}
			}

			// those methods are only availably in Java EE 1.4 or higher, so take care for older Java EE 1.3
			if (!isOldJavaEE13) {
				try {
					appendValueToMessage(logMessage, "remotePort", ""+request.getRemotePort());
					appendValueToMessage(logMessage, "localPort", ""+request.getLocalPort());
					appendValueToMessage(logMessage, "localAddr", request.getLocalAddr());
					appendValueToMessage(logMessage, "localName", request.getLocalName());
				} catch (NoSuchMethodError e) {
					isOldJavaEE13 = true;
				}
			}

			// request parameters
			{
				final Enumeration names = request.getParameterNames();
				if (names != null) {
					while (names.hasMoreElements()) {
						final String name = (String) names.nextElement();
						final String[] values = request.getParameterValues(name);
						if (values != null) {
							for (int i=0 ; i<values.length; i++) {
								final String requestParam = removeSensitiveData(name,values[i],sensitiveRequestParamNameMatcherToReuse,sensitiveRequestParamNameAndValueUrlMatcherToReuse,sensitiveValueMatcherToReuse);

								appendValueToMessage(logMessage, "requestParam (sensitive data removed): "+name, requestParam);
                                /* too slow
                                if (logDecodedAndTransformedVariants) {
                                    final Permutation variants = ServerUtils.permutateVariants(requestParam,true);
                                    for (final Iterator iter = variants.iterator(); iter.hasNext();) {
                                        final String variant = (String) iter.next();
                                        if (!variant.equals(requestParam)) appendValueToMessage(logMessage, "requestParam (sensitive data removed + decoded and transformed variant): "+name, variant);
                                    }
                                }
                                 */

							}
						}
					}
				} else {
					System.err.println("This servlet-container does not allow the access of request params... VERY STRANGE");
				}
			}

			// HTTP headers
			{
				final Enumeration names = request.getHeaderNames();
				if (names != null) {
					while (names.hasMoreElements()) {
						final String name = (String) names.nextElement();
						if (!logClientUserData) {
							final String nameLowercased = name.toLowerCase();
							if (nameLowercased.contains("forward") || nameLowercased.contains("proxy") || nameLowercased.contains("client") || nameLowercased.contains("user")) {
								appendValueToMessage(logMessage, "header: "+name, CLIENT_LOGGING_DISABLED);
								continue; // to avoid logging it in clear text form
							}
						}
						for (final Enumeration values = request.getHeaders(name); values.hasMoreElements();) {
							final String value = (String) values.nextElement();
							appendValueToMessage(logMessage, "header: "+name, value);

						}
					}
				} else {
					System.err.println("This servlet-container does not allow the access of HTTP headers (unfortunately)");
				}
			}

			// Cookie values
			{
				final Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (int i=0; i<cookies.length; i++) {
						final Cookie cookie = cookies[i];
						if (cookie != null) {
							final String name = cookie.getName();
							final String value = cookie.getValue();

							appendValueToMessage(logMessage, "cookie: "+name, value);

						}
					}
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
			appendValueToMessage(logMessage, "Unable to create security log message (unexpected exception during message creation)", e.getMessage());
		}
		return logMessage.toString();
	}




	public static String printParameterMap(final Map/*<String,String[]>*/ parameterMap) {
		if (parameterMap == null) return null;
		final StringBuilder result = new StringBuilder();
		for (Object o : parameterMap.entrySet()) {
			final Map.Entry entry = (Map.Entry) o;
			result.append(entry.getKey()).append("-->").append(Arrays.asList((Object[]) entry.getValue())).append("   ");
		}
		return result.toString();
	}




	private static void changeKeysToUpperCaseAndUnifyValues(final Map/*<String,String[]>*/ map) {
		if (map == null) return;
		final Map copy = new HashMap(map.size());
		for (final Iterator iter = map.keySet().iterator(); iter.hasNext();) {
			final String key = (String) iter.next();
			final String[] value = (String[]) map.get(key);
			final String keyUpper = key.toUpperCase();
			// if the same key occurs several times with different cases, add all values, don't overwrite
			if (copy.containsKey(keyUpper)) {
				final String[] alreadyContainedValue = (String[]) copy.get(keyUpper);
				copy.put( keyUpper, combineArrays(alreadyContainedValue,value) );
			} else copy.put(keyUpper, value);
		}
		map.clear();
		map.putAll(copy);
	}

	private static String[] combineArrays(final String[] leftPart, final String[] rightPart) {
		final String[] result = new String[leftPart.length+rightPart.length];
		if (leftPart.length > 0) System.arraycopy(leftPart, 0, result, 0, leftPart.length);
		if (rightPart.length > 0) System.arraycopy(rightPart, 0, result, leftPart.length, rightPart.length);
		return result;
	}



	public static Map/*<String,String[]>*/ createHeaderMap(final HttpServletRequest request) {
		final Map/*<String,String[]>*/ headerMap = new HashMap();
		final Enumeration names = request.getHeaderNames();
		if (names == null) {
			System.err.println("This servlet-container does not allow the access of HTTP headers (unfortunately)");
			return headerMap;
		}
		while (names.hasMoreElements()) {
			final String name = (String) names.nextElement();
			if (!headerMap.containsKey(name)) {
				final List/*<String>*/ collectedValues = new ArrayList();
				for (final Enumeration values = request.getHeaders(name); values.hasMoreElements();) {
					final String value = (String) values.nextElement();
					collectedValues.add(value);
				}
				final String[] collectedValuesAsArray = (String[]) collectedValues.toArray(new String[0]);
				headerMap.put(name, collectedValuesAsArray);
			}
		}
		changeKeysToUpperCaseAndUnifyValues(headerMap);
		return headerMap;
	}



	public static  Map/*<String,String[]>*/ createCookieMap(final HttpServletRequest request) {
		final Map/*<String,List<String>>*/ cookieMap = new HashMap();
		final Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i=0; i<cookies.length; i++) {
				final Cookie cookie = cookies[i];
				if (cookie != null) {
					final String name = cookie.getName();
					List/*<String>*/ valueList = (List) cookieMap.get(name);
					if (valueList == null) {
						valueList = new ArrayList();
						cookieMap.put(name, valueList);
					}
					final String value = cookie.getValue();
					valueList.add(value);
				}
			}
		}
		final Map result = convertMapOfListOfStrings2MapOfStringArray(cookieMap);
		changeKeysToUpperCaseAndUnifyValues(result);
		return result;
	}




	public static String removeParameter(String queryString, final String parameterKey) {
		final int index; // TODO: also take care for encoded querystrings ?
		if (queryString != null && queryString.length() > 0 && parameterKey != null && (index=queryString.indexOf(parameterKey)) != -1) {
			// determine start
			final boolean isAmpersandBefore = index > 0 && queryString.charAt(index-1) == '&';
			final boolean isMaskedAmpersandBefore = !isAmpersandBefore && index > 4
					&& queryString.charAt(index-5) == '&' && queryString.charAt(index-4) == 'a' && queryString.charAt(index-3) == 'm' && queryString.charAt(index-2) == 'p' && queryString.charAt(index-1) == ';';
			final int start = isAmpersandBefore ? index-1 : isMaskedAmpersandBefore ? index-5 : index;
			// determine end
			int end = queryString.length()-1; char c;
			for (int i=index+parameterKey.length(); i<queryString.length(); i++) {
				c = queryString.charAt(i);
				if (c == '&' || c == '#') {
					end = i-1;
					break;
				}
			}
			final boolean isMaskedAmpersandAfter = queryString.length() >= end+6
					&& queryString.charAt(end+1) == '&' && queryString.charAt(end+2) == 'a' && queryString.charAt(end+3) == 'm' && queryString.charAt(end+4) == 'p' && queryString.charAt(end+5) == ';';
			final boolean isAmpersandAfter = !isMaskedAmpersandAfter && queryString.length() >= end+2 && queryString.charAt(end+1) == '&';
			if (!isAmpersandBefore && !isMaskedAmpersandBefore) {
				end = isAmpersandAfter ? end+1 : isMaskedAmpersandAfter ? end+5 : end;
			}
			end++;
			// extract string
			if (DEBUG) {
				System.out.println("=================");
				System.out.println("queryString: "+queryString);
				System.out.println("isAmpersandBefore: "+isAmpersandBefore);
				System.out.println("isMaskedAmpersandBefore: "+isMaskedAmpersandBefore);
				System.out.println("isAmpersandAfter: "+isAmpersandAfter);
				System.out.println("isMaskedAmpersandAfter: "+isMaskedAmpersandAfter);
				System.out.println("Extracting: "+queryString.substring(start,end));
			}
			queryString = queryString.substring(0,start) + queryString.substring(end);
			if (DEBUG) {
				System.out.println("Result: "+queryString);
			}
		}
		return queryString;
	}




	public static String createOrRetrieveRandomTokenFromSession(final HttpSession session, final String sessionKey) {
		return createOrRetrieveRandomTokenFromSession(session, sessionKey, -1, -1);
	}
	public static String createOrRetrieveRandomTokenFromSession(final HttpSession session, final String sessionKey, final int minimumLength, final int maximumLength) {
		String value = (String) ServerUtils.getAttributeIncludingInternal(session,sessionKey);
		if (value == null) {
			if (maximumLength > minimumLength && minimumLength > 0) value = CryptoUtils.generateRandomToken(true, CryptoUtils.generateRandomNumber(true,minimumLength,maximumLength) );
			else value = CryptoUtils.generateRandomToken(true);
			session.setAttribute(sessionKey, value);
		}
		return value;
	}

	public static String retrieveRandomTokenFromSessionIfExisting(final HttpServletRequest request, final String sessionKey) {
		final HttpSession session = request.getSession(false);
		if (session == null) return null;
		return (String) ServerUtils.getAttributeIncludingInternal(session,sessionKey);
	}


	public static CryptoKeyAndSalt createOrRetrieveRandomCryptoKeyFromSession(final HttpSession session, final String sessionKey, final boolean extraEncryptedValueHashProtection) throws NoSuchAlgorithmException {
		CryptoKeyAndSalt value = (CryptoKeyAndSalt) ServerUtils.getAttributeIncludingInternal(session,sessionKey);
		if (value == null) {
			value = CryptoUtils.generateRandomCryptoKeyAndSalt(extraEncryptedValueHashProtection);
			session.setAttribute(sessionKey, value);
		}
		assert value != null;
		return value;
	}





	public static final class DecryptedQuerystring {
		public String decryptedString = null;
		public Boolean isFormSubmit = null;
		public Boolean resourceEndsWithSlash = null;
		public Boolean isFormMultipart = null;
		public Boolean wasManipulated = Boolean.FALSE;

		public String toString() {
			return this.decryptedString;
		}
	}

	/**
	 * returns null when the URL is not encrypted
	 */
	public static DecryptedQuerystring decryptQueryStringInServletPathWithQueryString(final String contextPath, final String servletPath, String servletPathWithQueryStringEncrypted, final String cryptoDetectionString, final CryptoKeyAndSalt key, final String uriRequested, final boolean isRequestHavingAdditionalParameters, final boolean isRequestMethodPOST, final boolean useFullPathForResourceToBeAccessedProtection, final boolean additionalFullOrMediumResourceRemoval, final boolean appendQuestionmarkOrAmpersandToLinks) {
		if (servletPath == null || servletPathWithQueryStringEncrypted == null) return null;
		if (contextPath == null) throw new NullPointerException("contextPath must not be null");
		if (cryptoDetectionString == null) throw new NullPointerException("cryptoDetectionString must not be null"); // cryptoDetectionString is used to detect encrypted links
		if (key == null) throw new NullPointerException("key must not be null");
		if (uriRequested == null) throw new NullPointerException("uriRequested must not be null");
		final int firstQuestionMark = servletPathWithQueryStringEncrypted.indexOf('?');
		if (firstQuestionMark > -1 && servletPathWithQueryStringEncrypted.length() > firstQuestionMark+1) {
			try {
				if (appendQuestionmarkOrAmpersandToLinks && servletPathWithQueryStringEncrypted.endsWith("&")) servletPathWithQueryStringEncrypted = servletPathWithQueryStringEncrypted.substring(0,servletPathWithQueryStringEncrypted.length()-1);
				// remove the cryptoDetectionString
				final int pos = servletPathWithQueryStringEncrypted.indexOf(cryptoDetectionString);
				if (pos == -1) return null; // returns null when the URL is not encrypted
				servletPathWithQueryStringEncrypted = servletPathWithQueryStringEncrypted.substring(0,pos) + servletPathWithQueryStringEncrypted.substring(pos+cryptoDetectionString.length());
				// safe already unencrypted prefix
				final int firstQuestionMarkOrAmpersandLeftFromCryptoDetectionString = Math.max(firstQuestionMark, servletPathWithQueryStringEncrypted.lastIndexOf('&',pos));
				final String alreadyUnencryptedPrefix = servletPathWithQueryStringEncrypted.substring(0,firstQuestionMarkOrAmpersandLeftFromCryptoDetectionString+1);
				// safe already unencrypted suffix
				final int firstAmpersandOrEqualsRightFromCryptoDetectionString;
				final int tmpAmpersandRightFromCryptoDetectionString = servletPathWithQueryStringEncrypted.indexOf('&',pos);
				final int tmpEqualsRightFromCryptoDetectionString = servletPathWithQueryStringEncrypted.indexOf('=',pos);
				if (tmpAmpersandRightFromCryptoDetectionString == -1) firstAmpersandOrEqualsRightFromCryptoDetectionString = tmpEqualsRightFromCryptoDetectionString;
				else if (tmpEqualsRightFromCryptoDetectionString == -1) firstAmpersandOrEqualsRightFromCryptoDetectionString = tmpAmpersandRightFromCryptoDetectionString;
				else firstAmpersandOrEqualsRightFromCryptoDetectionString = Math.min(tmpEqualsRightFromCryptoDetectionString, tmpAmpersandRightFromCryptoDetectionString);
				final String alreadyUnencryptedSuffix = firstAmpersandOrEqualsRightFromCryptoDetectionString > -1 ? servletPathWithQueryStringEncrypted.substring(firstAmpersandOrEqualsRightFromCryptoDetectionString) : "";
				// define what to decrypt
				String decrypt = servletPathWithQueryStringEncrypted.substring(firstQuestionMarkOrAmpersandLeftFromCryptoDetectionString+1, firstAmpersandOrEqualsRightFromCryptoDetectionString > -1 ? firstAmpersandOrEqualsRightFromCryptoDetectionString : servletPathWithQueryStringEncrypted.length());
				// save any anchor
				String anchor = null; final int anchorPos = decrypt.indexOf('#');
				if (anchorPos > -1) {
					anchor = decrypt.substring(anchorPos);
					decrypt = decrypt.substring(0,anchorPos);
				}
				// if decrypt ends with equals sign (as is the case when the browser automatically appends an equals sign while mapping GET form parameters into URL parameters, remove that
				if (decrypt.length() > 1 && decrypt.charAt(decrypt.length()-1) == '=') decrypt = decrypt.substring(0,decrypt.length()-1);
				final String alreadyUnencryptedPrefixWithParamsRemoved = removeAfterFirstQuestionMark(alreadyUnencryptedPrefix);
				final String alreadyUnencryptedSuffixWithParamsRemoved = removeBeforeFirstHash(alreadyUnencryptedSuffix);
				// Using the requestedURI as a check for the resource to be accessed makes very much sense, since the textual link (for example <a href="xxx">) in the source html page might already be encoded
				// by the application and the requestedURI is also still %-encoded when a request is made. So comparing them makes very much sense since then we even detect mismatches in the given encoding as
				// a potential spoofing automatically...
				final String resourceToBeAccessed = ServerUtils.extractResourceToBeAccessed(uriRequested, contextPath, servletPath, useFullPathForResourceToBeAccessedProtection);

				// decrypt the query-string
				StringBuilder result = new StringBuilder(servletPathWithQueryStringEncrypted.length());
				String decryptedQueryString = CryptoUtils.decryptURLSafe(decrypt, key);
				if (DEBUG) System.out.println("decryptedQueryString: "+decryptedQueryString);
				// extract the decrypted payload by removing the resource-to-be-accessed value and the request-type-get/post value (and double-checking it for security reasons)
				int delimiterPos = decryptedQueryString.lastIndexOf(KsWafFilter.INTERNAL_URL_DELIMITER);
				if (delimiterPos == -1 || delimiterPos == decryptedQueryString.length()-1) throw new ServerAttackException("Decrypted URL contains no matching flags");
				final DecryptedQuerystring decryptedQuerystring = new DecryptedQuerystring();
				// read flag for resourceEndsWithSlash form - and check for manual adding of parameters
				final char resourceEndsWithSlashFlag = decryptedQueryString.charAt(delimiterPos+1);
				final boolean resourceEndsWithSlash;
				if (KsWafFilter.INTERNAL_RESOURCE_ENDS_WITH_SLASH_YES_FLAG == resourceEndsWithSlashFlag) resourceEndsWithSlash = true;
				else if (KsWafFilter.INTERNAL_RESOURCE_ENDS_WITH_SLASH_NO_FLAG == resourceEndsWithSlashFlag) resourceEndsWithSlash = false;
				else throw new ServerAttackException("Decrypted URL contains unknown 'resource ends with slash' value: "+resourceEndsWithSlashFlag);
				decryptedQuerystring.resourceEndsWithSlash = resourceEndsWithSlash;
				// read flag for multipart form - and check for manual adding of parameters
				delimiterPos = decryptedQueryString.lastIndexOf(KsWafFilter.INTERNAL_URL_DELIMITER, delimiterPos-1);
				final char formMultipartFlag = decryptedQueryString.charAt(delimiterPos+1);
				final boolean isFormMultipart;
				if (KsWafFilter.INTERNAL_MULTIPART_YES_FLAG == formMultipartFlag) isFormMultipart = true;
				else if (KsWafFilter.INTERNAL_MULTIPART_NO_FLAG == formMultipartFlag) isFormMultipart = false;
				else throw new ServerAttackException("Decrypted URL contains unknown 'form multipart' value: "+formMultipartFlag);
				decryptedQuerystring.isFormMultipart = isFormMultipart;
				// read flag for form/link - and check for manual adding of parameters
				delimiterPos = decryptedQueryString.lastIndexOf(KsWafFilter.INTERNAL_URL_DELIMITER, delimiterPos-1);
				final char formLinkFlag = decryptedQueryString.charAt(delimiterPos+1);
				final boolean isFormAction;
				if (KsWafFilter.INTERNAL_TYPE_FORM_FLAG == formLinkFlag) isFormAction = true;
				else if (KsWafFilter.INTERNAL_TYPE_LINK_FLAG == formLinkFlag) isFormAction = false;
				else throw new ServerAttackException("Decrypted URL contains unknown 'form/link' value: "+formLinkFlag);
				decryptedQuerystring.isFormSubmit = isFormAction;
				if (isRequestHavingAdditionalParameters && !isFormAction) decryptedQuerystring.wasManipulated = Boolean.TRUE; // OLD throw new ServerAttackException("Encrypted request contains additional unencrypted parameters (it is not a form submit and strict parameter checking is enabled)");
				// continue with double-checking the request method type (GET or POST)
				delimiterPos = decryptedQueryString.lastIndexOf(KsWafFilter.INTERNAL_URL_DELIMITER, delimiterPos-1);
				if (delimiterPos == -1 || delimiterPos == decryptedQueryString.length()-1) throw new ServerAttackException("Decrypted URL contains no 'resource-to-be-accessed' and/or 'request-type-get/post' and/or 'form/link' value");
				final char flag = decryptedQueryString.charAt(delimiterPos+1);
				if (flag != KsWafFilter.INTERNAL_METHOD_TYPE_UNDEFINED) {
					if (isRequestMethodPOST) {
						// so the value in the decrypted URL must also be of type POST
						if (flag != KsWafFilter.INTERNAL_METHOD_TYPE_POST) throw new ServerAttackException("Decrypted URL contains mismatching 'request-type-get/post' value (expected POST)");
					} else {
						// so the value in the decrypted URL must also be of type GET
						if (flag != KsWafFilter.INTERNAL_METHOD_TYPE_GET) throw new ServerAttackException("Decrypted URL contains mismatching 'request-type-get/post' value (expected GET)");
					}
				}
				// continue with double-checking the resource to be accessed
				final int previousDelimiter = delimiterPos;
				delimiterPos = decryptedQueryString.lastIndexOf(KsWafFilter.INTERNAL_URL_DELIMITER, delimiterPos-1);
				if (delimiterPos == -1 || delimiterPos == decryptedQueryString.length()-1) throw new ServerAttackException("Decrypted URL contains no 'resource-to-be-accessed' and/or 'request-type-get/post' and/or 'form/link' value");
				final String actualResourceToBeAccessed = decryptedQueryString.substring(delimiterPos+1, previousDelimiter);
				if (!additionalFullOrMediumResourceRemoval) {
					// check the resource-to-be-accessed for mismatches,
					// but be carful to also handle cases where folders are accessed, like "folder?a=b" which will be translated to "folder/?a=b" by most web-servers
					boolean mismatch = !actualResourceToBeAccessed.equals(resourceToBeAccessed); // = default check
					if (mismatch) {
						if (resourceToBeAccessed.length() == 0) {
							// it can be such a case where "folder?a=b" was translated to "folder/?a=b", so check that
							if (uriRequested.endsWith("/"+actualResourceToBeAccessed+"/") || uriRequested.startsWith(actualResourceToBeAccessed+"/")) mismatch = false; // = yes, it is such a case where the web-server has translated a folder access, so it is OK and the pure equalness check yielded a false positive
							// also it can be such a case where "/?a=b" was translated into something like "/indes.jsp?a=b", so check that
							if (uriRequested.endsWith("/") && servletPath.endsWith("/"+actualResourceToBeAccessed)) mismatch = false; // = yes, it is such a case where the web-server has translated a welcome page access, so it is OK and the pure equalness check yielded a false positive
						} else if (resourceToBeAccessed.equals(actualResourceToBeAccessed+"/")) {
							// it is a case where    expected=/demoapp/folder1/ vs. actual=/demoapp/folder1   and that's OK and allowed
							mismatch = false;
						}
					}
					if (mismatch) {
						throw new ServerAttackException("Decrypted URL contains mismatching 'resource-to-be-accessed' value (expected="+resourceToBeAccessed+" vs. actual="+actualResourceToBeAccessed+")");
					}
				}

				decryptedQueryString = decryptedQueryString.substring(0, delimiterPos);

				// re-create complete URL
				// BUT DELIBERATELY REMOVE ANY MANUALLY ADDED URL PARAMS AFTER THE ENCRYPTION HAS HAPPENED,
				// since the include mechanism of KsWaf ensures that these manually added url parameters are already
				// assigned to the request object (as request parameters). When we would otherwise include them here too,
				// we would get two same values for each manually added url parameter and therefore count them twice...
				// So to avoid this double counting we avoid adding the manually added url-params here by design...
				if (additionalFullOrMediumResourceRemoval) {
					if (actualResourceToBeAccessed.startsWith(contextPath)) {
						result.append( actualResourceToBeAccessed.substring(contextPath.length()) );
					} else result.append(actualResourceToBeAccessed);
					if (decryptedQuerystring.resourceEndsWithSlash != null && decryptedQuerystring.resourceEndsWithSlash) {
						if (DEBUG) {
							System.out.println("Intermediate result: "+result);
							System.out.println("alreadyUnencryptedPrefixWithParamsRemoved: "+alreadyUnencryptedPrefixWithParamsRemoved);
						}
						if (alreadyUnencryptedPrefixWithParamsRemoved.charAt(alreadyUnencryptedPrefixWithParamsRemoved.length()-1) == '?') {
							result.append(alreadyUnencryptedPrefixWithParamsRemoved, 0, alreadyUnencryptedPrefixWithParamsRemoved.length()-1);
						}
					}
					result.append("?");
				} else {
					result.append(alreadyUnencryptedPrefixWithParamsRemoved);
					if (DEBUG) System.out.println("alreadyUnencryptedPrefixWithParamsRemoved: "+alreadyUnencryptedPrefixWithParamsRemoved);
				}
				if (DEBUG) System.out.println("Decrypted yet without querystring: "+result);
				result.append(decryptedQueryString);
				result.append(alreadyUnencryptedSuffixWithParamsRemoved);
				if (anchor != null) result.append(anchor);
				if (result.indexOf(cryptoDetectionString) > -1) throw new ServerAttackException("URL is encrypted multiple times (possible denial of service attack with endless decryption loop): "+result);
				if (appendQuestionmarkOrAmpersandToLinks) {
					if (result.charAt(result.length()-1) == '&' && result.length() > 1) {
						// crop trailing &
						result.deleteCharAt(result.length()-1);
					}
					// replace also && by & in result to be more compatible
					result = new StringBuilder( result.toString().replaceAll("\\&\\&","&") );
				}
				if (DEBUG) System.out.println("result="+result);
				decryptedQuerystring.decryptedString = result.toString();
				return decryptedQuerystring;
			} catch (IllegalBlockSizeException | UnsupportedEncodingException | NoSuchPaddingException | BadPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
				if (DEBUG) e.printStackTrace();
				throw new ServerAttackException("Unable to decrypt URL: "+e.getMessage());
			} catch (ServerAttackException e) {
				if (DEBUG) e.printStackTrace();
				throw e;
			} catch (RuntimeException e) {
				if (DEBUG) e.printStackTrace();
				throw new ServerAttackException("Unable to decrypt URL: "+e.getMessage());
			}
		} else {
			return null; // returns null when the URL is not encrypted
		}
	}



	public static boolean isMismatch(final List/*<String>*/ expectedValues, final String[] actualSubmittedValues) {
		if (expectedValues == null) throw new NullPointerException("expectedValues must not be null");
		if (actualSubmittedValues == null || actualSubmittedValues.length == 0) return true; //= yes, empty is counted as a mismatch
		if (expectedValues.size() != actualSubmittedValues.length) return true;
		for (int i=0; i<actualSubmittedValues.length; i++) {
			final String actualSubmittedValue = actualSubmittedValues[i];
			final boolean wasThere = expectedValues.remove(actualSubmittedValue);
			if (!wasThere) return true;
		}
		return !expectedValues.isEmpty();
	}



	private static String removeAfterFirstQuestionMark(final String value) {
		if (value == null) return null;
		final int pos = value.indexOf('?');
		if (pos > -1) return value.substring(0, pos+1);
		return value;
	}
	private static String removeBeforeFirstHash(final String value) {
		if (value == null) return null;
		final int pos = value.indexOf('#');
		if (pos > -1) return value.substring(pos);
		return "";
	}


	private static Map/*<String,String[]>*/ convertMapOfListOfStrings2MapOfStringArray(final Map/*<String,List<String>>*/ map) {
		if (map == null) return null;
		final/*<String,String[]>*/ Map result = new HashMap();
		for (final Iterator entries = map.entrySet().iterator(); entries.hasNext();) {
			final Map.Entry entry = (Map.Entry) entries.next();
			result.put( entry.getKey(), ((List)entry.getValue()).toArray(new String[0]) );
		}
		return result;
	}


	private static String formatDateTime(long value) {
		// use the very same format for logging that is used in rule file syntax: RequestDefinitionContainer.FORMAT_TIME
		return new SimpleDateFormat(RequestDefinitionContainer.FORMAT_TIME).format( new Date(value) );
	}


	public static void appendValueToMessage(final StringBuilder logMessage, final String key, final Object value) {
		logMessage.append("\t").append(key).append(" = ").append(value==null?"":value).append("\n");
	}

	private static String removeSensitiveData(final String name, final Object obj, final Matcher sensitiveRequestParamNameMatcherToReuse, final Matcher sensitiveRequestParamNameAndValueUrlMatcherToReuse, final Matcher sensitiveValueMatcherToReuse) {
		if (obj == null) return null;
		String value = obj.toString();
		if (name != null) {
			sensitiveRequestParamNameMatcherToReuse.reset(name);
			if (sensitiveRequestParamNameMatcherToReuse.find()) return SENSITIVE_VALUE_REMOVED;
		}

		sensitiveValueMatcherToReuse.reset(value);
		value = sensitiveValueMatcherToReuse.replaceAll(SENSITIVE_VALUE_REMOVED);

		sensitiveRequestParamNameAndValueUrlMatcherToReuse.reset(value);
		value = sensitiveRequestParamNameAndValueUrlMatcherToReuse.replaceAll(SENSITIVE_VALUE_REMOVED);
		return value;
	}

	/**
	 * Filters the given requestParamterMap for Image Map indicators. These are _.x _.y as coordinates for the map.
	 * This params would fire an attack, cause of not expected values in request.
	 *
	 * @param requestParamaterMap
	 * @return cleaned Set of RequestParams
	 */
	public static Set filterRequestParameterMap(final Set requestParamaterMap) {
		Set cleanedParamSet = new HashSet();

		for (Object param : requestParamaterMap) {
			if (param instanceof String) {
				String newParam = (String) param;
				if (newParam.matches(IMAGE_MAP_EXLUDE_EXPRESSION)) {
					param = newParam.substring(0, newParam.length() - 2);
				}
			}
			cleanedParamSet.add(param);
		}

		return cleanedParamSet;
	}

	/**
	 * Checks a {@link ServletRequest} for it's session is active or timed out or may have deleted
	 *
	 * @param request current Request object
	 * @return {@link Boolean} indicates Session is active
	 */
	public static boolean checkSessionIsActive(final ServletRequest request) {
		boolean active = true;
		if(request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest)request;
			HttpSession session = httpRequest.getSession(false);
			active = session != null && !session.isNew();
		}
		return active;
	}



    /*
    // for local testing only: ==============
    public static final void main(String[] args) throws Exception {
        final SecretKey key = CryptoUtils.generateRandomCryptoKey();
        String encrypted = ResponseUtils.encryptQueryStringInURL("http://www.example.com/demo/hahah?1=2","http://www.example.com/demo/test;jsession=uuuuuu?id=16&huhu=haha#anchor7", "1234567890", key/*,CryptoUtils.createReusableCipher()*);
        encrypted = encrypted.replaceFirst("\\?", "?eins=111&");
        encrypted = encrypted + "&zwei=222";
        System.out.println(encrypted);
        final String decrypted = decryptQueryStringInServletPathWithQueryString(encrypted, "1234567890", key, "/demo/test");
        System.out.println(decrypted);
    }*/


}
