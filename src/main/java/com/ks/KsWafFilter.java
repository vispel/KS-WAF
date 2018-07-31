package com.ks;


import com.ks.attack.Attack;
import com.ks.attack.AttackHandler;
import com.ks.attack.AttackLogger;
import com.ks.config.ConfigurationManager;
import com.ks.container.ContentModificationExcludeDefinitionContainer;
import com.ks.container.FormFieldMaskingExcludeDefinitionContainer;
import com.ks.container.RequestDefinitionContainer;
import com.ks.container.SimpleDefinitionContainer;
import com.ks.crypto.CryptoKeyAndSalt;
import com.ks.crypto.ParameterAndFormProtection;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.exceptions.RuleLoadingException;
import com.ks.exceptions.ServerAttackException;
import com.ks.exceptions.StopFilterProcessingException;
import com.ks.filter.ReplyMessageOrStatusCode;
import com.ks.filter.RuleDefinitions;
import com.ks.loaders.RuleFileLoader;
import com.ks.pojo.*;
import com.ks.pojo.interfaces.ClientIpDeterminator;
import com.ks.request.RequestDetails;
import com.ks.trackers.DenialOfServiceLimitTracker;
import com.ks.trackers.HttpStatusCodeTracker;
import com.ks.trackers.SessionCreationTracker;
import com.ks.utils.*;
import com.ks.wrapper.RequestWrapper;
import com.ks.wrapper.ResponseWrapper;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;

import static com.ks.utils.ParamConsts.*;

public class KsWafFilter implements Filter {

	private FilterConfig filterConfig;
	private AttackHandler attackHandler;
	private final ContentInjectionHelper contentInjectionHelper = new ContentInjectionHelper();
	private final Object o_reloadRulesOnNextRequest = new Object();
	private boolean reloadRulesOnNextRequest;
	private boolean restartCompletelyOnNextRequest = true;
	private boolean isHavingEnabledQueryStringCheckingRules;
	private boolean isHavingEnabledRequestParameterCheckingRules;
	private boolean isHavingEnabledHeaderCheckingRules;
	private boolean isHavingEnabledCookieCheckingRules;
	private boolean debug;
	private boolean showTimings;
	private boolean logSessionValuesOnAttack;
	private boolean invalidateSessionOnAttack;
	private boolean logVerboseForDevelopmentMode;
	private boolean extraEncryptedValueHashProtection;
	private boolean appendSessionIdToLinks;
	private boolean jmsUsed;
	private boolean flushResponse = true;
	private boolean forceEntranceThroughEntryPoints;
	private boolean blockResponseHeadersWithCRLF;
	private boolean blockRequestsWithUnknownReferrer;
	private boolean blockRequestsWithMissingReferrer;
	private boolean blockRequestsWithDuplicateHeaders;
	private boolean blockNonLocalRedirects;
	private boolean blockInvalidEncodedQueryString;
	private boolean useFullPathForResourceToBeAccessedProtection;
	private boolean additionalFullResourceRemoval;
	private boolean additionalMediumResourceRemoval;
	private boolean maskAmpersandsInLinkAdditions;
	private boolean hiddenFormFieldProtection;
	private boolean selectBoxProtection;
	private boolean checkboxProtection;
	private boolean radiobuttonProtection;
	private boolean selectBoxValueMasking;
	private boolean checkboxValueMasking;
	private boolean radiobuttonValueMasking;
	private boolean reuseSessionContent;
	private boolean hideInternalSessionAttributes;
	private boolean randomizeHoneyLinksOnEveryRequest;
	private boolean pdfXssProtection;
	private boolean treatNonMatchingServletPathAsMatchForWhitelistRules;
	private boolean blockMultipartRequestsForNonMultipartForms;
	private ClientIpDeterminator clientIpDeterminator;
	private boolean catchAll;

	private final RuleDefinitions ruleDefinitions = new RuleDefinitions();


	private String applicationName;
	private String redirectWelcomePage;
	private String requestCharacterEncoding;
	private String learningModeAggregationDirectory;
	private Class ruleFileLoaderClass;

	public static int customerIdentifier;
	private int forcedSessionInvalidationPeriodMinutes;
	private int blockRepeatedRedirectsThreshold;
	private int housekeepingIntervalMinutes;
	private int blockPeriodMinutes;
	private long ruleFileReloadingIntervalMillis;
	private long nextRuleReloadingTime;
	private long configReloadingIntervalMillis;
	private long nextConfigReloadingTime;
	private int resetPeriodMinutesAttack;
	private int resetPeriodMinutesRedirectThreshold;
	private String honeyLinkPrefix;
	private String honeyLinkSuffix;

	private short honeyLinkMaxPerPage;
	private final Set allowedRequestMimeTypesLowerCased = new HashSet();

	private HttpStatusCodeTracker httpStatusCodeCounter;
	private SessionCreationTracker sessionCreationCounter;
	private DenialOfServiceLimitTracker denialOfServiceLimitCounter;

	/*Messages*/
	private final ReplyMessageOrStatusCode filterReplyMessageOrStatusCode = new ReplyMessageOrStatusCode();

	public KsWafFilter() {
		System.out.println(Version.tagLine());
	}

	@Override
	public void init(FilterConfig filterConfig)  {
		this.filterConfig = filterConfig;
		try {
			restartCompletelyWhenRequired();
		} catch (Exception e) {
			logLocal("Unable to initialize security filter", e);
		}
	}

	private final Object restartCompletelyWhenRequired = new Object();

	private void restartCompletelyWhenRequired() throws UnavailableException {
		if (this.restartCompletelyOnNextRequest) {
			synchronized (this.restartCompletelyWhenRequired) {
				if (this.restartCompletelyOnNextRequest) {
					try {
						checkRequirementsAndInitialize();
						this.restartCompletelyOnNextRequest = false;
						logLocal("Initialized protection layer");
					} catch (RuntimeException | UnavailableException e) {
						this.restartCompletelyOnNextRequest = true;
						try {
							destroy();
						} catch (Exception e2) {
							e2.printStackTrace();
						}
						e.printStackTrace();
						throw e;
					}
                }
			}
		}
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// check if total-exclude match:
		if (this.ruleDefinitions.getTotalExcludeDefinitions() != null &&this.ruleDefinitions.getTotalExcludeDefinitions().hasEnabledDefinitions() && request instanceof HttpServletRequest) {
			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			final String servletPath = httpRequest.getServletPath();
			final String requestURI = httpRequest.getRequestURI();

			if ( this.ruleDefinitions.getTotalExcludeDefinitions().isTotalExclude(servletPath,requestURI) ) {
				// TOTAL EXCLUDE: SO DON'T DO ANY SECURITY STUFF HERE, JUST LET THE REQUEST PASS - AT THE USER'S OWN WILL AND RISK
				chain.doFilter(request, response); // TODO: log this total-exlucde access in the log-file for revision safeness !!
				return;
			}
		}
		chain.doFilter(request, response); //clean after
		// apply full security filter stuff (i.e. NO total-exclude):
		if (this.catchAll) {
			try {
				internalDoFilter(request, response, chain);
			} catch (Exception e) {
				logLocal("Uncaught exception: "+e.getClass().getName()+": "+e.getMessage()); //
			}
		} else {
			internalDoFilter(request, response, chain);
		}
	}

	@Override
	public void destroy() {

		if (this.httpStatusCodeCounter != null) {
			try {
				this.httpStatusCodeCounter.destroy();
			} catch (Exception e) {
				logLocal("Exception during destroy: "+e);
			}
		}
		if (this.sessionCreationCounter != null) {
			try {
				this.sessionCreationCounter.destroy();
			} catch (Exception e) {
				logLocal("Exception during destroy: "+e);
			}
		}
		if (this.denialOfServiceLimitCounter != null) {
			try {
				this.denialOfServiceLimitCounter.destroy();
			} catch (Exception e) {
				logLocal("Exception during destroy: "+e);
			}
		}
		if (this.attackHandler != null) {
			try {
				this.attackHandler.destroy(); // = this also destroys the ClientBlacklist which lives only inside of the AttackHandler as an implementation detail of the AttackHandler
			} catch (Exception e) {
				logLocal("Exception during destroy: "+e);
			}
		}
		if (this.jmsUsed) try {
			JmsUtils.closeQuietly(true);
		} catch (Exception e) {
			// log only (so that not found JMS classes when JMS is not used don't make problems
			logLocal("JMS utility not destroyed",e);
		}
		this.restartCompletelyOnNextRequest = true;
	}


	private void logLocal(String msg) {
		logLocal(msg, null);
	}

	private void logLocal(String msg, Exception e) {
		if (e != null) {
			if ((this.filterConfig != null) && (this.filterConfig.getServletContext() != null)) {
				this.filterConfig.getServletContext().log(msg, e);
			} else {
				System.out.println(msg + ": " + e);
			}
		} else if ((this.filterConfig != null) && (this.filterConfig.getServletContext() != null)) {
			this.filterConfig.getServletContext().log(msg);
		} else {
			System.out.println(msg);
		}
	}

	private void checkRequirementsAndInitialize() throws UnavailableException {
		{
			if (this.filterConfig == null)
				throw new IllegalStateException("Filter must be initialized via web container before 'init()' this method may be called");

			try {
				destroy();
			} catch (RuntimeException e) {
				logLocal("Unable to destroy configuration during (re-)initialization", e);
			}

			final ConfigurationManager configManager = getConfigurationManager();

			assert configManager != null;

			boolean initJMS = false; // might be set to true during initialization, which then indicates that we should init JMS (i.e. start listening) at the end of config loading (see below)

			// LOAD THE CONFIG-MISSING-STUFF

			// Load config: Configuration missing reply HTTP status code or message resource - OPTIONAL
			filterReplyMessageOrStatusCode.loadConfigurationMissingReplyConfig(configManager);

			// init to false
			isHavingEnabledQueryStringCheckingRules = false;
			isHavingEnabledRequestParameterCheckingRules = false;
			isHavingEnabledHeaderCheckingRules = false;
			isHavingEnabledCookieCheckingRules = false;

			// THE REGULAR STUFF
			// Load optional config

            // debug flag - OPTIONAL
			{
				this.debug = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_DEBUG);
			}

			// timing flag - OPTIONAL
			{
				this.showTimings = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_SHOW_TIMINGS);
			}

			// redirect welcome page - OPTIONAL
			{
				this.redirectWelcomePage = FilterLoadConfigUtils.loadConfig(configManager,PARAM_REDIRECT_WELCOME_PAGE, "");
				if (this.debug) logLocal("Redirect welcome page: " + this.redirectWelcomePage);
			}

			//session timeout redirect page - OPTIONAL
			{
				String sessionTimeoutRedirectPage = FilterLoadConfigUtils.loadConfig(configManager, PARAM_SESSION_TIMEOUT_REDIRECT_PAGE, "");
				if (this.debug) logLocal("Session Timeout Redirect page: " + sessionTimeoutRedirectPage);
			}


			//request character encoding - OPTIONAL
			{
				this.requestCharacterEncoding = FilterLoadConfigUtils.loadConfig(configManager,PARAM_SESSION_TIMEOUT_REDIRECT_PAGE, DEFAULT_CHARACTER_ENCODING);
				if (this.debug) logLocal("Request character encoding: " + this.requestCharacterEncoding);
			}

			//log session values on attack - OPTIONAL
			{
				this.logSessionValuesOnAttack = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_LOG_SESSION_VALUES_ON_ATTACK);
			}

			// learning mode aggregation directory - OPTIONAL
			{
				this.learningModeAggregationDirectory = FilterLoadConfigUtils.loadConfig(configManager,PARAM_LEARNING_MODE_AGGREGATION_DIRECTORY,"");
				if (this.debug) logLocal("Learning mode aggregation directory: " + this.learningModeAggregationDirectory);
			}

			//application name - OPTIONAL
			{
				this.applicationName = FilterLoadConfigUtils.loadConfig(configManager,PARAM_APPLICATION_NAME,"DEFAULT");
				if (this.debug) logLocal("Application name: " + this.applicationName);
			}

			// flush response - OPTIONAL
			{
				this.flushResponse = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_FLUSH_RESPONSE);
				if (this.debug) logLocal("Flush response: " + this.flushResponse);
			}

			// invalidate session on attack - OPTIONAL
			{
				this.invalidateSessionOnAttack = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_INVALIDATE_SESSION_ON_ATTACK);
				if (this.debug) logLocal("Invalidate session on attack: " + this.invalidateSessionOnAttack);
			}

			// log verbose for development mode - OPTIONAL
			{
				this.logVerboseForDevelopmentMode =FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_LOG_VERBOSE_FOR_DEVELOPMENT_MODE);
				if (this.debug) logLocal("Log verbose for development mode: " + this.logVerboseForDevelopmentMode);
			}

			loadRuleFileLoader(configManager);
			loadClientIpDeteminator(configManager);
			loadHouseKeepingInterval(configManager);
			loadRuleFileReloadInterval(configManager);
			loadConfigReloadInterval(configManager);
			loadBlockPeriodMinutes(configManager);
			loadResetPeriodMinutesAfterAttack(configManager);
			loadResetPeriodMinutesForSessionCreation(configManager);
			loadResetPeriodMinutesForBadResponseCode(configManager);
			loadResetPeriodForRedirectThreshold(configManager);
			loadBlockRepeatedRedirectsThreshold(configManager);

			boolean logClientUserData = isLogClientUserData(configManager);

			// block attacking clients threshold - OPTIONAL
			{
				final AttackLogger attackLogger = getAttackLogger(configManager);
				loadBlockAttackingClientsThreshold(configManager, logClientUserData, attackLogger);
			}

			// entry-point file path - OPTIONAL
			{
				String entryPointDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_ENTRY_POINT_FILES,"entry-points");
				loadRuleDefinition(entryPointDefinitionsValue);
			}
			// optimization-hint file path - OPTIONAL
			{
				String optimizationHintDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_OPTIMIZATION_HINT_FILES,"optimization-hints");
				loadRuleDefinition(optimizationHintDefinitionsValue);
			}

			// renewSession-point file path - OPTIONAL
			{
				String renewSessionPointDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_RENEW_SESSION_AND_TOKEN_POINT_FILES,"renew-session-and-token-points");
				loadRuleDefinition(renewSessionPointDefinitionsValue);
			}


			// incomingProtectionExclude-point file path - OPTIONAL
			{
				String incomingProtectionExcludeDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_INCOMING_PROTECTION_EXCLUDE_FILES, "incoming-protection-excludes");
				loadRuleDefinition(incomingProtectionExcludeDefinitionsValue);
			}

			// response-modifications file path - OPTIONAL
			{
				String responseModificationDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_RESPONSE_MODIFICATION_FILES, RESPONSE_MODIFICATIONS_DEFAULT);
				loadRuleDefinition(responseModificationDefinitionsValue);
			}

			// white-list request file path - OPTIONAL
			{
				String whiteListDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_WHITELIST_REQUESTS_FILES, "whitelist-requests");
				loadRuleDefinition(whiteListDefinitionsValue);
			}

			// bad-request file path - OPTIONAL
			{
				String badRequestDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_BAD_REQUEST_FILES, "bad-requests");
				loadRuleDefinition(badRequestDefinitionsValue);
			}

			// DoS-Limit file path - OPTIONAL
			{
				String denialOfServiceLimitsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_DOS_LIMIT_FILES, "denial-of-service-limits");
				loadRuleDefinition(denialOfServiceLimitsValue);
			}

			// total exclude request file path - OPTIONAL
            {
                String totalExcludeDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_TOTAL_EXCLUDE_FILES, "total-excludes");
                loadDefinition(totalExcludeDefinitionsValue);
            }

			// content modification exclude request file path - OPTIONAL
            {
                String contentModificationExcludeDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_CONTENT_MODIFICATION_EXCLUDE_FILES, MODIFICATION_EXCLUDES_DEFAULT);
                loadDefinition(contentModificationExcludeDefinitionsValue);
            }

			// size limit request file path - OPTIONAL
            {
                String sizeLimitDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_SIZE_LIMIT_FILES, "size-limits");
                loadDefinition(sizeLimitDefinitionsValue);
            }

			// multipart size limit request file path - OPTIONAL
            {
                String multipartSizeLimitDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_MULTIPART_SIZE_LIMIT_FILES, "multipart-size-limits");
                loadDefinition(multipartSizeLimitDefinitionsValue);
            }

			//decoding permutation file path - OPTIONAL
            {
                String decodingPermutationDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_DECODING_PERMUTATION_FILES, "decoding-permutations");
                loadDefinition(decodingPermutationDefinitionsValue);
            }

			//form field masking exclude request file path - OPTIONAL
            {
                String decodingPermutationDefinitionsValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_PATH_TO_FORM_FIELD_MASKING_EXCLUDE_FILES, "form-field-masking-excludes");
                loadDefinition(decodingPermutationDefinitionsValue);
            }


			// ordering is important here, since the features depend on each other

			// secret token link injection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_SECRET_TOKEN_LINK_INJECTION);
				this.contentInjectionHelper.setInjectSecretTokenIntoLinks(flag);
				if (this.debug) logLocal("Apply secret token link injection: " + flag);
			}

			// encrypt query strings - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_ENCRYPT_QUERY_STRINGS);
				this.contentInjectionHelper.setEncryptQueryStringInLinks(flag);
				if (this.debug) logLocal("Encrypt query strings: " + flag);
				// this feature depends on another feature:
				if (this.contentInjectionHelper.isEncryptQueryStringInLinks() && !this.contentInjectionHelper.isInjectSecretTokenIntoLinks()) {
					throw new UnavailableException("When 'query string encryption' is activated the feature 'secret token link injection' must be activated also");
				}
			}


			// extra encrypted value hash protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_HASH_PROTECTION);
				this.extraEncryptedValueHashProtection = flag;
				if (this.debug) logLocal("Apply extra encrypted value hash protection: " + flag);
				// this feature depends on another feature:
				if (this.extraEncryptedValueHashProtection && !this.contentInjectionHelper.isEncryptQueryStringInLinks()) {
					throw new UnavailableException("When 'extra encrypted value hash protection' is activated the feature 'query string encryption' must be activated also");
				}
			}
			// extra encrypted full path resource protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_FULL_PATH_PROTECTION);
				this.useFullPathForResourceToBeAccessedProtection = flag;
				if (this.debug) logLocal("Apply extra encrypted full path resource protection: " + flag);
				// this feature depends on another feature(s):
				if (this.useFullPathForResourceToBeAccessedProtection) {
					if (!this.contentInjectionHelper.isEncryptQueryStringInLinks())
						throw new UnavailableException("When 'extra encrypted full path resource protection' is activated the feature 'query string encryption' must be activated also");
				}
			}

			//extra encrypted medium path resource removal - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_MEDIUM_PATH_REMOVAL);
				this.additionalMediumResourceRemoval = flag;
				this.contentInjectionHelper.setExtraMediumPathRemoval(flag);
				if (this.debug) logLocal("Apply extra encrypted medium path resource removal: " + flag);
				// this feature depends on another feature:
				if (this.additionalMediumResourceRemoval && !this.contentInjectionHelper.isEncryptQueryStringInLinks()) {
					throw new UnavailableException("When 'extra encrypted medium path resource removal' is activated the feature 'query string encryption' must be activated also");
				}
			}
			// extra encrypted full path resource removal - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_FULL_PATH_REMOVAL);
				this.additionalFullResourceRemoval = flag;
				this.contentInjectionHelper.setExtraFullPathRemoval(flag);
				if (this.debug) logLocal("Apply extra encrypted full path resource removal: " + flag);
				// this feature depends on another feature:
				if (this.additionalFullResourceRemoval) {
					if (!this.useFullPathForResourceToBeAccessedProtection)
						throw new UnavailableException("When 'extra encrypted full path resource removal' is activated the feature 'extra encrypted full path resource protection' must be activated also");
					if (!this.contentInjectionHelper.isEncryptQueryStringInLinks())
						throw new UnavailableException("When 'extra encrypted full path resource removal' is activated the feature 'query string encryption' must be activated also");
					if (this.additionalMediumResourceRemoval)
						throw new UnavailableException("When 'extra encrypted full path resource removal' is activated the feature 'extra encrypted medium path resource removal' makes no sense and should be deactivated");
				}
			}

			plausibilityCheck();

			// Block multipart requests for non-multipart forms - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_MULTIPART_REQUESTS_FOR_NON_MULTIPART_FORMS);
				this.blockMultipartRequestsForNonMultipartForms = flag;
				if (this.debug) logLocal("Block multipart requests for non-multipart forms: " + flag);
				// this feature depends on another feature:
				if (this.blockMultipartRequestsForNonMultipartForms && !this.contentInjectionHelper.isEncryptQueryStringInLinks()) {
					throw new UnavailableException("When 'block multipart requests for non-multipart forms' is activated the feature 'query string encryption' must be activated also");
				}
			}
			// parameter and form protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_PARAMETER_AND_FORM_PROTECTION);
				this.contentInjectionHelper.setProtectParametersAndForms(flag);
				if (this.debug) logLocal("Apply parameter and form protection: " + flag);
				// this feature depends on another feature:
				if (this.contentInjectionHelper.isProtectParametersAndForms() && !this.contentInjectionHelper.isEncryptQueryStringInLinks()) {
					throw new UnavailableException("When 'parameter and form protection' is activated the feature 'query string encryption' must be activated also");
				}
			}
			// Extra strict parameter checking for encrypted links - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_STRICT_PARAMETER_CHECKING_FOR_ENCRYPTED_LINKS);
				this.contentInjectionHelper.setExtraStrictParameterCheckingForEncryptedLinks(flag);
				if (this.debug) logLocal("Extra strict parameter checking for encrypted links: " + flag);
				// this feature depends on another feature:
				if (this.contentInjectionHelper.isExtraStrictParameterCheckingForEncryptedLinks() && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra strict parameter checking for encrypted links' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			// extra disabled form field protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_DISABLED_FORM_FIELD_PROTECTION);
				this.contentInjectionHelper.setExtraProtectDisabledFormFields(flag);
				if (this.debug) logLocal("Apply extra disabled form field protection: " + flag);
				// this feature depends on another feature:
				if (this.contentInjectionHelper.isExtraProtectDisabledFormFields() && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra disabled form field protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			//extra readonly form field protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_READONLY_FORM_FIELD_PROTECTION);
				this.contentInjectionHelper.setExtraProtectReadonlyFormFields(flag);
				if (this.debug) logLocal("Apply extra readonly form field protection: " + flag);
				// this feature depends on another feature:
				if (this.contentInjectionHelper.isExtraProtectReadonlyFormFields() && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra readonly form field protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			// extra request-param value count protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_REQUEST_PARAM_VALUE_COUNT_PROTECTION);
				this.contentInjectionHelper.setExtraProtectRequestParamValueCount(flag);
				if (this.debug) logLocal("Apply extra request-param value count protection: " + flag);
				// this feature depends on another feature:
				if (this.contentInjectionHelper.isExtraProtectRequestParamValueCount() && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra request-param value count protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			// extra hidden form field protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_HIDDEN_FORM_FIELD_PROTECTION);
				this.hiddenFormFieldProtection = flag;
				if (this.debug) logLocal("Apply extra hidden form field protection: " + flag);
				// this feature depends on another feature:
				if (flag && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra hidden form field protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			// extra selectbox protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_SELECTBOX_PROTECTION);
				this.selectBoxProtection = flag;
				if (this.debug) logLocal("Apply extra selectbox protection: " + flag);
				// this feature depends on another feature:
				if (flag && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra selectbox protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			// extra checkbox protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_CHECKBOX_PROTECTION);
				this.checkboxProtection = flag;
				if (this.debug) logLocal("Apply extra checkbox protection: " + flag);
				// this feature depends on another feature:
				if (flag && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra checkbox protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}
			// extra radiobutton protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_RADIOBUTTON_PROTECTION);
				this.radiobuttonProtection = flag;
				if (this.debug) logLocal("Apply extra radiobutton protection: " + flag);
				// this feature depends on another feature:
				if (flag && !this.contentInjectionHelper.isProtectParametersAndForms()) {
					throw new UnavailableException("When 'extra radiobutton protection' is activated the feature 'parameter and form protection' must be activated also");
				}
			}


			// extra selectbox value masking - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_SELECTBOX_VALUE_MASKING);
				this.selectBoxValueMasking = flag;
				if (this.debug) logLocal("Apply extra selectbox value masking: " + flag);
				// this feature depends on another feature(s):
				if (flag) {
					if (!this.contentInjectionHelper.isProtectParametersAndForms())
						throw new UnavailableException("When 'extra selectbox value masking' is activated the feature 'parameter and form protection' must be activated also");
					if (!this.selectBoxProtection)
						throw new UnavailableException("When 'extra selectbox value masking' is activated the feature 'extra selectbox protection' must be activated also");
				}
			}
			//  extra checkbox value masking - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_CHECKBOX_VALUE_MASKING);
				this.checkboxValueMasking = flag;
				if (this.debug) logLocal("Apply extra checkbox value masking: " + flag);
				// this feature depends on another feature(s):
				if (flag) {
					if (!this.contentInjectionHelper.isProtectParametersAndForms())
						throw new UnavailableException("When 'extra checkbox value masking' is activated the feature 'parameter and form protection' must be activated also");
					if (!this.checkboxProtection)
						throw new UnavailableException("When 'extra checkbox value masking' is activated the feature 'extra checkbox protection' must be activated also");
				}
			}
			//  extra radiobutton value masking - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_EXTRA_RADIOBUTTON_VALUE_MASKING);
				this.radiobuttonValueMasking = flag;
				if (this.debug) logLocal("Apply extra radiobutton value masking: " + flag);
				// this feature depends on another feature(s):
				if (flag) {
					if (!this.contentInjectionHelper.isProtectParametersAndForms())
						throw new UnavailableException("When 'extra radiobutton value masking' is activated the feature 'parameter and form protection' must be activated also");
					if (!this.radiobuttonProtection)
						throw new UnavailableException("When 'extra radiobutton value masking' is activated the feature 'extra radiobutton protection' must be activated also");
				}
			}


			// treat non-matchig of servletPath as a match for whitelist rules - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_TREAT_NON_MATCHING_SERVLET_PATH_AS_MATCH_FOR_WHITELIST_RULES);
				this.treatNonMatchingServletPathAsMatchForWhitelistRules = flag;
				if (this.debug) logLocal("Treat non-matchig of servletPath as a match for whitelist rules: " + flag);
			}

			// append session-id to links - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_APPEND_SESSIONID_TO_LINKS);
				this.appendSessionIdToLinks = flag;
				if (this.debug) logLocal("Append session-id to lnks: " + flag);
			}


			// use tuned block parser - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_USE_TUNED_BLOCK_PARSER);
				this.contentInjectionHelper.setUseTunedBlockParser(flag);
				if (this.debug) logLocal("Use tuned block parser: " + flag);
			}

			// use response buffering - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_USE_RESPONSE_BUFFERING);
				this.contentInjectionHelper.setUseResponseBuffering(flag);
				if (this.debug) logLocal("Use response buffering: " + flag);
			}

			//block invalid encoded query string - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_INVALID_ENCODED_QUERY_STRING);
				this.blockInvalidEncodedQueryString = flag;
				if (this.debug) logLocal("Block invalid encoded query string: " + flag);
			}


			// handle uncaught exceptions - OPTIONAL
			{
				this.catchAll = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_HANDLE_UNCAUGHT_EXCEPTIONS);
				if (this.debug) logLocal("Handle uncaught exceptions: " + catchAll);
			}

			// reuse session content - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_REUSE_SESSION_CONTENT);
				this.reuseSessionContent = flag;
				if (this.debug) logLocal("Reuse session content: " + flag);
			}

			// hide internal session attributes - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_HIDE_INTERNAL_SESSION_ATTRIBUTES);
				this.hideInternalSessionAttributes = flag;
				if (this.debug) logLocal("Hide internal session attributes: " + flag);
			}

			// Allowed request mime types - OPTIONAL
			{
				String value = FilterLoadConfigUtils.loadConfig(configManager,PARAM_HIDE_INTERNAL_SESSION_ATTRIBUTES,"application/x-www-form-urlencoded,multipart/form-data,text/plain,text/xml,application/xml");
				this.allowedRequestMimeTypesLowerCased.clear();
				for (final StringTokenizer tokenizer = new StringTokenizer(value.toLowerCase().trim(), ","); tokenizer.hasMoreTokens(); ) {
					final String token = tokenizer.nextToken().trim();
					if (token.length() > 0) this.allowedRequestMimeTypesLowerCased.add(token);
				}
				if (this.debug) logLocal("Allowed request mime types: " + this.allowedRequestMimeTypesLowerCased);
			}

			// PDF XSS protection - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_PDF_XSS_PROTECTION);
				this.pdfXssProtection = flag;
				if (this.debug) logLocal("PDF XSS protection: " + flag);
			}

			// honeylink max per page - OPTIONAL
			{
				String value = FilterLoadConfigUtils.loadConfig(configManager,PARAM_HONEYLINK_MAX_PER_RESPONSE,"0");
				try {
					this.honeyLinkMaxPerPage = Short.parseShort(value.trim());
					if (this.honeyLinkMaxPerPage < 0)
						throw new UnavailableException("Configured 'honeylink max per response' must not be negative: " + value);
				} catch (NumberFormatException e) {
					throw new UnavailableException("Unable to number-parse (short) configured 'honeylink max per response': " + value);
				}
			}

			// honeylink prefix and sufix- OPTIONAL
			{
				this.honeyLinkPrefix = FilterLoadConfigUtils.loadConfig(configManager,PARAM_HONEYLINK_PREFIX,"");
				this.honeyLinkSuffix = FilterLoadConfigUtils.loadConfig(configManager,PARAM_HONEYLINK_SUFFIX,"");
				if (this.debug) logLocal("Honeylink prefix: " + this.honeyLinkPrefix +" and honeylink suffix: " + this.honeyLinkSuffix);
			}

			// randomize honeylinks on every response - OPTIONAL
			{
				final boolean flag = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_RANDOMIZE_HONEYLINKS_ON_EVERY_RESPONSE);
				this.randomizeHoneyLinksOnEveryRequest = flag;
				if (this.debug) logLocal("Randomize honeylinks on every response: " + flag);
			}

			//forced session invalidation period - OPTIONAL
			{
				String value = FilterLoadConfigUtils.loadConfig(configManager,PARAM_FORCED_SESSION_INVALIDATION_PERIOD_MINUTES,"900");
				try {
					this.forcedSessionInvalidationPeriodMinutes = Integer.parseInt(value.trim());
					if (this.forcedSessionInvalidationPeriodMinutes < 0)
						throw new UnavailableException("Configured HTTP status code to send as reply to attacks must not be negative: " + value);
				} catch (NumberFormatException e) {
					throw new UnavailableException("Unable to number-parse configured 'forced session invalidation period': " + value);
				}
			}

			//  force entrance through entry-points flag - OPTIONAL
			{
				this.forceEntranceThroughEntryPoints = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_FORCE_ENTRANCE_THROUGH_ENTRY_POINTS);
				if (this.debug) logLocal("Force entrance through entry-points: " + this.forceEntranceThroughEntryPoints);
			}

			// block response headers with CRLF flag - OPTIONAL
			{
				this.blockResponseHeadersWithCRLF = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_RESPONSE_HEADERS_WITH_CRLF);
				if (this.debug) logLocal("Block response headers with CRLF: " + this.blockResponseHeadersWithCRLF);
			}

			// block requests with unknown referrer - OPTIONAL
			{
				this.blockRequestsWithUnknownReferrer = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_REQUESTS_WITH_UNKNOWN_REFERRER);
				if (this.debug) logLocal("Block requests with unknown referrer: " + this.blockRequestsWithUnknownReferrer);
			}
			//block requests with missing referrer - OPTIONAL
			{
				this.blockRequestsWithMissingReferrer = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_REQUESTS_WITH_MISSING_REFERRER);
				if (this.debug) logLocal("Block requests with missing referrer: " + this.blockRequestsWithMissingReferrer);
			}

			//block requests with duplicate headers - OPTIONAL
			{
				this.blockRequestsWithDuplicateHeaders = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_REQUESTS_WITH_DUPLICATE_HEADERS);
				if (this.debug)
					logLocal("Block requests with duplicate headers: " + this.blockRequestsWithDuplicateHeaders);
			}

			// block non-local redirects - OPTIONAL
			{
				this.blockNonLocalRedirects = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_BLOCK_NON_LOCAL_REDIRECTS);
				if (this.debug) logLocal("Block non-local redirects: " + this.blockNonLocalRedirects);
			}

			// mask ampersands in link additions - OPTIONAL
			{
				this.maskAmpersandsInLinkAdditions = FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_MASK_AMPERSANDS_IN_LINK_ADDITIONS);
				if (this.debug) logLocal("Mask ampersands in link additions: " + this.maskAmpersandsInLinkAdditions);
			}

			// INIT JMS IF REQUIRED TO DO SO
			if (this.jmsUsed) {
				JmsUtils.closeQuietly(false); // to be fresh
			}
		}
	}

	private void plausibilityCheck() throws UnavailableException {
		if (this.useFullPathForResourceToBeAccessedProtection) {
			if (!this.additionalMediumResourceRemoval && !this.additionalFullResourceRemoval)
				throw new UnavailableException("When 'extra encrypted full path resource protection' is activated either the feature 'extra encrypted medium path resource removal' or 'extra encrypted full path resource removal' must be activated also");
		}
		if (this.additionalMediumResourceRemoval && this.additionalFullResourceRemoval) {
			throw new UnavailableException("The features 'extra encrypted medium path resource removal' and 'extra encrypted full path resource removal' must not be activated both (does not make sense)");
		}
	}

	private void loadBlockAttackingClientsThreshold(ConfigurationManager configManager, boolean logClientUserData, AttackLogger attackLogger) throws UnavailableException {
		String blockAttackingClientsThresholdValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_BLOCK_ATTACKING_CLIENTS_THRESHOLD,"0");
		try {
			this.attackHandler = new AttackHandler(attackLogger, Integer.parseInt(blockAttackingClientsThresholdValue), housekeepingIntervalMinutes * 60 * 1000,
					blockPeriodMinutes * 60 * 1000, resetPeriodMinutesAttack * 60 * 1000,
					resetPeriodMinutesRedirectThreshold * 60 * 1000,
					this.learningModeAggregationDirectory, this.applicationName, this.logSessionValuesOnAttack, this.invalidateSessionOnAttack,
					this.blockRepeatedRedirectsThreshold, this.logVerboseForDevelopmentMode, logClientUserData);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'block attacking clients threshold': " + blockAttackingClientsThresholdValue);
		}
		assert this.attackHandler != null;
		if (this.debug) logLocal("Attack handler with block attacking clients threshold: " + this.attackHandler.getBlockAttackingClientsThreshold());
	}

	private AttackLogger getAttackLogger(ConfigurationManager configManager) throws UnavailableException {
		final AttackLogger attackLogger; // only used below inside the AttackHandler
		String attackLoggerValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_ATTACK_LOGGER,"com.ks.DefaultAttackLogger");
		if (attackLoggerValue.isEmpty())
			throw new UnavailableException("Filter init-param is empty: " + PARAM_ATTACK_LOGGER);
		try {
			attackLogger = (AttackLogger) Class.forName(attackLoggerValue).newInstance();
			attackLogger.setFilterConfig(filterConfig);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Unable to find geo-locator class (" + attackLoggerValue + "): " + e.getMessage());
		} catch (InstantiationException e) {
			throw new UnavailableException("Unable to instantiate geo-locator (" + attackLoggerValue + "): " + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new UnavailableException("Unable to access geo-locator (" + attackLoggerValue + "): " + e.getMessage());
		} catch (FilterConfigurationException e) {
			throw new UnavailableException("Unable to configure geo-locator (" + attackLoggerValue + "): " + e.getMessage());
		} catch (RuntimeException e) {
			throw new UnavailableException("Unable to use geo-locator (" + attackLoggerValue + "): " + e.getMessage());
		}
		assert attackLogger != null;
		return attackLogger;
	}

	private boolean isLogClientUserData(ConfigurationManager configManager) {
		boolean logClientUserData;
		logClientUserData =  FilterLoadConfigUtils.loadConfigFlag(configManager,PARAM_LOG_CLIENT_USER_DATA);
		if (this.debug) logLocal("Log client user data: " + logClientUserData);
		return logClientUserData;
	}

	private void loadBlockRepeatedRedirectsThreshold(ConfigurationManager configManager) throws UnavailableException {
		String blockRepeatedRedirectsThresholdValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_BLOCK_REPEATED_REDIRECTS_THRESHOLD,"150");
		try {
			this.blockRepeatedRedirectsThreshold = Integer.parseInt(blockRepeatedRedirectsThresholdValue);
			if (this.blockRepeatedRedirectsThreshold < 0)
				throw new UnavailableException("Configured 'block repeated redirects threshold' must not be negative: " + blockRepeatedRedirectsThresholdValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'block repeated redirects threshold': " + blockRepeatedRedirectsThresholdValue);
		}
		if (this.debug) logLocal("Block repeated redirects threshold: " + this.blockRepeatedRedirectsThreshold);
	}

	private void loadResetPeriodForRedirectThreshold(ConfigurationManager configManager) throws UnavailableException {
		String resetPeriodMinutesRedirectThresholdValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_RESET_PERIOD_REDIRECT_THRESHOLD,"2");
		try {
			this.resetPeriodMinutesRedirectThreshold = Integer.parseInt(resetPeriodMinutesRedirectThresholdValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'reset period minutes (redirect threshold)': " + resetPeriodMinutesRedirectThresholdValue);
		}
		if (this.debug) logLocal("Reset period minutes (redirect threshold): " + resetPeriodMinutesRedirectThresholdValue);
	}

	private void loadResetPeriodMinutesForBadResponseCode(ConfigurationManager configManager) throws UnavailableException {
		String resetPeriodMinutesBadResponseCodeValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_RESET_PERIOD_BAD_RESPONSE_CODE,"2");
		try {
			int resetPeriodMinutesBadResponseCode = Integer.parseInt(resetPeriodMinutesBadResponseCodeValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'reset period minutes (bad response code)': " + resetPeriodMinutesBadResponseCodeValue);
		}
		if (this.debug) logLocal("Reset period minutes (bad response code): " + resetPeriodMinutesBadResponseCodeValue);
	}

	private void loadResetPeriodMinutesForSessionCreation(ConfigurationManager configManager) throws UnavailableException {
		String resetPeriodMinutesSessionCreationValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_RESET_PERIOD_SESSION_CREATION,"5");
		try {
			int resetPeriodMinutesSessionCreation = Integer.parseInt(resetPeriodMinutesSessionCreationValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'reset period minutes (session creation)': " + resetPeriodMinutesSessionCreationValue);
		}
		if (this.debug) logLocal("Reset period minutes (session creation): " + resetPeriodMinutesSessionCreationValue);
	}

	private void loadResetPeriodMinutesAfterAttack(ConfigurationManager configManager) throws UnavailableException {
		String resetPeriodMinutesAttackValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_RESET_PERIOD_ATTACK,"10");
		try {
			this.resetPeriodMinutesAttack = Integer.parseInt(resetPeriodMinutesAttackValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'reset period minutes (attack)': " + resetPeriodMinutesAttackValue);
		}
		if (this.debug) logLocal("Reset period minutes (attack): " + resetPeriodMinutesAttackValue);
	}

	private void loadBlockPeriodMinutes(ConfigurationManager configManager) throws UnavailableException {
		String blockPeriodMinutesValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_BLOCK_ATTACKING_CLIENTS_DURATION,"20");
		try {
			this.blockPeriodMinutes = Integer.parseInt(blockPeriodMinutesValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'block period minutes': " + blockPeriodMinutesValue);
		}
		if (this.debug) logLocal("Block period minutes: " + this.blockPeriodMinutes);
	}

	private void loadConfigReloadInterval(ConfigurationManager configManager) throws UnavailableException {
		String configReloadingIntervalMillisValue = FilterLoadConfigUtils.loadConfig(configManager,PARAM_CONFIG_RELOADING_INTERVAL,"0");
		try {
			this.configReloadingIntervalMillis = Integer.parseInt(configReloadingIntervalMillisValue) * 60 * 1000;
			if (this.configReloadingIntervalMillis > 0) {
				this.nextConfigReloadingTime = System.currentTimeMillis() + this.configReloadingIntervalMillis;
			}
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'config reloading interval': " + configReloadingIntervalMillisValue);
		}
		if (this.debug) logLocal("Config reloading interval minutes: " + configReloadingIntervalMillisValue);
	}

	private void loadRuleFileReloadInterval(ConfigurationManager configManager) throws UnavailableException {
		String ruleFileReloadingIntervalMillisValue =  FilterLoadConfigUtils.loadConfig(configManager,PARAM_RULE_RELOADING_INTERVAL,"0");
		try {
			this.ruleFileReloadingIntervalMillis = Integer.parseInt(ruleFileReloadingIntervalMillisValue) * 60 * 1000;
			if (this.ruleFileReloadingIntervalMillis > 0) {
				this.nextRuleReloadingTime = System.currentTimeMillis() + this.ruleFileReloadingIntervalMillis;
			}
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'rule reloading interval': " + ruleFileReloadingIntervalMillisValue);
		}
		if (this.debug) logLocal("Rule reloading interval minutes: " + ruleFileReloadingIntervalMillisValue);
	}

	private void loadHouseKeepingInterval(ConfigurationManager configManager) throws UnavailableException {
		String housekeepingIntervalMinutesValue =  FilterLoadConfigUtils.loadConfig(configManager,PARAM_HOUSEKEEPING_INTERVAL,"15");
		try {
			this.housekeepingIntervalMinutes = Integer.parseInt(housekeepingIntervalMinutesValue);
		} catch (NumberFormatException e) {
			throw new UnavailableException("Unable to number-parse configured 'housekeeping interval': " + housekeepingIntervalMinutesValue);
		}
		if (this.debug) logLocal("Housekeeping interval minutes: " + this.housekeepingIntervalMinutes);
	}

	private void loadClientIpDeteminator(ConfigurationManager configManager) throws UnavailableException {
		String clientIpDeterminatorValue =  FilterLoadConfigUtils.loadConfig(configManager,PARAM_CLIENT_IP_DETERMINATOR,"com.ks.DefaultClientIpDeterminator");
		if (clientIpDeterminatorValue.isEmpty())
			throw new UnavailableException("Filter init-param is empty: " + PARAM_CLIENT_IP_DETERMINATOR);
		Class clientIpDeterminatorClass;
		try {
			clientIpDeterminatorClass = Class.forName(clientIpDeterminatorValue);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Unable to find client-ip-determinator class (" + clientIpDeterminatorValue + "): " + e.getMessage());
		}
		try {
			assert clientIpDeterminatorClass != null;
			this.clientIpDeterminator = (ClientIpDeterminator) clientIpDeterminatorClass.newInstance();
			this.clientIpDeterminator.setFilterConfig(filterConfig);
		} catch (Exception ex) {
			throw new UnavailableException("Unable to create and configure client-ip-determinator instance: " + ex.getMessage());
		}
	}

	private void loadRuleFileLoader(ConfigurationManager configManager) throws UnavailableException {
		String ruleFileLoaderClass =  FilterLoadConfigUtils.loadConfig(configManager,PARAM_RULE_LOADER,"com.ks.loaders.ClasspathZipRuleFileLoader");
		if (ruleFileLoaderClass.isEmpty())
			throw new UnavailableException("Filter init-param is empty: " + PARAM_RULE_LOADER);
		try {
			this.ruleFileLoaderClass = Class.forName(ruleFileLoaderClass);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Unable to find rule-file-loader class (" + ruleFileLoaderClass + "): " + e.getMessage());
		}
		assert this.ruleFileLoaderClass != null;
	}

	private void internalDoFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		final long timerBefore = System.currentTimeMillis();

		if (Boolean.TRUE.equals(request.getAttribute(REQUEST_NESTED_FORWARD_CALL))) { // NOTE: the request and response can be of any type here, since some applications even wrap further and add another wrapper over our wrappers
			chain.doFilter(request, response);
			return; // avoid being called in nested (forwarded) pages too (of course still working on decrypted requests since decryption removes this flag)
		}

		{
			if (isFilterReady((HttpServletResponse) response)) return;
		}

		{
			if (reloadRulesWhenRequired((HttpServletResponse) response)) return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		if (defineCharacterEncoding(request, (HttpServletResponse) response, httpRequest)) return;

		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) {
			logLocal("---------------------- "+new Date());
			logLocal("1A param map: "+httpRequest.getParameterMap().keySet());
			logLocal("1A query string: "+httpRequest.getQueryString());
			logLocal("1A servlet path: "+httpRequest.getServletPath());
		}

		final String clientAddress = RequestUtils.determineClientIp(httpRequest, this.clientIpDeterminator);
		HttpSession session = httpRequest.getSession(false);

		if (checkBlockedIps((HttpServletResponse) response, clientAddress)) return;
		// ===== PER-REQUEST CHECKING STUFF STARTS HERE:

		if (checkclienAddress((HttpServletResponse) response, clientAddress))
			return; // = stop processing

		final String servletPath = httpRequest.getServletPath();
		final String requestURI = httpRequest.getRequestURI();
		final String queryString = httpRequest.getQueryString();

		if (ckeckAllowedRequestMimeTypes((HttpServletResponse) response, httpRequest, clientAddress))
			return; // = stop processing

		// =========================================================
		// Calculate sizes for the size-limits checking
		// =========================================================
		if (calculateSizeLimits((HttpServletResponse) response, httpRequest, servletPath, requestURI, queryString))
			return; // = stop processing


		if (this.debug) logLocal("doFilter on "+request.getClass().getName());
		if (this.debug) logLocal("... with URL "+httpRequest.getRequestURL()+" and query string "+queryString);
		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("2: " + httpRequest.getParameterMap().keySet());



		// =========================================================
		// check forced-session-invalidation
		// =========================================================
		if (session != null && this.forcedSessionInvalidationPeriodMinutes > 0) {
			session = checkForcedSessionInvalidation((HttpServletResponse) response, httpRequest, clientAddress, session);
			if (session == null) return; // = stop processing

		}

		// SESSION AUTO-CREATE

		// create or retrieve session-based crypto stuff - for later use (see below)
		String cryptoDetectionString = null; CryptoKeyAndSalt cryptoKey = null; Cipher cipher = null;
		if (this.contentInjectionHelper.isEncryptQueryStringInLinks()) {
			try {
				session = getSession(httpRequest, session);
				cryptoDetectionString = RequestUtils.createOrRetrieveRandomTokenFromSession(session, ParamConsts.SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY);
				cipher = CryptoUtils.getCipher();
				cryptoKey = RequestUtils.createOrRetrieveRandomCryptoKeyFromSession(session, ParamConsts.SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY, this.extraEncryptedValueHashProtection);
			} catch (Exception e) {
				this.attackHandler.logWarningRequestMessage("Unable to define protection content in session: "+e.getMessage());
				try {
					if (session != null) {
						session.invalidate();
					}
				} catch (IllegalStateException ignored) {}
				sendDisallowedResponse((HttpServletResponse)response, new Attack("Unable to define protection content in session"));
				return;
			}
		}
		// create or retrieve session-based request tokens - for later use (see below)
		String secretTokenKey = null, secretTokenValue = null;
		if (this.contentInjectionHelper.isInjectSecretTokenIntoLinks()) {
			try {
				session = getSession(httpRequest, session);
				secretTokenKey = RequestUtils.createOrRetrieveRandomTokenFromSession(session, ParamConsts.SESSION_SECRET_RANDOM_TOKEN_KEY_KEY);
				secretTokenValue = RequestUtils.createOrRetrieveRandomTokenFromSession(session, ParamConsts.SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY);
			} catch (Exception e) {
				this.attackHandler.logWarningRequestMessage("Unable to define protection content in session: "+e.getMessage());
				try {
					if (session != null) {
						session.invalidate();
					}
				} catch (IllegalStateException ignored) {}
				sendDisallowedResponse((HttpServletResponse)response, new Attack("Unable to define protection content in session"));
				return;
			}
		}
		// now the key for param-and-form protection keys - for later use (see below)
		String parameterAndFormProtectionKeyKey = null;
		if (this.contentInjectionHelper.isProtectParametersAndForms()) {
			try {
				session = getSession(httpRequest, session);
				parameterAndFormProtectionKeyKey = RequestUtils.createOrRetrieveRandomTokenFromSession(session, ParamConsts.SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY); // yes, it is the key of the key
			} catch (Exception e) {
				this.attackHandler.logWarningRequestMessage("Unable to define protection content in session: "+e.getMessage());
				try {
					if (session != null) {
						session.invalidate();
					}
				} catch (IllegalStateException ignored) {}
				sendDisallowedResponse((HttpServletResponse)response, new Attack("Unable to define protection content in session"));
				return;
			}
		}


		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("3: "+httpRequest.getParameterMap().keySet());


		// ########################################################
		// Try to decrypt request since it might be encrypted
		// ########################################################

		if (ParamConsts.DEBUG_PRINT_UNCOVERING_DETAILS) logLocal("cryptoDetectionString=" + cryptoDetectionString);
		if (this.contentInjectionHelper.isEncryptQueryStringInLinks() && cryptoDetectionString != null && cryptoKey != null
				&& request.getAttribute(ParamConsts.REQUEST_ALREADY_DECRYPTED_FLAG) == null) { // = potentially encrypted (and not yet via "requestDispatcher.forward" decrypt handled) request - i.e. encryption is active ==> Note that here request.getAttribute and not getParameter is used !! This is important !
			if (decryptRequest(request, response, httpRequest, clientAddress, session, servletPath, requestURI, cryptoDetectionString, cryptoKey, secretTokenKey, parameterAndFormProtectionKeyKey))
				return; // = stop processing as desired :-)
		}

		// =========================================================
		// Decoding Permutation according to the defined level
		// =========================================================
		byte decodingPermutationLevel = 1; // default level
		if (this.ruleDefinitions.getDecodingPermutationDefinitions() != null && this.ruleDefinitions.getDecodingPermutationDefinitions().hasEnabledDefinitions()) {
			final DecodingPermutationDefinition matchingDefinition = this.ruleDefinitions.getDecodingPermutationDefinitions().getMatchingDecodingPermutationDefinition(servletPath, requestURI);
			if (matchingDefinition != null) decodingPermutationLevel = matchingDefinition.getLevel();
		}

		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Request detail data fetching
		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		boolean nonStandardPermutationsRequired = false; // here we check all those that "extends RequestDefinitionContainer" for non-standard permutation requirements:
		nonStandardPermutationsRequired = isNonStandardPermutationsRequired(nonStandardPermutationsRequired);

		final RequestDetails requestDetails = getRequestDetails(httpRequest, servletPath, requestURI, decodingPermutationLevel, nonStandardPermutationsRequired);

		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("6: "+httpRequest.getParameterMap().keySet());
		// remove temporarily injected parameters (from copied Map only, NOT from original request)
		requestDetails.requestParameterMap = new HashMap( httpRequest.getParameterMap() ); // defensive copy of the map
		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("7: "+httpRequest.getParameterMap().keySet());
		removeTemporarilyInjectedParametersFromMap(requestDetails.requestParameterMap, httpRequest.getSession(false), cryptoDetectionString);
		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("8: "+httpRequest.getParameterMap().keySet());
		if (isHavingEnabledRequestParameterCheckingRules) requestDetails.requestParameterMapVariants = ServerUtils.permutateVariants(requestDetails.requestParameterMap, nonStandardPermutationsRequired,decodingPermutationLevel);
		requestDetails.nonStandardPermutationsRequired = nonStandardPermutationsRequired;
		requestDetails.decodingPermutationLevel = decodingPermutationLevel;
		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

		/*
		 * Create wrappers for the request and response objects.
		 * Using these, you can extend the capabilities of the
		 * request and response, for example, allow setting parameters
		 * on the request before sending the request to the rest of the filter chain,
		 * or keep track of the cookies that are set on the response.
		 *
		 * Caveat: some servers do not handle wrappers very well for forward or
		 * include requests.
		 */
		boolean transparentQueryString = true;
		boolean transparentForwarding = true;
		final RequestWrapper wrappedRequest = new RequestWrapper(httpRequest, this.contentInjectionHelper, this.sessionCreationCounter, requestDetails.clientAddress,
				this.hideInternalSessionAttributes, transparentQueryString, transparentForwarding);
		ResponseWrapper wrappedResponse = null;
		AllowedFlagWithMessage allowed = new AllowedFlagWithMessage(false, new Attack("Disallowed by default"));
		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("9: "+httpRequest.getParameterMap().keySet());
		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("9c: "+wrappedRequest.getParameterMap().keySet());
		try {


			// fetch the response-modification patterns
			final ResponseModificationDefinition[] responseModificationDefinitionsArr = this.ruleDefinitions.getResponseModificationDefinitions().getAllMatchingResponseModificationDefinitions(httpRequest,
					requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
					requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
					requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
					requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
					requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);
			final List tmpPatternsToExcludeCompleteTag = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPatternsToExcludeCompleteScript = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPatternsToExcludeLinksWithinScripts = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPatternsToExcludeLinksWithinTags = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPatternsToCaptureLinksWithinScripts = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPatternsToCaptureLinksWithinTags = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPrefiltersToExcludeCompleteTag = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPrefiltersToExcludeCompleteScript = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPrefiltersToExcludeLinksWithinScripts = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPrefiltersToExcludeLinksWithinTags = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPrefiltersToCaptureLinksWithinScripts = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpPrefiltersToCaptureLinksWithinTags = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpGroupNumbersToCaptureLinksWithinScripts = new ArrayList(responseModificationDefinitionsArr.length);
			final List tmpGroupNumbersToCaptureLinksWithinTags = new ArrayList(responseModificationDefinitionsArr.length);
            for (final ResponseModificationDefinition responseModificationDefinition : responseModificationDefinitionsArr) {
                if (responseModificationDefinition.isMatchesScripts()) {
                    tmpPatternsToExcludeCompleteScript.add(responseModificationDefinition.getScriptExclusionPattern());
                    tmpPrefiltersToExcludeCompleteScript.add(responseModificationDefinition.getScriptExclusionPrefilter());
                    tmpPatternsToExcludeLinksWithinScripts.add(responseModificationDefinition.getUrlExclusionPattern());
                    tmpPrefiltersToExcludeLinksWithinScripts.add(responseModificationDefinition.getUrlExclusionPrefilter());
                    tmpPatternsToCaptureLinksWithinScripts.add(responseModificationDefinition.getUrlCapturingPattern());
                    tmpPrefiltersToCaptureLinksWithinScripts.add(responseModificationDefinition.getUrlCapturingPrefilter());
                    tmpGroupNumbersToCaptureLinksWithinScripts.add(ServerUtils.convertSimpleToObjectArray(responseModificationDefinition.getCapturingGroupNumbers()));
                }
                if (responseModificationDefinition.isMatchesTags()) {
                    tmpPatternsToExcludeCompleteTag.add(responseModificationDefinition.getTagExclusionPattern());
                    tmpPrefiltersToExcludeCompleteTag.add(responseModificationDefinition.getTagExclusionPrefilter());
                    tmpPatternsToExcludeLinksWithinTags.add(responseModificationDefinition.getUrlExclusionPattern());
                    tmpPrefiltersToExcludeLinksWithinTags.add(responseModificationDefinition.getUrlExclusionPrefilter());
                    tmpPatternsToCaptureLinksWithinTags.add(responseModificationDefinition.getUrlCapturingPattern());
                    tmpPrefiltersToCaptureLinksWithinTags.add(responseModificationDefinition.getUrlCapturingPrefilter());
                    tmpGroupNumbersToCaptureLinksWithinTags.add(ServerUtils.convertSimpleToObjectArray(responseModificationDefinition.getCapturingGroupNumbers()));
                }
            }
			// convert lists of Pattern to arrays of Matcher
			final Matcher[] matchersToExcludeCompleteTag = ServerUtils.convertListOfPatternToArrayOfMatcher(tmpPatternsToExcludeCompleteTag);
			final Matcher[] matchersToExcludeCompleteScript = ServerUtils.convertListOfPatternToArrayOfMatcher(tmpPatternsToExcludeCompleteScript);
			final Matcher[] matchersToExcludeLinksWithinScripts = ServerUtils.convertListOfPatternToArrayOfMatcher(tmpPatternsToExcludeLinksWithinScripts);
			final Matcher[] matchersToExcludeLinksWithinTags = ServerUtils.convertListOfPatternToArrayOfMatcher(tmpPatternsToExcludeLinksWithinTags);
			final Matcher[] matchersToCaptureLinksWithinScripts = ServerUtils.convertListOfPatternToArrayOfMatcher(tmpPatternsToCaptureLinksWithinScripts);
			final Matcher[] matchersToCaptureLinksWithinTags = ServerUtils.convertListOfPatternToArrayOfMatcher(tmpPatternsToCaptureLinksWithinTags);
			final WordDictionary[] prefiltersToExcludeCompleteTag = (WordDictionary[]) tmpPrefiltersToExcludeCompleteTag.toArray(new WordDictionary[0]);
			final WordDictionary[] prefiltersToExcludeCompleteScript = (WordDictionary[]) tmpPrefiltersToExcludeCompleteScript.toArray(new WordDictionary[0]);
			final WordDictionary[] prefiltersToExcludeLinksWithinScripts = (WordDictionary[]) tmpPrefiltersToExcludeLinksWithinScripts.toArray(new WordDictionary[0]);
			final WordDictionary[] prefiltersToExcludeLinksWithinTags = (WordDictionary[]) tmpPrefiltersToExcludeLinksWithinTags.toArray(new WordDictionary[0]);
			final WordDictionary[] prefiltersToCaptureLinksWithinScripts = (WordDictionary[]) tmpPrefiltersToCaptureLinksWithinScripts.toArray(new WordDictionary[0]);
			final WordDictionary[] prefiltersToCaptureLinksWithinTags = (WordDictionary[]) tmpPrefiltersToCaptureLinksWithinTags.toArray(new WordDictionary[0]);
			final int[][] groupNumbersToCaptureLinksWithinScripts = ServerUtils.convertArrayIntegerListTo2DimIntArray(tmpGroupNumbersToCaptureLinksWithinScripts);
			final int[][] groupNumbersToCaptureLinksWithinTags = ServerUtils.convertArrayIntegerListTo2DimIntArray(tmpGroupNumbersToCaptureLinksWithinTags);

			if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("10: "+httpRequest.getParameterMap().keySet());
			if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("10c: "+wrappedRequest.getParameterMap().keySet());
			// create response wrapper (but watch out for optimization-hint requests)
			final boolean isOptimizationHint = this.ruleDefinitions.getOptimizationHintDefinitions().isOptimizationHint(wrappedRequest,
					requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
					requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
					requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
					requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
					requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);
			wrappedResponse = new ResponseWrapper((HttpServletResponse)response, wrappedRequest, this.attackHandler, this.contentInjectionHelper, isOptimizationHint, cryptoDetectionString,cipher,cryptoKey, secretTokenKey,secretTokenValue,
					parameterAndFormProtectionKeyKey, this.blockResponseHeadersWithCRLF, this.blockNonLocalRedirects, clientAddress,
					prefiltersToExcludeCompleteScript, matchersToExcludeCompleteScript,
					prefiltersToExcludeCompleteTag, matchersToExcludeCompleteTag,
					prefiltersToExcludeLinksWithinScripts, matchersToExcludeLinksWithinScripts,
					prefiltersToExcludeLinksWithinTags, matchersToExcludeLinksWithinTags,
					prefiltersToCaptureLinksWithinScripts, matchersToCaptureLinksWithinScripts,
					prefiltersToCaptureLinksWithinTags, matchersToCaptureLinksWithinTags,
					groupNumbersToCaptureLinksWithinScripts,
					groupNumbersToCaptureLinksWithinTags,
					this.useFullPathForResourceToBeAccessedProtection, this.additionalFullResourceRemoval, this.additionalMediumResourceRemoval, this.maskAmpersandsInLinkAdditions,
					hiddenFormFieldProtection, selectBoxProtection, checkboxProtection, radiobuttonProtection, selectBoxValueMasking, checkboxValueMasking, radiobuttonValueMasking,
					this.appendSessionIdToLinks, this.reuseSessionContent,
					this.honeyLinkPrefix, this.honeyLinkSuffix, this.honeyLinkMaxPerPage, this.randomizeHoneyLinksOnEveryRequest, this.pdfXssProtection);
			if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("11: "+httpRequest.getParameterMap().keySet());
			if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("11c: "+wrappedRequest.getParameterMap().keySet());
			// do "before processing" stuff
			//final String requestURL = ""+wrappedRequest.getRequestURL();
			final Boolean isFormSubmit = (Boolean) request.getAttribute(ParamConsts.REQUEST_IS_FORM_SUBMIT_FLAG);
			final Boolean isUrlManipulated = (Boolean) request.getAttribute(REQUEST_IS_URL_MANIPULATED_FLAG);
			allowed = doBeforeProcessing(wrappedRequest, wrappedResponse, requestDetails, cryptoDetectionString, isFormSubmit, isUrlManipulated);
			if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("12: "+httpRequest.getParameterMap().keySet());
			if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("12c: "+wrappedRequest.getParameterMap().keySet());
		} catch (StopFilterProcessingException e) {
			this.attackHandler.handleRegularRequest(httpRequest, clientAddress); // = since we're about to stop this request, we log it here
			this.attackHandler.logWarningRequestMessage("Desired stop in filter processing of previously logged request: "+e.getMessage());
			return; // = stop processing as desired :-)
		} catch (Exception e) {
			final String message = "Exception ("+e.getMessage()+") while checking request (therefore disallowing it by default)";
			allowed = new AllowedFlagWithMessage(false, new Attack(message));
			if (!(e instanceof ServerAttackException)) {
				System.err.println(message);
				e.printStackTrace();
			}
			this.attackHandler.logWarningRequestMessage(message);
		}

		if (allowed.isAllowed()) {
			assert wrappedResponse != null;
			Throwable problem = null;
			boolean attackSoFar = false;
			try {
				if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("13: "+httpRequest.getParameterMap().keySet());
				if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("13c: "+wrappedRequest.getParameterMap().keySet());
				// to avoid pollution of the application remove any temporarily injected parameters (used only internally by this filter)
				removeTemporarilyInjectedParametersFromRequest(wrappedRequest, cryptoDetectionString);
				if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("14: "+httpRequest.getParameterMap().keySet());
				if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("14c: "+wrappedRequest.getParameterMap().keySet());
				// during the chain (i.e. during the application logic code) transfer protective session contents in case the application itself renews the session
				// that way all KsWaf content of the old session which the application might invalidate is transferred into the new one
				wrappedRequest.setTransferProtectiveSessionContentToNewSessionsDefinedByApplication(true);
				// during the chain (i.e. during the application logic code) apply even the unsecure parameter values checks
				wrappedRequest.setApplyUnsecureParameterValueChecks(true);
				// CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN
				wrappedRequest.setAttribute(REQUEST_NESTED_FORWARD_CALL, Boolean.TRUE);
				if (this.showTimings) logLocal("Duration for pre-processing of request: "+(System.currentTimeMillis()-timerBefore)+" ms");
				chain.doFilter(wrappedRequest, wrappedResponse);
				final int status = wrappedResponse.getCapturedStatus();
				if (status < 400) this.attackHandler.handleLearningModeRequestAggregation(wrappedRequest); // = LEARNING MODE
				wrappedRequest.removeAttribute(REQUEST_NESTED_FORWARD_CALL);
				if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("15: "+httpRequest.getParameterMap().keySet());
				if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("15c: "+wrappedRequest.getParameterMap().keySet());
				// CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN CHAIN
			} catch(ServerAttackException e) {
				attackSoFar = true;
				if (this.debug) e.printStackTrace();
				wrappedRequest.setTransferProtectiveSessionContentToNewSessionsDefinedByApplication(false); // also clears the eventually used TreadLocal
				// already handled by attack-handler, but send disallowed response nevertheless without counting it in AttackHandler
				sendDisallowedResponse((HttpServletResponse)response, new Attack(e.getMessage()));
			} catch(Throwable t) {
				/*
				 * If an exception is thrown somewhere down the filter chain,
				 * we still want to execute our after processing, and then
				 * rethrow the problem after that.
				 */
				problem = t;
				t.printStackTrace();
			} finally {
				// Stop transferring KsWaf session content from the old into new sessions, as the chain is over and if now the session gets invalidated it gets
				// invalidated by KsWaf, so that is then desired and should no longer get the KsWaf session contents transferred...
				wrappedRequest.setTransferProtectiveSessionContentToNewSessionsDefinedByApplication(false); // also clears the eventually used TreadLocal
				// Stop the unsecure values checks as the application logic has now finished
				wrappedRequest.setApplyUnsecureParameterValueChecks(false);
			}

			final long timerAfter = System.currentTimeMillis();
			if (!attackSoFar) doAfterProcessing(wrappedRequest, wrappedResponse);
			if (this.showTimings) logLocal("Duration for post-processing of response: "+(System.currentTimeMillis()-timerAfter)+" ms");


			/*
			 * If there was a problem, we want to rethrow it if it is
			 * a known type, otherwise log it.
			 */
			if (problem != null) {
				if (problem instanceof ServletException) throw (ServletException)problem;
				if (problem instanceof IOException) throw (IOException)problem;
				if (problem instanceof RuntimeException) throw (RuntimeException)problem;
				sendProcessingError(problem, (HttpServletResponse)response);
			} else {
				if (this.flushResponse) wrappedResponse.flushBuffer();
			}


		} else {
			if (allowed.getAttack() != null) {
				// already counted in AttackHandler (that's where the Attack object came from), so only send the disallowed response here
				sendDisallowedResponse((HttpServletResponse)response, allowed.getAttack());
			}
		}

	}

	private RequestDetails getRequestDetails(HttpServletRequest httpRequest, String servletPath, String requestURI, byte decodingPermutationLevel, boolean nonStandardPermutationsRequired) {
		final RequestDetails requestDetails = new RequestDetails();
		requestDetails.clientAddress = RequestUtils.determineClientIp(httpRequest, this.clientIpDeterminator);
		requestDetails.agent = httpRequest.getHeader("user-agent");
		requestDetails.servletPath = servletPath;
		requestDetails.queryString = httpRequest.getQueryString();
		if (isHavingEnabledQueryStringCheckingRules) requestDetails.queryStringVariants = ServerUtils.permutateVariants(requestDetails.queryString, nonStandardPermutationsRequired,decodingPermutationLevel);
		requestDetails.requestedSessionId = httpRequest.getRequestedSessionId();
		requestDetails.sessionCameFromCookie = httpRequest.isRequestedSessionIdFromCookie();
		requestDetails.sessionCameFromURL = httpRequest.isRequestedSessionIdFromURL();
		requestDetails.referrer = httpRequest.getHeader("Referer"); // yes, according to HTTP RFC the exact name is "Referer" which is a misspelling
		requestDetails.url = ""+httpRequest.getRequestURL();
		requestDetails.uri = requestURI;
		requestDetails.method = httpRequest.getMethod();
		requestDetails.protocol = httpRequest.getProtocol();
		requestDetails.mimeType = httpRequest.getContentType();
		requestDetails.remoteHost = httpRequest.getRemoteHost();
		requestDetails.remoteUser = httpRequest.getRemoteUser();
		requestDetails.headerMap = RequestUtils.createHeaderMap(httpRequest);
		if (isHavingEnabledHeaderCheckingRules) requestDetails.headerMapVariants = ServerUtils.permutateVariants(requestDetails.headerMap, nonStandardPermutationsRequired,decodingPermutationLevel);
		requestDetails.cookieMap = RequestUtils.createCookieMap(httpRequest);
		if (isHavingEnabledCookieCheckingRules) requestDetails.cookieMapVariants = ServerUtils.permutateVariants(requestDetails.cookieMap, nonStandardPermutationsRequired,decodingPermutationLevel);
		requestDetails.encoding = httpRequest.getCharacterEncoding();
		requestDetails.contentLength = httpRequest.getContentLength();
		requestDetails.scheme = httpRequest.getScheme();
		requestDetails.serverName = httpRequest.getServerName();
		requestDetails.serverPort = httpRequest.getServerPort();
		requestDetails.authType = httpRequest.getAuthType();
		requestDetails.contextPath = httpRequest.getContextPath();
		requestDetails.pathInfo = httpRequest.getPathInfo();
		requestDetails.pathTranslated = httpRequest.getPathTranslated();
		requestDetails.remotePort = httpRequest.getRemotePort();
		requestDetails.localPort = httpRequest.getLocalPort();
		requestDetails.localAddr = httpRequest.getLocalAddr();
		requestDetails.localName = httpRequest.getLocalName();
		return requestDetails;
	}

	private boolean isNonStandardPermutationsRequired(boolean nonStandardPermutationsRequired) {
		boolean checkWhiteListDefenitions = this.ruleDefinitions.getWhiteListDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getWhiteListDefinitions().hasEnabledDefinitions();
		boolean checkBadRequestDefenitions = this.ruleDefinitions.getBadRequestDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getBadRequestDefinitions().hasEnabledDefinitions();
		boolean checkDenialOfServiceDefinitions = this.ruleDefinitions.getDenialOfServiceLimitDefinitions().isNonStandardPermutationsAllowed() &&this.ruleDefinitions.getDenialOfServiceLimitDefinitions().hasEnabledDefinitions();
		boolean checkEntryPointDefenitions = this.ruleDefinitions.getEntryPointDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getEntryPointDefinitions().hasEnabledDefinitions();
		boolean checkOptimizationHintDefinitions = this.ruleDefinitions.getOptimizationHintDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getOptimizationHintDefinitions().hasEnabledDefinitions();
		boolean checkRenewSessionPointDefinitions = this.ruleDefinitions.getRenewSessionPointDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getRenewSessionPointDefinitions().hasEnabledDefinitions();
		boolean checkIncomingProtectionExcludeDefinitions = this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().hasEnabledDefinitions();
		boolean checkResponseModificationDefinitions = this.ruleDefinitions.getResponseModificationDefinitions().isNonStandardPermutationsAllowed() && this.ruleDefinitions.getResponseModificationDefinitions().hasEnabledDefinitions();

		if (checkWhiteListDefenitions || checkBadRequestDefenitions || checkDenialOfServiceDefinitions || checkEntryPointDefenitions
				|| checkOptimizationHintDefinitions || checkRenewSessionPointDefinitions || checkIncomingProtectionExcludeDefinitions || checkResponseModificationDefinitions) nonStandardPermutationsRequired = true;
		return nonStandardPermutationsRequired;
	}

	private boolean decryptRequest(ServletRequest request, ServletResponse response, HttpServletRequest httpRequest, String clientAddress, HttpSession session, String servletPath, String requestURI, String cryptoDetectionString, CryptoKeyAndSalt cryptoKey, String secretTokenKey, String parameterAndFormProtectionKeyKey) throws ServletException, IOException {
		try {
			// if the request contains a secret-token *already unencrypted* (via URL or FORM), we can assume some spoofings, since the real secret token sits inside the encrypted value (when encryption is active),
			// because every link that gets a secret-token injected also gets encrypted when both is activated
			if (this.contentInjectionHelper.isInjectSecretTokenIntoLinks() && secretTokenKey != null && request.getParameter(secretTokenKey) != null) {
				throw new ServerAttackException("Additional (unencrypted) secret token parameter provided (potential spoofing)");
			}
			// if the request contains a protection-token *already unencrypted* (via URL or FORM), we can assume some spoofings, since the real protection token sits inside the encrypted value (when encryption is active),
			// because every link that gets a protection-token injected also gets encrypted when both is activated
			if (this.contentInjectionHelper.isProtectParametersAndForms() && parameterAndFormProtectionKeyKey != null && request.getParameter(parameterAndFormProtectionKeyKey) != null) {
				throw new ServerAttackException("Additional (unencrypted) parameter-and-form protection token parameter provided (potential spoofing)");
			}


			// Prepare for a query-string adjustment: when a form field submits the crypto value and the form has an empty action url
			// in which case the form submits to itself (current url; a.k.a self-submit) the URL params of the current URL are also reused
			// by the browser on the self-submit... That means that (potentially different) crypto values are present in the URL and the FORM submit.
			// When that is the case, the FORM submitted value has precedence and the URL's query-string must get the crypto-value removed (as
			// it is an old one)...
			String queryStringEventuallyAdjusted = httpRequest.getQueryString();
			String queryStringContentToRemove = null;

			// OK, try to decrypt the potentially encrypted request
			// ==============================
			// At first loop over the request parameter names to check if there is one parameter with the cryptoDetectionString in its name...
			// In case there are two ones (this could be the case when a self-submit form with an empty action value submits against the
			// current URL where then the browser also uses the current page's URL params as params for the self-submit) we have to use
			// the form-field submitted value instead of the URL value...
			String encryptedParam = null;
			boolean hasAdditionalParameters = false;
			for (final Enumeration paramNames = httpRequest.getParameterNames(); paramNames.hasMoreElements();) {
				final String paramName = (String) paramNames.nextElement();
				if (ParamConsts.DEBUG_PRINT_UNCOVERING_DETAILS) logLocal("paramName="+paramName);
				if (paramName.contains(cryptoDetectionString)) {
					final String value = httpRequest.getParameter(paramName);
					// check for multiple encrypted tokens
					if (encryptedParam != null) { // = we already have one
						// Now we have to check if the current one is a form-field one (i.e. not part of the query-string)...
						// So when the current one is a form-field submitted crypto value, use that instead of the previous one...  for example on action-less GET-based forms that are submitted to their URLs back inclusive their params (at least most of the time)
						final boolean cameViaFormField = ParamConsts.INTERNAL_TYPE_FORM.equals(value);
						if (ParamConsts.DEBUG_PRINT_UNCOVERING_DETAILS) {
							logLocal("queryStringEventuallyAdjusted=   "+queryStringEventuallyAdjusted);
							logLocal("cameViaFormField="+cameViaFormField);
						}
						if (cameViaFormField) {
							if (queryStringContentToRemove != null) {
								throw new ServerAttackException("Multiple (more than the two allowed URL vs. FORM) crypto values provided (potential spoofing): "+paramName);
							}
							// use it (see below) and set a flag that the queryString has to be adjusted by removing any crypto URL content from the queryString
							queryStringContentToRemove = encryptedParam;
						} else {
							// here the FORM field submitted value came as the first param and so the current param is the URL-borne...
							// which must get removed, since FORM-borne crypto params have precedence
							queryStringContentToRemove = paramName;
							continue; // = continue directly and don't use it in below's code
						}
					}
					if (ParamConsts.APPEND_EQUALS_SIGN_TO_VALUELESS_URL_PARAM_NAMES) {
						if ( value == null || (!value.equals(ParamConsts.INTERNAL_TYPE_URL) && !value.equals(ParamConsts.INTERNAL_TYPE_FORM)) ) {
							throw new ServerAttackException("Missing or wrong value for encrypted name-only parameter provided (potential spoofing): "+value);
						}
					} else {
						if ( value != null && value.trim().length() > 0 ) {
							throw new ServerAttackException("Value for encrypted name-only parameter provided (potential spoofing): "+value);
						}
					}
					// OK, take it
					encryptedParam = paramName;
				} else hasAdditionalParameters = true;
			}
			if (ParamConsts.DEBUG_PRINT_UNCOVERING_DETAILS) logLocal("encryptedParam="+encryptedParam);
			if (queryStringContentToRemove != null) {
				// remove any crypto value from the queryString
				queryStringEventuallyAdjusted = ServerUtils.removeParameterFromQueryString(queryStringEventuallyAdjusted, queryStringContentToRemove);
			}
			// if there is something encrypted submitted, decrypt it
			if (encryptedParam != null) {
				final String encryptedInput;
				// check if it came in via URL or FORM param
				boolean cameViaURL = false;
				final boolean isQueryStringFilled = queryStringEventuallyAdjusted != null && queryStringEventuallyAdjusted.length() > 0;
				if (isQueryStringFilled) cameViaURL = queryStringEventuallyAdjusted.contains(cryptoDetectionString);
				// re-create the unencrypted original URL
				final StringBuilder servletPathWithQueryString = new StringBuilder(servletPath);
				if (isQueryStringFilled) servletPathWithQueryString.append('?').append(queryStringEventuallyAdjusted);
				if (!cameViaURL) { // = if it came in via FORM param, re-inject the param as URL param into the URL and use that re-created URL
					// re-inject the encrypted thing into the URL
					servletPathWithQueryString.append(isQueryStringFilled?'&':'?').append(encryptedParam); // BOOKMARK 62653266
				}
				encryptedInput = servletPathWithQueryString.toString();
				assert encryptedInput != null;
				final boolean isRequestMethodPOST = "POST".equalsIgnoreCase(httpRequest.getMethod());
				// logLocal("isRequestMethodPOST="+isRequestMethodPOST);
				if (!this.contentInjectionHelper.isExtraStrictParameterCheckingForEncryptedLinks()) hasAdditionalParameters = false;
				final RequestUtils.DecryptedQuerystring decryptedRequest = RequestUtils.decryptQueryStringInServletPathWithQueryString(httpRequest.getContextPath(), servletPath, encryptedInput, cryptoDetectionString, cryptoKey, requestURI, hasAdditionalParameters, isRequestMethodPOST, this.useFullPathForResourceToBeAccessedProtection, this.additionalFullResourceRemoval||this.additionalMediumResourceRemoval);
				if (ParamConsts.DEBUG_PRINT_UNCOVERING_DETAILS) logLocal("decryptedRequest="+decryptedRequest);
				if (decryptedRequest != null) {
					if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("4 (dive into forward): "+httpRequest.getParameterMap().keySet());
					// OK, it was encrypted and is now decrypted... so forward to the decrypted URL...
					this.attackHandler.logRegularRequestMessage("Successfully decrypted request:\n\tEncrypted input: "+encryptedInput+"\n\tDecrypted output without added parameters: "+decryptedRequest.decryptedString);
					// check multipartness
					if (this.blockMultipartRequestsForNonMultipartForms && Boolean.FALSE.equals(decryptedRequest.isFormMultipart)) {
						throw new ServerAttackException("Multipart encoding used for non-multipart form request");
					}
					// go on
					final RequestDispatcher requestDispatcher = httpRequest.getRequestDispatcher(decryptedRequest.decryptedString);
					if (request != null) {
						request.setAttribute(ParamConsts.REQUEST_ALREADY_DECRYPTED_FLAG, Boolean.TRUE);
						request.setAttribute(ParamConsts.REQUEST_IS_FORM_SUBMIT_FLAG, decryptedRequest.isFormSubmit);
						request.setAttribute(REQUEST_IS_URL_MANIPULATED_FLAG, decryptedRequest.wasManipulated);
						request.removeAttribute(REQUEST_NESTED_FORWARD_CALL); // = to let the filter work on that forwarded (i.e. nested) call too we simulate that this is not a nested (forwarded) call
						// set the response character encoding to the same custom request character encoding (when defined) as this is a very special situation here: we're including/forwarding stuff....
						if (this.requestCharacterEncoding != null && this.requestCharacterEncoding.length() > 0) {
							 response.setCharacterEncoding(this.requestCharacterEncoding);
						}

						// forward to the original unencrypted resource (not including since the include mechanism does not forward control to the resource, so that for example redirects and such originating from the included application logic will not work...
						// therefore we delegate to the application logic by forwarding server-side control to the decrypted resource)!
						// FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD
						requestDispatcher.forward(request, response);
						// FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD FORWARD
						if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("5 (exit from forward): "+httpRequest.getParameterMap().keySet());
						return true;
					}
				}
			}


		} catch (ServerAttackException e) {
			final HttpServletResponse httpResponse = (HttpServletResponse) response;
			if (this.redirectWelcomePage.length() == 0) {
				// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
				final Attack attack = this.attackHandler.handleAttack(httpRequest, clientAddress, "Client provided mismatching crypto values ("+e.getMessage()+") - and no redirect welcome page configured");
				sendDisallowedResponse(httpResponse, attack);
			} else {
				final String message = "Client provided mismatching crypto values ("+e.getMessage()+")\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
				this.attackHandler.handleRegularRequest(httpRequest, clientAddress); // = since we're about to stop this request, we log it here
				this.attackHandler.logWarningRequestMessage("Desired stop in filter processing of previously logged request: "+message);
				try {
					if (session != null) {
						session.invalidate();
					}
				} catch (IllegalStateException ignored) {}
				// response.sendRedirectDueToRecentAttack is not yet possible, since the response is not yet wrapped (will be wrapped below), so it is safe to use standard response.sendRedirect here
				httpResponse.sendRedirect( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
				// don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
			}
			return true;
		}
		return false;
	}

	private HttpSession getSession(HttpServletRequest httpRequest, HttpSession session) {
		if (session == null) {
			session = httpRequest.getSession(true);
		}
		assert session != null;
		return session;
	}

	private HttpSession checkForcedSessionInvalidation(HttpServletResponse response, HttpServletRequest httpRequest, String clientAddress, HttpSession session) throws IOException {
		final long forcedCutoff = session.getCreationTime() + (this.forcedSessionInvalidationPeriodMinutes * 60 * 1000);
		if ( System.currentTimeMillis() > forcedCutoff ) {
			this.attackHandler.logWarningRequestMessage("Forced session invalidation: "+session.getId());
			try {
				if (session != null) {
					session.invalidate();
				}
			} catch (IllegalStateException ignored) {}
			final HttpServletResponse httpResponse = response;
			if (this.redirectWelcomePage.length() == 0) {
				// send disallowed response without AttackHandler counting (i.e. we directly create the Attack object), since the session will be invalidated here (was done above) any way, so AttackHandler does not need to check if session-invalidation-on-attack is defined
				final Attack attack = new Attack("Maximum session lifetime exceeded ("+(this.forcedSessionInvalidationPeriodMinutes*60)+" seconds) - and no redirect welcome page configured");
				sendDisallowedResponse(httpResponse, attack);
			} else {
				final String message = "Maximum session lifetime exceeded ("+(this.forcedSessionInvalidationPeriodMinutes*60)+" seconds)\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
				// Session was already invalidated above
				// response.sendRedirectDueToRecentAttack is not yet possible, since the response is not yet wrapped (will be wrapped below), so it is safe to use standard response.sendRedirect here
				httpResponse.sendRedirect( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); //
				// don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
				this.attackHandler.handleRegularRequest(httpRequest, clientAddress); // = since we're about to stop this request, we log it here
				this.attackHandler.logWarningRequestMessage("Desired stop in filter processing of previously logged request: "+message);
			}
			return null;
	}
		return session;
	}

	private boolean calculateSizeLimits(HttpServletResponse response, HttpServletRequest httpRequest, String servletPath, String requestURI, String queryString) throws IOException {
		if (this.ruleDefinitions.getSizeLimitDefinitions() != null && this.ruleDefinitions.getSizeLimitDefinitions().hasEnabledDefinitions()) {
			// Header
			int headerCount = 0;
			int totalHeaderSize = 0;
			int greatestHeaderNameLength = 0;
			int greatestHeaderValueLength = 0;
			final Enumeration names = httpRequest.getHeaderNames();
			if (names != null) {
				while (names.hasMoreElements()) {
					final String name = (String) names.nextElement();
					headerCount++;
					if (name == null) continue;
					final int nameLength = name.length();
					greatestHeaderNameLength = Math.max(greatestHeaderNameLength, nameLength);
					totalHeaderSize += nameLength;
					final Enumeration values = httpRequest.getHeaders(name);
					if (values != null) {
						while (values.hasMoreElements()) {
							final String value = (String) values.nextElement();
							if (value == null) continue;
							final int valueLength = value.length();
							greatestHeaderValueLength = Math.max(greatestHeaderValueLength, valueLength);
							totalHeaderSize += valueLength;
						}
					} else logLocal("Container does not allow to access header information");
				}
			} else logLocal("Container does not allow to access header information");
			// Cookie
			int cookieCount = 0;
			int totalCookieSize = 0;
			int greatestCookieNameLength = 0;
			int greatestCookieValueLength = 0;
			final Cookie[] cookies = httpRequest.getCookies();
			if (cookies != null) {
				for (final Cookie cookie : cookies) {
					cookieCount++;
					final String comment = cookie.getComment();
					final String domain = cookie.getDomain();
					final String path = cookie.getPath();
					final String name = cookie.getName();
					final String value = cookie.getValue();
					if (comment != null) totalCookieSize += comment.length();
					if (domain != null) totalCookieSize += domain.length();
					if (path != null) totalCookieSize += path.length();
					if (name != null) {
						final int length = name.length();
						greatestCookieNameLength = Math.max(greatestCookieNameLength, length);
						totalCookieSize += length;
					}
					if (value != null) {
						final int length = value.length();
						greatestCookieValueLength = Math.max(greatestCookieValueLength, length);
						totalCookieSize += length;
					}
				}
			}
			// Request-Param
			int requestParamCount = 0;
			int totalRequestParamSize = 0;
			int greatestRequestParamNameLength = 0;
			int greatestRequestParamValueLength = 0;
			for (final Enumeration paramNames = httpRequest.getParameterNames(); paramNames.hasMoreElements();) {
				final String name = (String) paramNames.nextElement();
				requestParamCount++;
				if (name == null) continue;
				final int nameLength = name.length();
				greatestRequestParamNameLength = Math.max(greatestRequestParamNameLength, nameLength);
				totalRequestParamSize += nameLength;
				final String[] values = httpRequest.getParameterValues(name);
				if (values == null) continue;
				for (int i=0; i<values.length; i++) {
					final String value = values[i];
					final int valueLength = value.length();
					greatestRequestParamValueLength = Math.max(greatestRequestParamValueLength, valueLength);
					totalRequestParamSize += valueLength;
				}
			}
			// Query-String
			final int queryStringLength = queryString == null ? 0 : queryString.length();

			// limit exceeded ?
			if ( this.ruleDefinitions.getSizeLimitDefinitions().isSizeLimitExceeded(servletPath,requestURI,
					headerCount, cookieCount, requestParamCount,
					queryStringLength,
					greatestHeaderNameLength, greatestHeaderValueLength, totalHeaderSize,
					greatestCookieNameLength, greatestCookieValueLength, totalCookieSize,
					greatestRequestParamNameLength, greatestRequestParamValueLength, totalRequestParamSize) ) {
				final String message = "Size limit exceeded by request (therefore not logging details)";
				this.attackHandler.logWarningRequestMessage(message);
				final Attack attack = new Attack(message);
				final HttpServletResponse httpResponse = response;
				sendDisallowedResponse(httpResponse, attack);
				return true;
			}
		}
		return false;
	}

	private boolean ckeckAllowedRequestMimeTypes(HttpServletResponse response, HttpServletRequest httpRequest, String clientAddress) throws IOException {
		if (this.allowedRequestMimeTypesLowerCased != null && !this.allowedRequestMimeTypesLowerCased.isEmpty()) {
			final String mimeType = httpRequest.getContentType();
			if (mimeType != null) {
				String mimeTypeUpToFirstSemicolon = mimeType;
				final int pos = mimeType.indexOf(';');
				if (pos != -1) mimeTypeUpToFirstSemicolon = mimeType.substring(0, pos);
				if (!this.allowedRequestMimeTypesLowerCased.contains(mimeTypeUpToFirstSemicolon.toLowerCase().trim())) {
					final Attack attack = this.attackHandler.handleAttack(httpRequest, clientAddress, "Mime type of request not allowed (configurable): "+mimeType);
					final HttpServletResponse httpResponse = response;
					sendDisallowedResponse(httpResponse, attack);
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkclienAddress(HttpServletResponse response, String clientAddress) throws IOException {
		if (clientAddress != null && !ParamConsts.PATTERN_VALID_CLIENT_ADDRESS.matcher(clientAddress).find()) {
			final String message = "Strange client address (nothing found matching the expected pattern of client address) - not incrementing attack-counter since tracking attacks using strange IP addresses is useless; simply blocking the request: "+clientAddress;
			this.attackHandler.logWarningRequestMessage(message);
			final Attack attack = new Attack(message);
			final HttpServletResponse httpResponse = response;
			sendDisallowedResponse(httpResponse, attack);
			return true;
		}
		return false;
	}

	private boolean checkBlockedIps(HttpServletResponse response, String clientAddress) throws IOException {
		if (this.attackHandler.shouldBeBlocked(clientAddress)) {
			sendDisallowedResponse(response, new Attack("Client is temporarily blocked: "+clientAddress));
			return true;
		}
		return false;
	}

	private boolean defineCharacterEncoding(ServletRequest request, HttpServletResponse response, HttpServletRequest httpRequest) throws IOException {
		if (this.debug) logLocal("Original request character encoding: "+request.getCharacterEncoding());
		if (this.requestCharacterEncoding != null && this.requestCharacterEncoding.length() > 0) {
			try {
				// IMPORTANT: set the encoding here as early as possible BEFORE ANY params are read,
				// otherwise the request.setCharacterEncoding() won't work when already request parameters have been read !!!
				request.setCharacterEncoding(this.requestCharacterEncoding);
				if (this.debug) logLocal("Request character encoding set to: "+this.requestCharacterEncoding);
			} catch (UnsupportedEncodingException e) { // = wrong configuration
				this.attackHandler.handleRegularRequest(httpRequest, RequestUtils.determineClientIp(httpRequest, this.clientIpDeterminator)); // = since we're about to stop this request, we log it here
				this.attackHandler.logWarningRequestMessage("Desired stop in filter processing of previously logged request: "+"Unsupported request character encoding configured for KsWaf: "+this.requestCharacterEncoding);
				sendUnavailableMessage(response, e);
				return true;
			}
		}
		return false;
	}

	private boolean reloadRulesWhenRequired(HttpServletResponse response) throws IOException {
		try {
			if (this.ruleFileReloadingIntervalMillis > 0 && System.currentTimeMillis() > this.nextRuleReloadingTime && this.nextRuleReloadingTime > 0) {
				this.nextRuleReloadingTime += this.ruleFileReloadingIntervalMillis;
				registerRuleReloadOnNextRequest();
			}
			reloadRulesWhenRequired();
		} catch (Exception e) {
			sendUnavailableMessage(response, e);
			return true;
		}
		return false;
	}

	private boolean isFilterReady(HttpServletResponse response) throws IOException {
		try {
			if (this.configReloadingIntervalMillis > 0 && System.currentTimeMillis() > this.nextConfigReloadingTime && this.nextConfigReloadingTime > 0) {
				this.nextConfigReloadingTime += this.configReloadingIntervalMillis;
				registerConfigReloadOnNextRequest();
			}
			restartCompletelyWhenRequired();
		} catch (Exception e) {
			sendUnavailableMessage(response, e);
			return true;
		}
		return false;
	}

	private void doAfterProcessing(final RequestWrapper request, final ResponseWrapper response) {
		if (this.debug) logLocal("KaWaf:doAfterProcessing --- begin");
		final int statusCode = response.getCapturedStatus();
		final String ip = RequestUtils.determineClientIp(request,this.clientIpDeterminator);
		if (this.debug) {
			logLocal("KsWaf:doAfterProcessing current response status code (up to here): "+statusCode);
		}
		// check honeylink access
		if (this.honeyLinkMaxPerPage > 0
				&& (statusCode == HttpServletResponse.SC_BAD_REQUEST || statusCode == HttpServletResponse.SC_NOT_FOUND)) {
			String servletPathOrRequestURI = request.getServletPath();
			if (servletPathOrRequestURI == null) servletPathOrRequestURI = request.getRequestURI();
			if (HoneylinkUtils.isHoneylinkFilename(servletPathOrRequestURI)) {
				this.attackHandler.handleAttack(request, ip, "Potential honeylink accessed");
			}
		}
		// track status code
		this.httpStatusCodeCounter.trackStatusCode(ip, statusCode, request);
		// determine content type
		String contentTypeUpperCased = response.extractContentTypeUpperCased();
		if (contentTypeUpperCased == null) contentTypeUpperCased = "NULL"; // yes, the word "NULL" in configuration means "match with an unset content-type" here...so we set the variable to check against to the word "NULL" here
		contentTypeUpperCased = contentTypeUpperCased.trim();
		if (this.debug) logLocal("KsWaf:doAfterProcessing ================================== end");
	}

	private AllowedFlagWithMessage doBeforeProcessing(final RequestWrapper request, final ResponseWrapper response, final RequestDetails requestDetails, final String cryptoDetectionString, final Boolean isDecryptedFormSubmit, final Boolean wasEncryptedUrlManipulated) throws IOException, StopFilterProcessingException, NoSuchAlgorithmException {
		if (this.debug) logLocal("KsWaf:doBeforeProcessing ================================== begin");

		if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) {
			Thread.dumpStack();
		}

		// Check for invalid encoding of query string
		// Further details on URI encoding stuff: http://en.wikipedia.org/wiki/Percent-encoding
		// =========================================================
		if (this.blockInvalidEncodedQueryString && requestDetails.queryString != null) { // if defined to block wrong encodings, do so:
			try {
				URLDecoder.decode(requestDetails.queryString, DEFAULT_CHARACTER_ENCODING);
			} catch (UnsupportedEncodingException e) { // = wrong configuration
				final StopFilterProcessingException ex = new StopFilterProcessingException("Unsupported request character encoding in KsWaf: "+DEFAULT_CHARACTER_ENCODING);
				sendUnavailableMessage(response, ex);
				throw ex;
			} catch (RuntimeException e) {
				// The query string contains invalid encoded data (i.e. %XV which is not hex)
				final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Invalid encoded query string");
				return new AllowedFlagWithMessage(false, attack);
			}
		}






		// =========================================================
		// Determine if some special points are accessed (entry-points and incomding-protection-excludes, ...)
		// =========================================================
		boolean isEntryPoint,
				isIncomingReferrerProtectionExclude, isIncomingSecretTokenProtectionExclude, isIncomingParameterAndFormProtectionExclude,
				isIncomingSelectboxFieldProtectionExclude, isIncomingCheckboxFieldProtectionExclude, isIncomingRadiobuttonFieldProtectionExclude,
				isIncomingForceEntranceProtectionExclude, isIncomingSessionToHeaderBindingProtectionExclude;
		try {
			isEntryPoint = this.ruleDefinitions.getEntryPointDefinitions().isEntryPoint(request,
					requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
					requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
					requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
					requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
					requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);

			final IncomingProtectionExcludeDefinition incomingProtectionExcludeDefinition = this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().getMatchingIncomingProtectionExcludeDefinition(request,
					requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
					requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
					requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
					requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
					requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);
			isIncomingReferrerProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeReferrerProtection();
			isIncomingSecretTokenProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeSecretTokenProtection();
			isIncomingParameterAndFormProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeParameterAndFormProtection();
			isIncomingSelectboxFieldProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeSelectboxFieldProtection();
			isIncomingCheckboxFieldProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeCheckboxFieldProtection();
			isIncomingRadiobuttonFieldProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeRadiobuttonFieldProtection();
			isIncomingForceEntranceProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeForceEntranceProtection();
			isIncomingSessionToHeaderBindingProtectionExclude = incomingProtectionExcludeDefinition != null && incomingProtectionExcludeDefinition.isExcludeSessionToHeaderBindingProtection();
		} catch (Exception e) {
			e.printStackTrace();
			final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Unable to determine if it is a special point (exception during checking): "+e.getMessage());
			return new AllowedFlagWithMessage(false, attack);
		}


		// =======================================================================
		// Check for session thefts and param/form tampering + UNCOVER SUBMITTED VALUES
		// =======================================================================
		HttpSession session = request.getSession(false);
		if (session != null) {
			// be careful, if session was invalidated meanwhile
			boolean sessionInvalidated=false, isNewSession=false;
			try {
				isNewSession = session.isNew();
			} catch (IllegalStateException e) {
				sessionInvalidated = true;
			}
			// only apply session theft checks when session is NOT invalidated and NOT new (but not treating renewed sessions as new)
			//if (isNewSession && isRenewSessionPoint) isNewSession = false;
			if (!sessionInvalidated && !isNewSession) {
				// ---------------------------------------------------------
				// Session to IP binding:
				// ---------------------------------------------------------
					final String clientBoundInSession = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_CLIENT_ADDRESS_KEY);
					if (clientBoundInSession == null) {
						// Set client IP into session
						session.setAttribute(SESSION_CLIENT_ADDRESS_KEY, requestDetails.clientAddress);
					} else if (clientBoundInSession != null) {
						// Check if client has still the same IP and flag as bad request when not... (possible a session-theft)
						if (!clientBoundInSession.equals(requestDetails.clientAddress)) {
							final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Session theft (expected client '"+clientBoundInSession+"' but was called from client '"+requestDetails.clientAddress+"'): "+session.getId());
							return new AllowedFlagWithMessage(false, attack);
						}
				}
				// ---------------------------------------------------------
				// Session to HTTP header binding:
				// ---------------------------------------------------------
				if (!isIncomingSessionToHeaderBindingProtectionExclude) {
					final Map/*<String,List<String>>*/ relevantHeaders = new HashMap();
					for (final Iterator entries = requestDetails.headerMap.entrySet().iterator(); entries.hasNext();) {
						final Map.Entry entry = (Map.Entry) entries.next();
						final String name = (String) entry.getKey();
					}
					if (this.debug) logLocal("Client session-check relevant headers: "+relevantHeaders);
					final Map/*<String,List<String>>*/ headersBoundInSession = (Map) ServerUtils.getAttributeIncludingInternal(session,SESSION_CLIENT_HEADERS_KEY);
					if (headersBoundInSession == null) {
						// Set client headers into session
						session.setAttribute(SESSION_CLIENT_HEADERS_KEY, relevantHeaders);
					} else if (headersBoundInSession != null) {
						// Check if client has still the same headers and flag as bad request when not... (possible a session-theft)
						if (!headersBoundInSession.equals(relevantHeaders)) {
							final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Session theft (expected relevant headers '"+headersBoundInSession+"' do not match the headers sent by the client '"+relevantHeaders+"'): "+session.getId());
							return new AllowedFlagWithMessage(false, attack);
						}
					}
				}


				// link/form protection stuff - only apply to relevant resource types AND also watch out for "isIncomingProtectionExclude"-matches (done inside the following code)

				// ---------------------------------------------------------
				// Secret random token matching (of protective-content-injection):
				// ---------------------------------------------------------
				if (this.contentInjectionHelper.isInjectSecretTokenIntoLinks() && !isIncomingSecretTokenProtectionExclude) {
					final String expectedTokenKey = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_SECRET_RANDOM_TOKEN_KEY_KEY);
					final String expectedTokenValue = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY);
					if (expectedTokenKey != null && expectedTokenValue != null) {
						final String actualTokenValue = request.getParameter(expectedTokenKey); // here the temporarily injected param is still in the request (not in the extracted map though, but that is OK)
						if (actualTokenValue == null || !expectedTokenValue.equals(actualTokenValue)) {
							// Session theft (client provided no matching request token)
							if (isEntryPoint) {
								try {
									if (session != null) {
										session.invalidate();
										session = null;
									}
								} catch (IllegalStateException ignored) {}
								this.attackHandler.logRegularRequestMessage("Client provided no matching secret request token - definitely invalidated the session but let the request pass (since it is an entry-point): "+requestDetails.servletPath+" with query string: "+requestDetails.queryString);
							} else {
								if (DEBUG_PRINT_UNCOVERING_DETAILS) System.out.println("actualTokenValue is "+actualTokenValue+" for request query string "+request.getQueryString());
								if (this.redirectWelcomePage.length() == 0) {
									// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
									final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Session theft (client provided no matching secret request token [actualTokenValue:"+actualTokenValue+"] and it is not on an entry-point) - and no redirect welcome page configured\n\tIf the requested URL is an application defined requirement without protection tokens consider the available 'incoming protection excludes' rule definitions (and watch for previous spoofing requests that might have terminated the session and are the root cause)");
									return new AllowedFlagWithMessage(false, attack);
								} else {
									final String message = "Client provided no matching secret request token [expectedTokenValue:"+expectedTokenValue+" vs. actualTokenValue:"+actualTokenValue+"] and it is not on an entry-point (could also be caused by a session timeout)\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage+"\n\tIf the requested URL is an application defined requirement without protection tokens consider the available 'incoming protection excludes' rule definitions (and watch for previous spoofing requests that might have terminated the session and are the root cause)";
									try {
										if (session != null) {
											session.invalidate();
                                        }
									} catch (IllegalStateException ignored) {}
									response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
									throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
								}
							}
						}
					}
				}


				// ---------------------------------------------------------
				// Parameter And Form Protection uncovering (PAF):
				// ---------------------------------------------------------
				if (wasEncryptedUrlManipulated != null && wasEncryptedUrlManipulated && !isIncomingParameterAndFormProtectionExclude) {
					// potential parameter tampering (spoofing) detected
					if (this.redirectWelcomePage.length() == 0) {
						// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
						final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameters) - and no redirect welcome page configured");
						return new AllowedFlagWithMessage(false, attack);
					} else {
						final String message = "Client provided mismatching request parameters. Instead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
						try {
							if (session != null) {
								session.invalidate();
								session = null;
							}
						} catch (IllegalStateException ignored) {}
						response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
						throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
					}
				}

				if (this.contentInjectionHelper.isProtectParametersAndForms() && session != null  // "session != null" here, since the previous checks above could set the session to null
						&& !(
						this.contentInjectionHelper.isExtraStrictParameterCheckingForEncryptedLinks() && isDecryptedFormSubmit != null && !isDecryptedFormSubmit
				) )
				{
					final String expectedParameterAndFormProtectionKeyKey = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY);
					if (expectedParameterAndFormProtectionKeyKey != null) {
						final String key = request.getParameter(expectedParameterAndFormProtectionKeyKey); // here the temporarily injected param is still in the request (not in the extracted request-param-map though, but that is by design and OK)
						if (key == null && !isIncomingParameterAndFormProtectionExclude) {
							// Session theft (client provided no matching param-and-form pointer token)
							if (isEntryPoint) {
								try {
									if (session != null) {
										session.invalidate();
										session = null;
									}
								} catch (IllegalStateException ignored) {}
								this.attackHandler.logRegularRequestMessage("Client provided no matching protection token - definitely invalidated the session but let the request pass (since it is an entry-point): "+requestDetails.servletPath+" with query string: "+requestDetails.queryString);
							} else {
								if (this.redirectWelcomePage.length() == 0) {
									// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
									final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided no matching protection token and it is not on an entry-point) - and no redirect welcome page configured");
									return new AllowedFlagWithMessage(false, attack);
								} else {
									final String message = "Client provided no matching protection token and it is not on an entry-point (could also be caused by a session timeout)\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
									try {
										if (session != null) {
											session.invalidate();
											session = null;
										}
									} catch (IllegalStateException ignored) {}
									response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
									throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
								}
							}
						}
						// OK, so when we've come here, we can check if the session holds a param-and-form protection for the given key
						if (key != null) { // key can be null when we're on an entry-point
							final ParameterAndFormProtection parameterAndFormProtection = (ParameterAndFormProtection) ServerUtils.getAttributeIncludingInternal(session,INTERNAL_CONTENT_PREFIX+key);
							if (parameterAndFormProtection == null && !isIncomingParameterAndFormProtectionExclude) {
								final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation - no matching ParameterAndFormProtection in session for key: "+key);
								return new AllowedFlagWithMessage(false, attack);
							}
							if (parameterAndFormProtection != null) {
								// OK, so when we've come here, we can check the concrete parameters and form-fields
								final Set allParametersExpected = parameterAndFormProtection.getAllParameterNames();
								final Set requiredParametersExpected = parameterAndFormProtection.getRequiredParameterNames();
								final Set parametersSentFromClient = requestDetails.requestParameterMap.keySet();
								// take incoming parameter protection excludes into account when checking for spoofings/attacks
								if (!isIncomingParameterAndFormProtectionExclude) {
									if (INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE && this.debug) logLocal("Minimum parameter expectation: "+requiredParametersExpected);
									// check if fields have been added or removed by the client
									if ( !allParametersExpected.containsAll(parametersSentFromClient) // so we check if unexpected parameters were submitted...
											|| !parametersSentFromClient.containsAll(requiredParametersExpected) // and check if required-to-submit parameters have been illegaly removed by the client...
											) { // here disabled form fields are checked automatically (when configured to check) since they were already taken care of when filling the "parameterAndFormProtection" object during the previous response (see ResponseFilter)
										// potential parameter tampering (spoofing) detected
										if (this.redirectWelcomePage.length() == 0) {
											// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
											final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameters) - and no redirect welcome page configured");
											return new AllowedFlagWithMessage(false, attack);
										} else {
											final String message = "Client provided mismatching request parameters\n\tExpected maximum: "+allParametersExpected+"\n\tExpected minimum: "+requiredParametersExpected+"\n\tActually received from client: "+parametersSentFromClient+"\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
											try {
												if (session != null) {
													session.invalidate();
													session = null;
												}
											} catch (IllegalStateException ignored) {}
											response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
											throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
										}
									}
									// check if the request parameter value counts (when configured to check) have mismatches according to their min/max value counts
									if (this.contentInjectionHelper.isExtraProtectRequestParamValueCount()) {
										for (final Iterator entries = request.getOriginalParameterMap().entrySet().iterator(); entries.hasNext();) {
											final Map.Entry/*<String,String[]>*/ entry = (Map.Entry) entries.next();
											final String parameterName = (String) entry.getKey();
											final String[] parameterValues = (String[]) entry.getValue();
											final int minimum = parameterAndFormProtection.getMinimumValueCountForParameterName(parameterName);
											final int maximum = parameterAndFormProtection.getMaximumValueCountForParameterName(parameterName);
											if (parameterValues.length < minimum  ||  parameterValues.length > maximum) {
												// potential parameter tampering (spoofing) detected
												if (this.redirectWelcomePage.length() == 0) {
													// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
													final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value count) - and no redirect welcome page configured");
													return new AllowedFlagWithMessage(false, attack);
												} else {
													final String message = "Client provided mismatching request parameter value count for parameter: "+parameterName+"\n\tExpected maximum value count for this parameter: "+maximum+"\n\tExpected minimum value count for this parameter: "+minimum+"\n\tValue count for this parameter actually received from client: "+parameterValues.length+"\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
													try {
														if (session != null) {
															session.invalidate();
															session = null;
														}
													} catch (IllegalStateException ignored) {}
													response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
													throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
												}
											}
										}
									}
									// check if readonly fields (when configured to check) have mismatching (tampered) values
									if (this.contentInjectionHelper.isExtraProtectReadonlyFormFields()) {
										for (final Iterator readonlyFields = parameterAndFormProtection.getReadonlyFieldsName2ExpectedValues().entrySet().iterator(); readonlyFields.hasNext();) {
											final Map.Entry/*<String,List<String>>*/ readonlyField = (Map.Entry) readonlyFields.next();
											final String fieldname = (String) readonlyField.getKey();
											// consider the potential case where a readonly field is defined multiple times and one of them is readwrite allowed...
											if ( !parameterAndFormProtection.isAlsoReadwriteField(fieldname) ) { // ...therefore only apply readonly field protection to those readonly fields that are *not* a readwrite field at the same time
												final List/*<String>*/ expectedValues = (List) readonlyField.getValue();
												assert expectedValues != null;
												final String[] actualSubmittedValues = request.getOriginalParameterValues(fieldname);
												if (RequestUtils.isMismatch(expectedValues, actualSubmittedValues)) {
													// potential readonly field tampering (spoofing) detected
													if (this.redirectWelcomePage.length() == 0) {
														// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
														final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value for readonly field '"+fieldname+"') - and no redirect welcome page configured");
														return new AllowedFlagWithMessage(false, attack);
													} else {
														final String message = "Client provided mismatching request parameter value for readonly field '"+fieldname+"'\n\tExpected: "+expectedValues+"\n\tActually received from client: "+Arrays.asList(actualSubmittedValues)+"\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
														try {
															if (session != null) {
																session.invalidate();
																session = null;
															}
														} catch (IllegalStateException ignored) {}
														response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
														throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
													}
												}
											}
										}
									}
								}


								// Here the uncovering stuff to re-add previously removed hidden fields happens:
								requestDetails.somethingHasBeenUncovered = false;
								if (this.hiddenFormFieldProtection) {
									for (final Iterator previouslyRemovedHiddenFields = parameterAndFormProtection.getHiddenFieldsName2RemovedValues().entrySet().iterator(); previouslyRemovedHiddenFields.hasNext();) {
										final Map.Entry/*<String,List<String>>*/ previouslyRemovedHiddenField = (Map.Entry) previouslyRemovedHiddenFields.next();
										final String fieldname = (String) previouslyRemovedHiddenField.getKey();
										final List/*<String>*/ values = (List) previouslyRemovedHiddenField.getValue();
										if (fieldname != null && values != null) {
											final String[] valueArray = (String[])values.toArray(new String[0]);
											request.setParameter(fieldname, valueArray, false);
											requestDetails.somethingHasBeenUncovered = true;
										}
									}
								}


								AllowedFlagWithMessage selectboxCheckboxRadiobuttonProtectionResult;
								// Uncover SelectBox-Protection
								selectboxCheckboxRadiobuttonProtectionResult = selectboxCheckboxRadiobuttonProtection(session, requestDetails, request, response, parameterAndFormProtection.getSelectboxFieldsName2AllowedValues(),
										this.selectBoxProtection, this.selectBoxValueMasking,
										SESSION_SELECTBOX_MASKING_PREFIX, isIncomingSelectboxFieldProtectionExclude);
								if (selectboxCheckboxRadiobuttonProtectionResult != null) return selectboxCheckboxRadiobuttonProtectionResult;
								// Uncover CheckBox-Protection
								selectboxCheckboxRadiobuttonProtectionResult = selectboxCheckboxRadiobuttonProtection(session, requestDetails, request, response, parameterAndFormProtection.getCheckboxFieldsName2AllowedValues(),
										this.checkboxProtection, this.checkboxValueMasking,
										SESSION_CHECKBOX_MASKING_PREFIX, isIncomingCheckboxFieldProtectionExclude);
								if (selectboxCheckboxRadiobuttonProtectionResult != null) return selectboxCheckboxRadiobuttonProtectionResult;
								// Uncover RadioButton-Protection
								selectboxCheckboxRadiobuttonProtectionResult = selectboxCheckboxRadiobuttonProtection(session, requestDetails, request, response, parameterAndFormProtection.getRadiobuttonFieldsName2AllowedValues(),
										this.radiobuttonProtection, this.radiobuttonValueMasking,
										SESSION_RADIOBUTTON_MASKING_PREFIX, isIncomingRadiobuttonFieldProtectionExclude);
								if (selectboxCheckboxRadiobuttonProtectionResult != null) return selectboxCheckboxRadiobuttonProtectionResult;


								// now re-create the requestDetails paramMap and its variants (since now the uncovering has taken place) - but only when something was uncovered
								if (requestDetails.somethingHasBeenUncovered) {
									if (DEBUG_PRINT_UNCOVERING_DETAILS) System.out.println("********* BEFORE RE-CREATION: "+RequestUtils.printParameterMap(requestDetails.requestParameterMap));
									requestDetails.requestParameterMap = new HashMap( request.getParameterMap() ); // defensive copy of the WRAPPED REQUEST map (i.e. the request as seen by the application)
									removeTemporarilyInjectedParametersFromMap(requestDetails.requestParameterMap, session, cryptoDetectionString);
									if (isHavingEnabledRequestParameterCheckingRules) requestDetails.requestParameterMapVariants = ServerUtils.permutateVariants(requestDetails.requestParameterMap, requestDetails.nonStandardPermutationsRequired,requestDetails.decodingPermutationLevel);
									if (DEBUG_PRINT_UNCOVERING_DETAILS) System.out.println("********* AFTER RE-CREATION: "+RequestUtils.printParameterMap(requestDetails.requestParameterMap));
								}

							}
						}
					}
				}





				// SESSION AUTO-CREATE, AFTER DESIRED INVALIDATION (see above)
				if (session == null && isEntryPoint /*&& !requestDetails.isMatchingOutgoingResponseModificationExclusion*/) {
					// create or retrieve session-based crypto stuff - for later use (see below)
					if (this.contentInjectionHelper.isEncryptQueryStringInLinks()) {
						try {
							session = request.getSession(true);
							assert session != null;
							RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY);
							RequestUtils.createOrRetrieveRandomCryptoKeyFromSession(session, SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY, this.extraEncryptedValueHashProtection);
						} catch (Exception e) {
							this.attackHandler.logWarningRequestMessage("Unable to define protection content in session: "+e.getMessage());
							try {
								if (session != null) {
									session.invalidate();
									session = null;
								}
							} catch (IllegalStateException ignored) {}
							return new AllowedFlagWithMessage(false, new Attack("Unable to define protection content in session"));
						}
					}
					// create or retrieve session-based request tokens - for later use (see below)
					if (this.contentInjectionHelper.isInjectSecretTokenIntoLinks()) {
						try {
							session = request.getSession(true);
							assert session != null;
							RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_SECRET_RANDOM_TOKEN_KEY_KEY);
							RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY);
						} catch (Exception e) {
							this.attackHandler.logWarningRequestMessage("Unable to define protection content in session: "+e.getMessage());
							try {
								if (session != null) {
									session.invalidate();
									session = null;
								}
							} catch (IllegalStateException ignored) {}
							return new AllowedFlagWithMessage(false, new Attack("Unable to define protection content in session"));
						}
					}
					// now the key for param-and-form protection keys - for later use (see below)
					if (this.contentInjectionHelper.isProtectParametersAndForms()) {
						try {
							session = request.getSession(true);
							assert session != null;
							RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY); // yes, it is the key of the key
							//OLD parameterAndFormProtectionEmptyValue = ServerUtils.findReusableSessionContentKeyOrCreateNewOne(session, ParameterAndFormProtection.EMPTY);
						} catch (Exception e) {
							this.attackHandler.logWarningRequestMessage("Unable to define protection content in session: "+e.getMessage());
							try {
								if (session != null) {
									session.invalidate();
									session = null;
								}
							} catch (IllegalStateException ignored) {}
							return new AllowedFlagWithMessage(false, new Attack("Unable to define protection content in session"));
						}
					}
				}





			}
		}










		// =========================================================
		// Check against DoS limits
		// =========================================================
		if (this.denialOfServiceLimitCounter != null && this.ruleDefinitions.getDenialOfServiceLimitDefinitions().hasEnabledDefinitions()) {
			try {
				final DenialOfServiceLimitDefinition definition = this.ruleDefinitions.getDenialOfServiceLimitDefinitions().getMatchingDenialOfServiceLimitDefinition(request,
						requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
						requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
						requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
						requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
						requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);
				if (definition != null) {
					this.denialOfServiceLimitCounter.trackDenialOfServiceRequest(requestDetails.clientAddress, definition, request);
				}
			} catch (Exception e) {
				e.printStackTrace();
				final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Unable to determine if it is a DoS match (exception during checking): "+e.getMessage());
				return new AllowedFlagWithMessage(false, attack);
			}
		}


		// =========================================================
		// Check against white-list
		// =========================================================
		if (this.ruleDefinitions.getWhiteListDefinitions().hasEnabledDefinitions()) {
			try {
				final boolean isWhitelistMatch = this.ruleDefinitions.getWhiteListDefinitions().isWhitelistMatch(request,
						requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
						requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
						requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
						requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
						requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap, this.treatNonMatchingServletPathAsMatchForWhitelistRules);
				if (!isWhitelistMatch) {
					final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Request does not match a white-list definition");
					return new AllowedFlagWithMessage(false, attack);
				}
			} catch (Exception e) {
				e.printStackTrace();
				final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Unable to determine if it is a whitelist match (exception during checking): "+e.getMessage());
				return new AllowedFlagWithMessage(false, attack);
			}
		}


		// =========================================================
		// Determine if some special points are accessed (renew-session-and-token-point, captcha-point, etc.)
		// =========================================================
		boolean isRenewSessionPoint, isRenewSecretTokenPoint, isRenewParamAndFormTokenPoint, isRenewCryptoKeyPoint;
		try {
			final RenewSessionAndTokenPointDefinition renewSessionAndTokenPointDefinition = this.ruleDefinitions.getRenewSessionPointDefinitions().getMatchingRenewSessionAndTokenPointDefinition(request,
					requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
					requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
					requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
					requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
					requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);
			isRenewSessionPoint = renewSessionAndTokenPointDefinition != null && renewSessionAndTokenPointDefinition.isRenewSession();
			isRenewSecretTokenPoint = renewSessionAndTokenPointDefinition != null && renewSessionAndTokenPointDefinition.isRenewSecretToken();
			isRenewParamAndFormTokenPoint = renewSessionAndTokenPointDefinition != null && renewSessionAndTokenPointDefinition.isRenewParamAndFormToken();
			isRenewCryptoKeyPoint = renewSessionAndTokenPointDefinition != null && renewSessionAndTokenPointDefinition.isRenewCryptoKey();
		} catch (Exception e) {
			e.printStackTrace();
			final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Unable to determine if it is a special point (exception during checking): "+e.getMessage());
			return new AllowedFlagWithMessage(false, attack);
		}



		// =========================================================
		// Check for renew-session-and-token-points
		// =========================================================
		if (session != null && (isRenewSessionPoint || isRenewSecretTokenPoint || isRenewParamAndFormTokenPoint || isRenewCryptoKeyPoint)) {

			// check if we should renew the session
			if (isRenewSessionPoint) {
				final String oldSessionId = session.getId();
				// copy session content from old session into temporary map
				final Map/*<String,Object>*/ sessionContent = new HashMap();
				for (final Enumeration/*<String>*/ names = ServerUtils.getAttributeNamesIncludingInternal(session); names.hasMoreElements();) {
					final String name = (String) names.nextElement();
					final Object value = ServerUtils.getAttributeIncludingInternal(session,name);
					sessionContent.put(name, value);
				}
				// redefine the secret token keys - along with the session renewal
				if (isRenewSecretTokenPoint) {
					final String oldSecretTokenKey = (String) sessionContent.get(SESSION_SECRET_RANDOM_TOKEN_KEY_KEY);
					final String oldSecretTokenValue = (String) sessionContent.get(SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY);
					if (oldSecretTokenKey != null && oldSecretTokenValue != null) {
						// set the new secret token IDs into the session
						final String newSecretTokenKey = CryptoUtils.generateRandomToken(true);
						final String newSecretTokenValue = CryptoUtils.generateRandomToken(true);
						sessionContent.put(SESSION_SECRET_RANDOM_TOKEN_KEY_KEY, newSecretTokenKey);
						sessionContent.put(SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY, newSecretTokenValue);
						// replace the old ones with the new ones in the request (but only if the old ones were correct)
						final String oldSecretTokenValueInRequest = request.getParameter(oldSecretTokenKey);
						if (oldSecretTokenValueInRequest != null && oldSecretTokenValueInRequest.equals(oldSecretTokenValue)) {
							// OK, the token the user supplied matches the correct (old) one so we are allowed to magically add the correct (new) one to the request (and remove the old one)
							request.setParameter(newSecretTokenKey, new String[]{newSecretTokenValue}, true); // add the new one
							request.removeParameter(oldSecretTokenKey);
						}
						// also set the new redefined tokens into the response wrapper for injecting the correct (new) ones into the response
						response.redefineSecretTokenKey(newSecretTokenKey);
						response.redefineSecretTokenValue(newSecretTokenValue);
						// AND as it is the secret token (which also gets incorporated as parameter into ParameterAndFormProtection-objects) also adjust those PAF objects in session to expect the new instead of the old secret token parameter
						ServerUtils.renameSecretTokenParameterInAllCachedParameterAndFormProtectionObjects(session, oldSecretTokenKey, newSecretTokenKey);
					}
				}
				// redefine the param-and-form token keys - along with the session renewal
				if (isRenewParamAndFormTokenPoint) {
					final String oldParamAndFormTokenKey = (String) sessionContent.get(SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY);
					if (oldParamAndFormTokenKey != null) {
						// set the new param-and-form token ID into the session
						final String newParamAndFormTokenKey = CryptoUtils.generateRandomToken(true);
						sessionContent.put(SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY, newParamAndFormTokenKey);
						// replace the old ones with the new ones in the request
						final String oldParamAndFormTokenValueInRequest = request.getParameter(oldParamAndFormTokenKey);
						if (oldParamAndFormTokenValueInRequest != null) {
							// OK, magically add the new one to the request (and remove the old one)
							request.setParameter(newParamAndFormTokenKey, new String[]{oldParamAndFormTokenValueInRequest}, true); // add the new one with the old value (to keep the form association)
							request.removeParameter(oldParamAndFormTokenKey);
						}
						// also set the new redefined tokens into the response wrapper for injecting the correct (new) ones into the response
						response.redefineParameterAndFormProtectionKey(newParamAndFormTokenKey);
					}
				}
				// redefine the crypto keys - along with the session renewal
				if (isRenewCryptoKeyPoint) {
					final String oldCryptoDetectionString = (String) sessionContent.get(SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY);
					final CryptoKeyAndSalt oldCryptoKey = (CryptoKeyAndSalt) sessionContent.get(SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY);
					if (oldCryptoDetectionString != null && oldCryptoKey != null) {
						// set the new crypto keys into the session
						final String newCryptoDetectionString = CryptoUtils.generateRandomToken(true);
						final CryptoKeyAndSalt newCryptoKey = CryptoUtils.generateRandomCryptoKeyAndSalt(this.extraEncryptedValueHashProtection);
						sessionContent.put(SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY, newCryptoDetectionString);
						sessionContent.put(SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY, newCryptoKey);
						// also set the new redefined tokens into the response wrapper for injecting the correct (new) ones into the response
						response.redefineCryptoDetectionString(newCryptoDetectionString);
						response.redefineCryptoKey(newCryptoKey);
					}
				}
				// RENEW THE SESSION
				session.invalidate();
				// create a new session
				session = request.getSession(true);
				// copy content from temporary map to new session
				for (final Iterator entries = sessionContent.entrySet().iterator(); entries.hasNext();) {
					final Map.Entry/*<String,Object>*/ entry = (Map.Entry) entries.next();
					final String name = (String) entry.getKey();
					final Object value = entry.getValue();
					session.setAttribute(name, value);
				}
				this.attackHandler.logRegularRequestMessage("User is touching a renew-session-and-token-point (see the following logged request details): session will be renewed from "+oldSessionId+" to "+session.getId()+" after the following logged request");
			} else {
				// OK, so *no* session renew is wanted, but maybe the token/key renewals are still desired:
				// redefine the secret token keys - without a session renewal
				if (isRenewSecretTokenPoint) {
					final String oldSecretTokenKey = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_SECRET_RANDOM_TOKEN_KEY_KEY);
					final String oldSecretTokenValue = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY);
					if (oldSecretTokenKey != null && oldSecretTokenValue != null) {
						// set the new secret token IDs into the session
						final String newSecretTokenKey = CryptoUtils.generateRandomToken(true);
						final String newSecretTokenValue = CryptoUtils.generateRandomToken(true);
						session.setAttribute(SESSION_SECRET_RANDOM_TOKEN_KEY_KEY, newSecretTokenKey);
						session.setAttribute(SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY, newSecretTokenValue);
						// replace the old ones with the new ones in the request (but only if the old ones were correct)
						final String oldSecretTokenValueInRequest = request.getParameter(oldSecretTokenKey);
						if (oldSecretTokenValueInRequest != null && oldSecretTokenValueInRequest.equals(oldSecretTokenValue)) {
							// OK, the token the user supplied matches the correct (old) one so we are allowed to magically add the correct (new) one to the request (and remove the old one)
							request.setParameter(newSecretTokenKey, new String[]{newSecretTokenValue}, true); // add the new one
							request.removeParameter(oldSecretTokenKey);
						}
						// also set the new redefined tokens into the response wrapper for injecting the correct (new) ones into the response
						response.redefineSecretTokenKey(newSecretTokenKey);
						response.redefineSecretTokenValue(newSecretTokenValue);
						// AND as it is the secret token (which also gets incorporated as parameter into ParameterAndFormProtection-objects) also adjust those PAF objects in session to expect the new instead of the old secret token parameter
						ServerUtils.renameSecretTokenParameterInAllCachedParameterAndFormProtectionObjects(session, oldSecretTokenKey, newSecretTokenKey);
					}
				}
				// redefine the param-and-form token keys - without a session renewal
				if (isRenewParamAndFormTokenPoint) {
					final String oldParamAndFormTokenKey = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY);
					if (oldParamAndFormTokenKey != null) {
						// set the new param-and-form token ID into the session
						final String newParamAndFormTokenKey = CryptoUtils.generateRandomToken(true);
						session.setAttribute(SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY, newParamAndFormTokenKey);
						// replace the old ones with the new ones in the request
						final String oldParamAndFormTokenValueInRequest = request.getParameter(oldParamAndFormTokenKey);
						if (oldParamAndFormTokenValueInRequest != null) {
							// OK, magically add the new one to the request (and remove the old one)
							request.setParameter(newParamAndFormTokenKey, new String[]{oldParamAndFormTokenValueInRequest}, true); // add the new one with the old value (to keep the form association)
							request.removeParameter(oldParamAndFormTokenKey);
						}
						// also set the new redefined tokens into the response wrapper for injecting the correct (new) ones into the response
						response.redefineParameterAndFormProtectionKey(newParamAndFormTokenKey);
					}
				}
				// redefine the crypto keys - without a session renewal
				if (isRenewCryptoKeyPoint) {
					final String oldCryptoDetectionString = (String) ServerUtils.getAttributeIncludingInternal(session,SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY);
					final CryptoKeyAndSalt oldCryptoKey = (CryptoKeyAndSalt) ServerUtils.getAttributeIncludingInternal(session,SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY);
					if (oldCryptoDetectionString != null && oldCryptoKey != null) {
						// set the new crypto keys into the session
						final String newCryptoDetectionString = CryptoUtils.generateRandomToken(true);
						final CryptoKeyAndSalt newCryptoKey = CryptoUtils.generateRandomCryptoKeyAndSalt(this.extraEncryptedValueHashProtection);
						session.setAttribute(SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY, newCryptoDetectionString);
						session.setAttribute(SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY, newCryptoKey);
						// also set the new redefined tokens into the response wrapper for injecting the correct (new) ones into the response
						response.redefineCryptoDetectionString(newCryptoDetectionString);
						response.redefineCryptoKey(newCryptoKey);
					}
				}
			}

		}



		// log some stuff for debugging purposes
		if (this.debug) {
			// Client IP stuff
			logLocal("KsWaf:doBeforeProcessing: access from client: "+requestDetails.clientAddress+" with user-agent: "+requestDetails.agent);
			logLocal("KsWaf:doBeforeProcessing: client host name: "+requestDetails.remoteHost);
			logLocal("KsWaf:doBeforeProcessing: client port: "+requestDetails.remotePort);

			// special points stuff
			logLocal("KsWaf:doBeforeProcessing: entry-point: "+isEntryPoint);
			logLocal("KsWaf:doBeforeProcessing: renew-point (session): "+isRenewSessionPoint);
			logLocal("KsWaf:doBeforeProcessing: renew-point (secret token): "+isRenewSecretTokenPoint);
			logLocal("KsWaf:doBeforeProcessing: renew-point (param-and-form token): "+isRenewParamAndFormTokenPoint);
			logLocal("KsWaf:doBeforeProcessing: renew-point (crypto key): "+isRenewCryptoKeyPoint);
			logLocal("KsWaf:doBeforeProcessing: incoming-protection-exclude (referrer): "+isIncomingReferrerProtectionExclude);
			logLocal("KsWaf:doBeforeProcessing: incoming-protection-exclude (secret-token): "+isIncomingSecretTokenProtectionExclude);
			logLocal("KsWaf:doBeforeProcessing: incoming-protection-exclude (parameter-and-form): "+isIncomingParameterAndFormProtectionExclude);
			logLocal("KsWaf:doBeforeProcessing: incoming-protection-exclude (selectbox-field): "+isIncomingSelectboxFieldProtectionExclude);
			logLocal("KsWaf:doBeforeProcessing: incoming-protection-exclude (force-entrance): "+isIncomingForceEntranceProtectionExclude);
			logLocal("KsWaf:doBeforeProcessing: incoming-protection-exclude (session-header-binding): "+isIncomingSessionToHeaderBindingProtectionExclude);

			// HTTP header stuff
			logLocal("KsWaf:doBeforeProcessing: HTTP headers of request (see below)");
			final Enumeration headers = request.getHeaderNames();
			if (headers != null) {
				while (headers.hasMoreElements()) {
					final String headerName = (String) headers.nextElement();
					for (final Enumeration headerValues = request.getHeaders(headerName); headerValues.hasMoreElements();) {
						final String headerValue = (String) headerValues.nextElement();
						logLocal(headerName+" = "+headerValue);
					}
				}
			} else logLocal("This servlet-container does not allow the access of HTTP headers");

			// Encoding and content stuff
			logLocal("KsWaf:doBeforeProcessing: character encoding: "+requestDetails.encoding);
			logLocal("KsWaf:doBeforeProcessing: content length: "+requestDetails.contentLength);
			logLocal("KsWaf:doBeforeProcessing: content MIME type: "+requestDetails.mimeType);
			logLocal("KsWaf:doBeforeProcessing: protocol: "+requestDetails.protocol);
			logLocal("KsWaf:doBeforeProcessing: scheme: "+requestDetails.scheme);
			logLocal("KsWaf:doBeforeProcessing: secure: "+request.isSecure());

			// Local (server) NIC
			logLocal("KsWaf:doBeforeProcessing: receiving NIC address: "+requestDetails.localAddr);
			logLocal("KsWaf:doBeforeProcessing: receiving NIC name: "+requestDetails.localName);
			logLocal("KsWaf:doBeforeProcessing: receiving NIC port: "+requestDetails.localPort);
			logLocal("KsWaf:doBeforeProcessing: receiving server name: "+requestDetails.serverName);
			logLocal("KsWaf:doBeforeProcessing: receiving server port: "+requestDetails.serverPort);

			// Authentication stuff
			logLocal("KsWaf:doBeforeProcessing: auth type: "+requestDetails.authType);
			logLocal("KsWaf:doBeforeProcessing: remote user: "+requestDetails.remoteUser);
			logLocal("KsWaf:doBeforeProcessing: user principal: "+request.getUserPrincipal());

			// URI stuff
			logLocal("KsWaf:doBeforeProcessing: requested URI: "+requestDetails.uri);
			logLocal("KsWaf:doBeforeProcessing: requested URL: "+requestDetails.url);
			logLocal("KsWaf:doBeforeProcessing: servlet path: "+requestDetails.servletPath);
			logLocal("KsWaf:doBeforeProcessing: context path: "+requestDetails.contextPath);
			logLocal("KsWaf:doBeforeProcessing: path info: "+requestDetails.pathInfo);
			logLocal("KsWaf:doBeforeProcessing: path translated: "+requestDetails.pathTranslated);
			logLocal("KsWaf:doBeforeProcessing: query string: "+requestDetails.queryString);
			logLocal("KsWaf:doBeforeProcessing: method: "+requestDetails.method);

			// Request parameter stuff
			logLocal("KsWaf:doBeforeProcessing: request parameters (see below)");
			for (final Enumeration parameters = request.getParameterNames(); parameters.hasMoreElements();) {
				final String parameterName = (String) parameters.nextElement();
				final String[] parameterValues = request.getParameterValues(parameterName);
				for (int i=0; i<parameterValues.length; i++) {
					logLocal(parameterName+" = "+parameterValues[i]);
				}
			}

			// Session stuff
			logLocal("KsWaf:doBeforeProcessing: requested session-id: "+requestDetails.requestedSessionId);
			if (session != null) {
				try {
					// TODO use StringBuilder
					final String fromWhere = (requestDetails.sessionCameFromCookie?"[cookie]":"") + (requestDetails.sessionCameFromCookie?"[url]":"");
					final String existingOrNew = session.isNew() ? "new" : "existing";
					logLocal("KsWaf:doBeforeProcessing: "+existingOrNew+" session ("+fromWhere+"): "+session.getId());
				} catch (IllegalStateException e) {
					logLocal("Unable to log session: "+e.getMessage());
				}
			}
		}




		// =========================================================
		// Check for duplicate headers
		// =========================================================
		if (this.blockRequestsWithDuplicateHeaders) {
			String duplicateHeaderName = null;
			for (final Iterator entries = requestDetails.headerMap.entrySet().iterator(); entries.hasNext();) {
				final Map.Entry/*<String,String[]>*/ entry = (Map.Entry) entries.next();
				final String name = (String) entry.getKey();
				final String[] values = (String[]) entry.getValue();
				if (values.length > 1) {
					duplicateHeaderName = name;
					break;
				}
			}
			if (duplicateHeaderName != null) {
				final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Duplicate request headers detected: "+duplicateHeaderName);
				return new AllowedFlagWithMessage(false, attack);
			}
		}

		// =========================================================
		// Check for strange (unknown or missing) referring URLs
		// =========================================================
		if (!isEntryPoint && !isIncomingReferrerProtectionExclude) {
			if (requestDetails.referrer == null) { // treat missing referrer as potential spoofing
				if (this.blockRequestsWithMissingReferrer) {
					if (this.redirectWelcomePage.length() == 0) {
						// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
						final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Missing referrer header in request to non-entry point - and no redirect welcome page configured");
						return new AllowedFlagWithMessage(false, attack);
					} else {
						final String message = "Missing referrer header in request to non-entry point\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
						try {
							if (session != null) {
								session.invalidate();
								session = null;
							}
						} catch (IllegalStateException ignored) {}
						response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
						throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
					}
				}
			} else if ( !ServerUtils.isSameServer(requestDetails.referrer,requestDetails.url) ) { // treat wrong referrer as potential spoofing
				if (this.blockRequestsWithUnknownReferrer) {
					if (this.redirectWelcomePage.length() == 0) {
						// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
						final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Unknown (strange) referrer header in request to non-entry point (not matching the server that is to be accessed) - and no redirect welcome page configured");
						return new AllowedFlagWithMessage(false, attack);
					} else {
						final String message = "Unknown (strange) referrer header in request to non-entry point (not matching the server that is to be accessed)\n\tURL: "+requestDetails.url+"\n\tReferrer: "+requestDetails.referrer+"\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
						try {
							if (session != null) {
								session.invalidate();
								session = null;
							}
						} catch (IllegalStateException ignored) {}
						response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
						throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
					}
				}
			}
		}


		// =========================================================
		// Check for bad request patterns
		// =========================================================
		try {
			final BadRequestDefinition badRequestDefinition = this.ruleDefinitions.getBadRequestDefinitions().getMatchingBadRequestDefinition(request,
					requestDetails.servletPath, requestDetails.contextPath, requestDetails.pathInfo, requestDetails.pathTranslated, requestDetails.clientAddress, requestDetails.remoteHost, requestDetails.remotePort,
					requestDetails.remoteUser, requestDetails.authType, requestDetails.scheme, requestDetails.method, requestDetails.protocol, requestDetails.mimeType, requestDetails.encoding, requestDetails.contentLength,
					requestDetails.headerMapVariants, requestDetails.url, requestDetails.uri, requestDetails.serverName, requestDetails.serverPort, requestDetails.localAddr, requestDetails.localName, requestDetails.localPort, requestDetails.country,
					requestDetails.cookieMapVariants, requestDetails.requestedSessionId, requestDetails.queryStringVariants,
					requestDetails.requestParameterMapVariants, requestDetails.requestParameterMap);
			if (badRequestDefinition != null) { // when "badRequestDefinition" is not null, it is a bad-request
				final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Bad request ("+badRequestDefinition.getDescription()+"): "+badRequestDefinition.getIdentification());
				return new AllowedFlagWithMessage(false, attack);
			}
		} catch (Exception e) {
			e.printStackTrace();
			final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Unable to determine if it is a bad-request (exception during bad-request checking): "+e.getMessage());
			return new AllowedFlagWithMessage(false, attack);
		}


		// =========================================================
		// Entrance-Enforcement of the web-application through an entry-point in the session
		// =========================================================
		if (this.forceEntranceThroughEntryPoints) {
			if (isEntryPoint) { // = the user comes through an entry-point and we record that
				if (this.contentInjectionHelper.isMatchingOutgoingResponseModificationExclusion(requestDetails.servletPath,requestDetails.uri)) {
					// TODO: log also to another log ? since otherwise it will only be readable when pre-/post attack logging sends the messages to the log file on an attack
					this.attackHandler.logRegularRequestMessage("Poor configuration: Entry-point definition is also an outgoing response modification excluded page match. If it is impossible to avoid that overlap remember to add the dynamic pages, where this page links to, also to the entry-point definitions.");
				}
				// auto-create session if no session exists
				if (session == null) {
					session = request.getSession(true);
					this.attackHandler.logRegularRequestMessage("Auto-created web session on entry-point: "+session.getId());
				}
				session.setAttribute(SESSION_ENTRY_POINT_TOUCHED_KEY, Boolean.TRUE);
				// (re)define the secret tokens from the session in the response...
				// this is important to ensure the invariant that every possible raising (setting) of the touched-an-entry-point flag also ensures that secret tokens and crypto stuff are present and expected in the session!
				response.redefineSecretTokenKey(RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_SECRET_RANDOM_TOKEN_KEY_KEY));
				response.redefineSecretTokenValue(RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY));
				response.redefineCryptoDetectionString(RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY));
				response.redefineCryptoKey(RequestUtils.createOrRetrieveRandomCryptoKeyFromSession(session, SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY, this.extraEncryptedValueHashProtection));
				response.redefineParameterAndFormProtectionKey(RequestUtils.createOrRetrieveRandomTokenFromSession(session, SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY));
				this.attackHandler.logRegularRequestMessage("User is entering the web application through an entry-point (see the following logged request details)");
			} else { // = as this is not an entry-point page request, we have to check if the user did already come through an entry-point
				// when the session is null or the session exists but does not have the SESSION_ENTRY_POINT_TOUCHED_KEY flag
				// then the user is cheating and did not came through an entry-point
				boolean isSessionValidAndFine = false;
				if (session != null) {
					try {
						// check if "already touched an entry-point"-flag is set in session
						if (Boolean.TRUE.equals(ServerUtils.getAttributeIncludingInternal(session,SESSION_ENTRY_POINT_TOUCHED_KEY))) isSessionValidAndFine = true;
					} catch (IllegalStateException e) {
						isSessionValidAndFine = false; // as the session is already invalidated
					} catch (IllegalArgumentException e) {
						isSessionValidAndFine = false;
					}
				}
				if (!isSessionValidAndFine && !isIncomingForceEntranceProtectionExclude) {
//                if (session == null || ServerUtils.getAttributeIncludingInternal(session,SESSION_ENTRY_POINT_TOUCHED_KEY) == null) {
					// NOTE: this could also be a simple session-timeout
					if (this.redirectWelcomePage.length() == 0) {
						// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
						final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Direct access without proper web-site entry (and no redirect welcome page configured)");
						return new AllowedFlagWithMessage(false, attack);
					} else {
						final String message = (session==null?"Potential session-timeout (NOT on entry-point)":"User entered application NOT through an entry-point (could also be caused by a session timeout)")+"\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
						try {
							if (session != null) {
								session.invalidate();
								session = null;
							}
						} catch (IllegalStateException ignored) {}
						response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
						throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
					}
				}

			}
		}


		// =========================================================
		// When we've come here, everything is fine and we welcome the user...
		// "...here is your welcome cocktail...."
		// =========================================================
		if (this.debug) logLocal("KsWaf:doBeforeProcessing --- end");
		this.attackHandler.handleRegularRequest(request, requestDetails.clientAddress);
		return new AllowedFlagWithMessage(true);
	}

	private AllowedFlagWithMessage selectboxCheckboxRadiobuttonProtection(final HttpSession session, final RequestDetails requestDetails, final RequestWrapper request, final ResponseWrapper response, final Map/*<String,List<String>>*/ fieldsName2AllowedValues,
                                                                          final boolean isProtectionEnabled, final boolean isMaskingEnabled, final String maskingPrefixSessionKey, final boolean isIncomingFieldProtectionExclude) throws StopFilterProcessingException, IOException {
		// Here we uncover and check the select/check/radiobox-protected values (anyway even when isIncomingSelect/Check/RadioboxFieldProtectionExclude is true):
		// BUT: On all "treat as attack/spoofing" things here only treat as attack/spoofing when isIncomingSelect/Check/RadioboxFieldProtectionExclude is false
		if (isProtectionEnabled) {
			if (fieldsName2AllowedValues != null && !fieldsName2AllowedValues.isEmpty()) {
				final String maskingPrefix = (String) ServerUtils.getAttributeIncludingInternal(session, maskingPrefixSessionKey);
				for (final Iterator entries = fieldsName2AllowedValues.entrySet().iterator(); entries.hasNext();) {
					final Map.Entry/*<String,List<String>>*/ entry = (Map.Entry) entries.next();
					final String fieldName = (String) entry.getKey();
					final List/*<String>*/ allowedValues = (List) entry.getValue();
					final String[] submittedValues = request.getParameterValues(fieldName);
					if (submittedValues != null && submittedValues.length > 0) {
						// work on this field:
						if (isMaskingEnabled) { // uncover (when they were masked)
							if (maskingPrefix == null) { // spoofing, since the prefix must be in the session
								if (!isIncomingFieldProtectionExclude) {
									// potential select/check/radiobox field tampering (spoofing) detected
									if (this.redirectWelcomePage.length() == 0) {
										// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
										final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"') - and no redirect welcome page configured");
										return new AllowedFlagWithMessage(false, attack);
									} else {
										final String message = "Client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"'\n\tUnable to locate the random masking prefix in the session.\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
										try {
											if (session != null) {
												session.invalidate();
												//session = null;
											}
										} catch (IllegalStateException ignored) {}
										response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
										throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
									}
								}
							}
							final String[] uncoveredValues = new String[submittedValues.length];
							for (int i=0; i<submittedValues.length; i++) {
								try {
									// check if the prefix is there and strip it off from the id
									if (maskingPrefix != null) {
										if (submittedValues[i].indexOf(maskingPrefix) == 0) { // found prefix at first position
											// strip it off
											submittedValues[i] = submittedValues[i].substring(maskingPrefix.length());
										} else { // spoofing, since the prefix must be at there and at the first position
											if (!isIncomingFieldProtectionExclude) {
												// potential select/check/radiobox field tampering (spoofing) detected
												if (this.redirectWelcomePage.length() == 0) {
													// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
													final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"') - and no redirect welcome page configured");
													return new AllowedFlagWithMessage(false, attack);
												} else {
													final String message = "Client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"'\n\tUnable to locate the random masking prefix in the submitted identifier.\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
													try {
														if (session != null) {
															session.invalidate();
															//session = null;
														}
													} catch (IllegalStateException ignored) {}
													response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
													throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
												}
											}
										}
									}
									// when we've come here, and select/check/radioBoxMaskingPrefix is still null, then it is on an incoming protection exclude
									final int submittedId = Integer.parseInt(submittedValues[i]);
									final String uncoveredValue = (String) allowedValues.get(submittedId);
									uncoveredValues[i] = uncoveredValue;
								} catch (NumberFormatException e) { // = someone has spoofed the form field by altering the masked index into something non-numeric
									if (isIncomingFieldProtectionExclude) {
										uncoveredValues[i] = submittedValues[i]; // simply use the submitted value since it is an incoming protection exclude here
									} else {
										// potential select/check/radiobox field tampering (spoofing) detected
										if (this.redirectWelcomePage.length() == 0) {
											// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
											final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"') - and no redirect welcome page configured");
											return new AllowedFlagWithMessage(false, attack);
										} else {
											final String message = "Client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"'\n\tUnable to number-parse the submitted identifier.\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
											try {
												if (session != null) {
													session.invalidate();
													//session = null;
												}
											} catch (IllegalStateException ignored) {}
											response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
											throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
										}
									}
								} catch (IndexOutOfBoundsException e) { // = someone has spoofed the form field by altering the masked index into something out of the number range of allowed values (allowed indexes)
									if (isIncomingFieldProtectionExclude) {
										uncoveredValues[i] = submittedValues[i]; // simply use the submitted value since it is an incoming protection exclude here
									} else {
										// potential select/check/radiobox field tampering (spoofing) detected
										if (this.redirectWelcomePage.length() == 0) {
											// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
											final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"') - and no redirect welcome page configured");
											return new AllowedFlagWithMessage(false, attack);
										} else {
											final String message = "Client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"'\n\tThe submitted identifier is out of range of allowed values.\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
											try {
												if (session != null) {
													session.invalidate();
													//session = null;
												}
											} catch (IllegalStateException ignored) {}
											response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
											throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
										}
									}
								}
							}
							// write the successfully uncovered values into the request (overwriting the submitted values)
							request.setParameter(fieldName, uncoveredValues, true);
							requestDetails.somethingHasBeenUncovered = true;
						} else { // no uncovering, so at least check against mismatches
							if (!isIncomingFieldProtectionExclude) {
								for (int i=0; i<submittedValues.length; i++) {
									if (!allowedValues.contains(submittedValues[i])) {
										// potential select/check/radiobox field tampering (spoofing) detected
										if (this.redirectWelcomePage.length() == 0) {
											// as no redirect welcome page is defined we have to treat it as a potential attack nevertheless
											final Attack attack = this.attackHandler.handleAttack(request, requestDetails.clientAddress, "Parameter and/or form manipulation (client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"') - and no redirect welcome page configured");
											return new AllowedFlagWithMessage(false, attack);
										} else {
											final String message = "Client provided mismatching request parameter value for selectbox/checkbox/radiobutton field '"+fieldName+"'\n\tAllowed: "+allowedValues+"\n\tActually received from client: "+Arrays.asList(submittedValues)+"\n\tInstead of letting the request pass we invalidate the session and redirect to the welcome page: "+this.redirectWelcomePage;
											try {
												if (session != null) {
													session.invalidate();
													//session = null;
												}
											} catch (IllegalStateException ignored) {}
											response.sendRedirectDueToRecentAttack( /*response.encodeRedirectURL(*/this.redirectWelcomePage/*)*/ ); // = by design we don't session-encode the URL here
											throw new StopFilterProcessingException(message); // = don't treat as attack but also don't let user pass, so simply after sending the redirect, stop the further processing
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return null; // = indicating a "please just continue normally" message to the caller
	}

	private void registerConfigReloadOnNextRequest() {
		  this.restartCompletelyOnNextRequest = true;
	}

	private void registerRuleReloadOnNextRequest() {
		this.reloadRulesOnNextRequest = true;
	}

	private void removeTemporarilyInjectedParametersFromRequest(final RequestWrapper request, final String cryptoDetectionString) {
		if (request == null) return;
		// remove the encrpyted param
		if (cryptoDetectionString != null && cryptoDetectionString.trim().length() > 0) request.removeEncryptedQueryString(cryptoDetectionString);
		// remove the rest
		final HttpSession session = request.getSession(false);
		if (session == null) return;
		try {
			// remove the protective parameters from the request (the names of those protective parameters are stored in the session, so fetch them from the session)
			request.removeParameter((String) ServerUtils.getAttributeIncludingInternal(session,SESSION_SECRET_RANDOM_TOKEN_KEY_KEY));
			request.removeParameter((String) ServerUtils.getAttributeIncludingInternal(session,SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY));
		} catch (IllegalStateException ignored) {} // = session already invalidated
	}

	private void sendProcessingError(final Throwable t, final HttpServletResponse response) {
		logLocal("Unable to process filter chain and unable to handle exception: "+t);
		t.printStackTrace();
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	private void sendUnavailableMessage(final HttpServletResponse response, final Exception exception) {
		// ID
		// this is an uncaught exception so use a generic timestamp
		final String logReferenceId = "time "+System.currentTimeMillis();
		// details
		final StringBuilder exceptionDetails = new StringBuilder();
		final String exceptionMessage = exception == null ? "No exception details available" : exception.getMessage();
		if (this.attackHandler != null) this.attackHandler.logWarningRequestMessage("Unable to initialize protection layer:\n\t"+exceptionDetails.toString().replaceAll("\n","\n\t"));
	}

	private void sendDisallowedResponse(final HttpServletResponse response, final Attack attack) {
		if (attack == null) throw new IllegalArgumentException("attack must not be null");
		// ID
		String logReferenceId = attack.getLogReferenceId();
		if (logReferenceId !=null) {
			// this is a logged attack
			logReferenceId = "log "+logReferenceId;
		} else {
			// this is an unlogged attack (like a black-listed client or something) so use a generic timestamp instead
			logReferenceId = "time "+System.currentTimeMillis();
		}
		// details
		final String attackDetails;

	}

	private void removeTemporarilyInjectedParametersFromMap(final Map parameterMap, final HttpSession session, final String cryptoDetectionString) {
		if (parameterMap == null) return;
		// remove the encrpyted param
		if (!StringUtils.isEmpty(cryptoDetectionString)) removeKeysContainingCryptoDetectionString(parameterMap, cryptoDetectionString);
		// remove the rest
		if (session == null) return;
		try {
			// remove the protective parameters from the request (the names of those protective parameters are stored in the session, so fetch them from the session)
			removeKey(parameterMap, (String) ServerUtils.getAttributeIncludingInternal(session,ParamConsts.SESSION_SECRET_RANDOM_TOKEN_KEY_KEY));
			removeKey(parameterMap, (String) ServerUtils.getAttributeIncludingInternal(session,ParamConsts.SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY));
		} catch (IllegalStateException ignored) {} // = session already invalidated
	}

	private void removeKey(final Map parameterMap, final String key) {
		if (StringUtils.isEmpty(key)) return;
		parameterMap.remove(key);
	}

	private void removeKeysContainingCryptoDetectionString(final Map parameterMap, final String cryptoDetectionString) {
		if (cryptoDetectionString == null || cryptoDetectionString.trim().length() == 0) return;
		for (final Iterator keys = parameterMap.keySet().iterator(); keys.hasNext();) {
			final String key = (String) keys.next();
			if (key.contains(cryptoDetectionString)) keys.remove();
		}
	}

	private void reloadRulesWhenRequired() throws RuleLoadingException {
		if (this.reloadRulesOnNextRequest) {
			List messages = null; // to have the logging outside the synchronized block
			synchronized (this.o_reloadRulesOnNextRequest) {
				if (this.reloadRulesOnNextRequest) {
					try {
						isHavingEnabledQueryStringCheckingRules = false;
						isHavingEnabledRequestParameterCheckingRules = false;
						isHavingEnabledHeaderCheckingRules = false;
						isHavingEnabledCookieCheckingRules = false;
						messages = new ArrayList(); // here we reload all types of rule-files, regardless if "extends RequestDefinition" or "extends SimpleDefinitions":
						if (this.ruleDefinitions.getWhiteListDefinitions() != null) {
							messages.add( this.ruleDefinitions.getWhiteListDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getWhiteListDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getWhiteListDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getWhiteListDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getWhiteListDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getBadRequestDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getBadRequestDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getBadRequestDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getBadRequestDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getBadRequestDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getBadRequestDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getDenialOfServiceLimitDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getDenialOfServiceLimitDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getDenialOfServiceLimitDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getDenialOfServiceLimitDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getDenialOfServiceLimitDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getDenialOfServiceLimitDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getEntryPointDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getEntryPointDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getEntryPointDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getEntryPointDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getEntryPointDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getEntryPointDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getOptimizationHintDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getOptimizationHintDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getOptimizationHintDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getOptimizationHintDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getOptimizationHintDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getOptimizationHintDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getRenewSessionPointDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getRenewSessionPointDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getRenewSessionPointDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getRenewSessionPointDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getRenewSessionPointDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getRenewSessionPointDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}

						if (this.ruleDefinitions.getIncomingProtectionExcludeDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getIncomingProtectionExcludeDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getResponseModificationDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getResponseModificationDefinitions().parseDefinitions() );
							if (this.ruleDefinitions.getResponseModificationDefinitions().isHavingEnabledQueryStringCheckingRules()) isHavingEnabledQueryStringCheckingRules = true;
							if (this.ruleDefinitions.getResponseModificationDefinitions().isHavingEnabledRequestParamCheckingRules()) isHavingEnabledRequestParameterCheckingRules = true;
							if (this.ruleDefinitions.getResponseModificationDefinitions().isHavingEnabledHeaderCheckingRules()) isHavingEnabledHeaderCheckingRules = true;
							if (this.ruleDefinitions.getResponseModificationDefinitions().isHavingEnabledCookieCheckingRules()) isHavingEnabledCookieCheckingRules = true;
						}
						if (this.ruleDefinitions.getTotalExcludeDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getTotalExcludeDefinitions().parseDefinitions() );
						}
						if (this.ruleDefinitions.getContentModificationExcludeDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getContentModificationExcludeDefinitions().parseDefinitions() );
						}
						if (this.ruleDefinitions.getSizeLimitDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getSizeLimitDefinitions().parseDefinitions() );
						}
						if (this.ruleDefinitions.getMultipartSizeLimitDefinitions()!= null) {
							messages.add( this.ruleDefinitions.getMultipartSizeLimitDefinitions().parseDefinitions() );
						}
						if (this.ruleDefinitions.getDecodingPermutationDefinitions() != null) {
							messages.add( this.ruleDefinitions.getDecodingPermutationDefinitions().parseDefinitions() );
						}
						if (this.ruleDefinitions.getFormFieldMaskingExcludeDefinitions() != null) {
							messages.add( this.ruleDefinitions.getFormFieldMaskingExcludeDefinitions().parseDefinitions() );
						}
						this.reloadRulesOnNextRequest = false;
					} catch (RuleLoadingException e) {
						this.reloadRulesOnNextRequest = true;
						logLocal("Unable to (re)load security rules", e);
						throw e;
					} catch (RuntimeException e) {
						this.reloadRulesOnNextRequest = true;
						logLocal("Unable to (re)load security rules", e);
						throw e;
					}
				}
			}
			// to have the actual logging happen outside of the synchronized block:
			if (messages != null && !messages.isEmpty()) {
				for (final Iterator iter = messages.iterator(); iter.hasNext();) {
					final String message = (String) iter.next();
					logLocal(message);
				}
			}
		}
	}

	private ConfigurationManager getConfigurationManager() throws UnavailableException {
		if (this.filterConfig == null)
			throw new IllegalStateException("Filter must be initialized via web container before 'init()' this method may be called");
		try {
			destroy();
		} catch (RuntimeException e) {
			logLocal("Unable to destroy configuration during (re-)initialization", e);
		}
		ConfigurationManager configManager;
		try {
			configManager = new ConfigurationManager(this.filterConfig);
			logLocal("ConfigurationManager: " + configManager);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Unable to initialize ConfigurationManager (caught ClassNotFoundException): " + e.getMessage());
		} catch (InstantiationException e) {
			throw new UnavailableException("Unable to initialize ConfigurationManager (caught InstantiationException): " + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new UnavailableException("Unable to initialize ConfigurationManager (caught IllegalAccessException): " + e.getMessage());
		} catch (FilterConfigurationException e) {
			throw new UnavailableException("Unable to initialize ConfigurationManager (caught FilterConfigurationException): " + e.getMessage());
		} catch (RuntimeException e) {
			throw new UnavailableException("Unable to initialize ConfigurationManager (caught RuntimeException): " + e.getMessage());
		}
		assert (configManager != null);
		return configManager;
	}

	private static final class AllowedFlagWithMessage {
		private final boolean allowed;
		private Attack attack;
		AllowedFlagWithMessage(final boolean allowed) {
			this.allowed = allowed;
		}
		AllowedFlagWithMessage(final boolean allowed, final Attack attack) {
			this(allowed);
			this.attack = attack;
		}
		boolean isAllowed() {
			return this.allowed;
		}
		Attack getAttack() {
			return this.attack;
		}
	}

	private void loadRuleDefinition(String value) throws UnavailableException {
		RequestDefinitionContainer container;
		try {
			assert this.ruleFileLoaderClass != null;
			final RuleFileLoader ruleFileLoader = (RuleFileLoader) this.ruleFileLoaderClass.newInstance();
			ruleFileLoader.setFilterConfig(filterConfig);
			ruleFileLoader.setPath(value);
			container = FilterLoadConfigUtils.createNewRuleContainer(value,ruleFileLoader);
			final String message = container.parseDefinitions();
			if (container.isHavingEnabledQueryStringCheckingRules())
				isHavingEnabledQueryStringCheckingRules = true;
			if (container.isHavingEnabledRequestParamCheckingRules())
				isHavingEnabledRequestParameterCheckingRules = true;
			if (container.isHavingEnabledHeaderCheckingRules())
				isHavingEnabledHeaderCheckingRules = true;
			if (container.isHavingEnabledCookieCheckingRules())
				isHavingEnabledCookieCheckingRules = true;
			logLocal(message);
		} catch (Exception ex) {
			throw new UnavailableException("Unable to load " + value +  " definitions: " + ex.getMessage());
		}
		assert container != null;
	}

	private void loadDefinition(String value) throws UnavailableException {
		SimpleDefinitionContainer container;
		try {
			assert this.ruleFileLoaderClass != null;
			final RuleFileLoader ruleFileLoader = (RuleFileLoader) this.ruleFileLoaderClass.newInstance();
			ruleFileLoader.setFilterConfig(filterConfig);
			ruleFileLoader.setPath(value);
			container = FilterLoadConfigUtils.createNewContainer(value, ruleFileLoader);
			final String message = container.parseDefinitions();
			setExcludeDefinitionsForContentInjectionhelper(value, container);
			logLocal(message);
		} catch (Exception ex) {
			throw new UnavailableException("Unable to load form-field-masking-excludes definitions: " + ex.getMessage());
		}
		assert container != null;
	}

	private void setExcludeDefinitionsForContentInjectionhelper(String value, SimpleDefinitionContainer container){
		if (StringUtils.equalsIgnoreCase(MODIFICATION_EXCLUDES_DEFAULT, value)){
			this.contentInjectionHelper.setContentModificationExcludeDefinitions((ContentModificationExcludeDefinitionContainer) container);
		} else if (StringUtils.equalsIgnoreCase("form-field-masking-excludes", value))
			this.contentInjectionHelper.setFormFieldMaskingExcludeDefinitions((FormFieldMaskingExcludeDefinitionContainer) container);
	}
}
