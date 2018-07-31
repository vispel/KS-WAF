package com.ks.container;

import com.ks.exceptions.CustomRequestMatchingException;
import com.ks.loaders.RuleFileLoader;
import com.ks.pojo.BadRequestDefinition;
import com.ks.pojo.Permutation;
import com.ks.pojo.RequestDefinition;
import com.ks.pojo.WordDictionary;
import com.ks.pojo.interfaces.CustomRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public final class WhitelistRequestDefinitionContainer extends RequestDefinitionContainer {

	public WhitelistRequestDefinitionContainer(final RuleFileLoader ruleFileLoader) {
		super(ruleFileLoader, false); // false since Whitelist-Definitions should (for security reasons) *not* utilize non-standard permutation variants
	}


	protected RequestDefinition createRequestDefinition(final boolean enabled, final String identification, final String description, final CustomRequestMatcher customRequestMatcher) {
		return new BadRequestDefinition(enabled, identification, description, customRequestMatcher);
	}
	protected RequestDefinition createRequestDefinition(final boolean enabled, final String identification, final String description, final WordDictionary servletPathPrefilter, final Pattern servletPathPattern, final boolean servletPathPatternNegated) {
		return new BadRequestDefinition(enabled, identification, description, servletPathPrefilter, servletPathPattern, servletPathPatternNegated);
	}


	protected void extractAndRemoveSpecificProperties(final /*BadRequestDefinition statt RequestDefinition*/RequestDefinition requestDefinition, final Properties properties) {
	}

	public final boolean isWhitelistMatch(HttpServletRequest request, String servletPath, String contextPath, String pathInfo, String pathTranslated, String clientAddress, String remoteHost, int remotePort, String remoteUser, String authType, String scheme, String method, String protocol, String mimeType, String encoding, int contentLength, Map headerMapVariants, String url, String uri, String serverName, int serverPort, String localAddr, String localName, int localPort, String country, Map cookieMapVariants, String requestedSessionId, Permutation queryStringVariants, Map parameterMapVariants, Map parameterMapExcludingInternalParams,
                                          final boolean treatNonMatchingServletPathAsMatch) throws CustomRequestMatchingException {
		final RequestDefinition requestDefinition = super.getMatchingRequestDefinition(request,
				servletPath,
				contextPath,
				pathInfo,
				pathTranslated,
				clientAddress,
				remoteHost,
				remotePort,
				remoteUser,
				authType,
				scheme,
				method,
				protocol,
				mimeType,
				encoding,
				contentLength,
				headerMapVariants,
				url,
				uri,
				serverName,
				serverPort,
				localAddr,
				localName,
				localPort,
				country,
				cookieMapVariants,
				requestedSessionId,
				queryStringVariants,
				parameterMapVariants, parameterMapExcludingInternalParams,
				false, treatNonMatchingServletPathAsMatch);
		return requestDefinition != null;
	}

}