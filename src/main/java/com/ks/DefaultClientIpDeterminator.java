package com.ks;

import com.ks.config.ConfigurationManager;
import com.ks.exceptions.ClientIpDeterminationException;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.pojo.interfaces.ClientIpDeterminator;
import com.ks.utils.ConfigurationUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

public final class DefaultClientIpDeterminator implements ClientIpDeterminator {

	public static final String PARAM_SPLIT_HEADER_VALUE = "ClientIpDeterminatorSplitHeaderValue";

	public static final String PARAM_HEADER_NAME = "ClientIpDetermination";


	private boolean splitHeaderValue = false;
	private String headerName = "";

	public static String extractFirstIP(final String headerFetchedClientIpValue) {
		final int posFirstComma = headerFetchedClientIpValue.indexOf(',');
		if (posFirstComma > 0) {
			return headerFetchedClientIpValue.substring(0, posFirstComma - 1).trim();
		}
		return headerFetchedClientIpValue.trim();
	}


	public void setFilterConfig(final FilterConfig filterConfig) throws FilterConfigurationException { // TODO: use  ConfigurationUtils.extractOptionalConfigValue
		if (filterConfig == null) throw new IllegalArgumentException("filterConfig must not be null");
		final ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
		{
			String value = configManager.getConfigurationValue(PARAM_HEADER_NAME);
			this.headerName = StringUtils.isEmpty(value)? "" : value.trim();
		}
		{
			String value = configManager.getConfigurationValue(PARAM_SPLIT_HEADER_VALUE);
			this.splitHeaderValue = StringUtils.isEmpty(value)? Boolean.FALSE:  Boolean.valueOf(value.trim());
		}
	}

	public String determineClientIp(final HttpServletRequest request) throws ClientIpDeterminationException {
		final String remoteAddr = request.getRemoteAddr();
		if (this.headerName == null || this.headerName.length() == 0) return remoteAddr;
		final String headerFetchedClientIpValue = request.getHeader(this.headerName);
		if (!this.splitHeaderValue) return headerFetchedClientIpValue != null ? headerFetchedClientIpValue : remoteAddr;
		// in case the header value shall be splitted: (required when a cascade of multiple proxies enhances the value to a comma-separated list - in reverse order of traversal (i.e. closest proxy first))
		if (headerFetchedClientIpValue != null && headerFetchedClientIpValue.length() > 0) {
			return extractFirstIP(headerFetchedClientIpValue);
		}
		return remoteAddr;
	}



}