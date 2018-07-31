package com.ks.utils;

import java.io.File;
import java.util.regex.Pattern;

public class ParamConsts {

	public static final File TEMP_DIRECTORY = null;
	public static final int TREE_MATCHING_THRSHOLD = 60;
	public static final boolean REMOVE_CONTENT_LENGTH_FOR_MODIFIABLE_RESPONSES = true;
	public static final boolean REMOVE_COMPRESSION_ACCEPT_ENCODING_HEADER_VALUES = false;
	public static final boolean APPEND_EQUALS_SIGN_TO_VALUELESS_URL_PARAM_NAMES = true; // required for WebSphere
	public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
	public static final String PARAM_DEBUG = "Debug";
	public static final String PARAM_SHOW_TIMINGS = "ShowTimings";
	public static final String PARAM_BLOCK_ATTACKING_CLIENTS_THRESHOLD = "BlockAttackingClientsThreshold";

	public static final String PARAM_ATTACK_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "AttackReplyStatusCodeOrMessageResource";
	public static final String PARAM_EXCEPTION_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "ExceptionReplyStatusCodeOrMessageResource";
	public static final String PARAM_CONFIG_MISSING_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "ConfigurationMissingReplyStatusCodeOrMessageResource";

	public static final String PARAM_FLUSH_RESPONSE = "FlushResponse";
	public static final String PARAM_INVALIDATE_SESSION_ON_ATTACK = "InvalidateSessionOnAttack";
	public static final String PARAM_BLOCK_RESPONSE_HEADERS_WITH_CRLF = "BlockResponseHeadersWithCRLF";
	public static final String PARAM_BLOCK_REQUESTS_WITH_UNKNOWN_REFERRER = "BlockRequestsWithUnknownReferrer";
	public static final String PARAM_BLOCK_REQUESTS_WITH_MISSING_REFERRER = "BlockRequestsWithMissingReferrer";
	public static final String PARAM_BLOCK_REQUESTS_WITH_DUPLICATE_HEADERS = "BlockRequestsWithDuplicateHeaders";
	public static final String PARAM_BLOCK_NON_LOCAL_REDIRECTS = "BlockNonLocalRedirects";

	public static final String PARAM_SECRET_TOKEN_LINK_INJECTION = "SecretTokenLinkInjection";
	public static final String PARAM_ENCRYPT_QUERY_STRINGS = "QueryStringEncryption";
	public static final String PARAM_PARAMETER_AND_FORM_PROTECTION = "ParameterAndFormProtection";
	public static final String PARAM_EXTRA_DISABLED_FORM_FIELD_PROTECTION = "ExtraDisabledFormFieldProtection";
	public static final String PARAM_EXTRA_READONLY_FORM_FIELD_PROTECTION = "ExtraReadonlyFormFieldProtection";
	public static final String PARAM_EXTRA_REQUEST_PARAM_VALUE_COUNT_PROTECTION = "ExtraRequestParamValueCountProtection";
	public static final String PARAM_EXTRA_HIDDEN_FORM_FIELD_PROTECTION = "ExtraHiddenFormFieldProtection";
	public static final String PARAM_EXTRA_SELECTBOX_PROTECTION = "ExtraSelectboxProtection";
	public static final String PARAM_EXTRA_RADIOBUTTON_PROTECTION = "ExtraRadiobuttonProtection";
	public static final String PARAM_EXTRA_CHECKBOX_PROTECTION = "ExtraCheckboxProtection";
	public static final String PARAM_EXTRA_SELECTBOX_VALUE_MASKING = "ExtraSelectboxValueMasking";
	public static final String PARAM_EXTRA_RADIOBUTTON_VALUE_MASKING = "ExtraRadiobuttonValueMasking";
	public static final String PARAM_EXTRA_CHECKBOX_VALUE_MASKING = "ExtraCheckboxValueMasking";
	public static final String PARAM_EXTRA_HASH_PROTECTION = "ExtraEncryptedValueHashProtection";
	public static final String PARAM_EXTRA_FULL_PATH_PROTECTION = "ExtraEncryptedFullPathProtection";
	public static final String PARAM_EXTRA_MEDIUM_PATH_REMOVAL = "ExtraEncryptedMediumPathRemoval";
	public static final String PARAM_EXTRA_FULL_PATH_REMOVAL = "ExtraEncryptedFullPathRemoval";
	public static final String PARAM_EXTRA_STRICT_PARAMETER_CHECKING_FOR_ENCRYPTED_LINKS = "ExtraStrictParameterCheckingForLinks";


	public static final String PARAM_PATH_TO_BAD_REQUEST_FILES = "PathToBadRequestFiles";
	public static final String PARAM_PATH_TO_WHITELIST_REQUESTS_FILES = "PathToWhitelistRequestFiles";
	public static final String PARAM_PATH_TO_ENTRY_POINT_FILES = "PathToEntryPointFiles";
	public static final String PARAM_PATH_TO_OPTIMIZATION_HINT_FILES = "PathToOptimizationHintFiles";
	public static final String PARAM_PATH_TO_DOS_LIMIT_FILES = "PathToDenialOfServiceLimitFiles";
	public static final String PARAM_PATH_TO_RENEW_SESSION_AND_TOKEN_POINT_FILES = "PathToRenewSessionAndTokenPointFiles";
	public static final String PARAM_PATH_TO_INCOMING_PROTECTION_EXCLUDE_FILES = "PathToIncomingProtectionExcludeFiles";
	public static final String PARAM_PATH_TO_RESPONSE_MODIFICATION_FILES = "PathToResponseModificationFiles"; // TODO: rename (legacy-safe) to link-patterns

	public static final String PARAM_PATH_TO_CONTENT_MODIFICATION_EXCLUDE_FILES = "PathToContentModificationExcludeFiles";
	public static final String PARAM_PATH_TO_TOTAL_EXCLUDE_FILES = "PathToTotalExcludeFiles";
	public static final String PARAM_PATH_TO_SIZE_LIMIT_FILES = "PathToSizeLimitFiles";
	public static final String PARAM_PATH_TO_MULTIPART_SIZE_LIMIT_FILES = "PathToMultipartSizeLimitFiles";
	public static final String PARAM_PATH_TO_DECODING_PERMUTATION_FILES = "PathToDecodingPermutationFiles";
	public static final String PARAM_PATH_TO_FORM_FIELD_MASKING_EXCLUDE_FILES = "PathToFormFieldMaskingExcludeFiles";

	public static final String RESPONSE_MODIFICATIONS_DEFAULT = "response-modifications";
	public static final String MODIFICATION_EXCLUDES_DEFAULT = "content-modification-excludes";

	public static final String PARAM_CLIENT_IP_DETERMINATION = "ClientIpDetermination";
	public static final String PARAM_MASK_AMPERSANDS_IN_LINK_ADDITIONS = "MaskAmpersandsInLinkAdditions";
	public static final String PARAM_STRIP_HTML_COMMENTS = "StripHtmlComments";
	public static final String PARAM_FORCED_SESSION_INVALIDATION_PERIOD_MINUTES = "ForcedSessionInvalidationPeriod";
	public static final String PARAM_RULE_LOADER = "RuleLoader";
	public static final String LEGACY_PARAM_RULE_FILE_LOADER = "RuleFileLoader";
	public static final String PARAM_ATTACK_LOGGER = "AttackLogger";
	public static final String PARAM_CLIENT_IP_DETERMINATOR = "ClientIpDeterminator";
	public static final String PARAM_BLOCK_ATTACKING_CLIENTS_DURATION = "BlockAttackingClientsDuration";
	public static final String PARAM_RESET_PERIOD_ATTACK = "ResetPeriodAttack";
	public static final String PARAM_RESET_PERIOD_SESSION_CREATION = "ResetPeriodSessionCreation";
	public static final String PARAM_RESET_PERIOD_BAD_RESPONSE_CODE = "ResetPeriodBadResponseCode";
	public static final String PARAM_RESET_PERIOD_REDIRECT_THRESHOLD = "ResetPeriodRedirectThreshold";
	public static final String PARAM_HOUSEKEEPING_INTERVAL = "HousekeepingInterval";
	public static final String PARAM_BLOCK_INVALID_ENCODED_QUERY_STRING = "BlockInvalidEncodedQueryString";
	public static final String PARAM_APPLICATION_NAME = "ApplicationName";
	public static final String PARAM_LEARNING_MODE_AGGREGATION_DIRECTORY = "LearningModeAggregationDirectory";
	public static final String PARAM_LOG_SESSION_VALUES_ON_ATTACK = "LogSessionValuesOnAttack";
	public static final String PARAM_RULE_RELOADING_INTERVAL = "RuleReloadingInterval";
	public static final String LEGACY_PARAM_RULE_FILE_RELOADING_INTERVAL = "RuleFileReloadingInterval";
	public static final String PARAM_CONFIG_RELOADING_INTERVAL = "ConfigurationReinitializationInterval";
	public static final String PARAM_ANTI_CACHE_RESPONSE_HEADER_INJECTION_CONTENT_TYPES = "AntiCacheResponseHeaderInjectionContentTypes";
	public static final String PARAM_FORCE_ENTRANCE_THROUGH_ENTRY_POINTS = "ForceEntranceThroughEntryPoints";
	public static final String PARAM_REDIRECT_WELCOME_PAGE = "RedirectWelcomePage";
	public static final String PARAM_CHARACTER_ENCODING = "CharacterEncoding";
	public static final String LEGACY_PARAM_CHARACTER_ENCODING = "RequestCharacterEncoding";
	public static final String PARAM_HANDLE_UNCAUGHT_EXCEPTIONS = "HandleUncaughtExceptions";
	public static final String PARAM_LOG_VERBOSE_FOR_DEVELOPMENT_MODE = "LogVerboseForDevelopmentMode";
	public static final String PARAM_BLOCK_REPEATED_REDIRECTS_THRESHOLD = "BlockRepeatedRedirectsThreshold";
	public static final String PARAM_REMOVE_SENSITIVE_DATA_REQUEST_PARAM_NAME_PATTERN = "RemoveSensitiveDataRequestParamNamePattern";
	public static final String PARAM_REMOVE_SENSITIVE_DATA_VALUE_PATTERN = "RemoveSensitiveDataValuePattern";
	public static final String PARAM_TREAT_NON_MATCHING_SERVLET_PATH_AS_MATCH_FOR_WHITELIST_RULES = "TreatNonMatchingServletPathAsMatchForWhitelistRules";
	public static final String PARAM_REMEMBER_LAST_CAPTCHA_FOR_MULTI_SUBMITS = "RememberLastCaptchaForMultiSubmits";
	public static final String PARAM_LOG_CLIENT_USER_DATA = "LogClientUserData";
	public static final String PARAM_APPEND_QUESTIONMARK_OR_AMPERSAND_TO_LINKS = "AppendQuestionmarkOrAmpersandToLinks";
	public static final String PARAM_APPEND_SESSIONID_TO_LINKS = "AppendSessionIdToLinks";
	public static final String PARAM_FAILED_CAPTCHA_PER_SESSION_ATTACK_THRESHOLD = "FailedCaptchaPerSessionAttackThreshold";
	public static final String PARAM_REUSE_SESSION_CONTENT = "ReuseSessionContent";
	public static final String PARAM_PARSE_MULTI_PART_FORMS = "InspectMultipartFormSubmits"; // for forms that have file uploads.... also useful to prvent attackers from changing form enctype to multipart/form-data and then circumventing servlet filters
	public static final String PARAM_PRESENT_MULTIPART_FORM_PARAMS_AS_REGULAR_PARAMS_TO_APPLICATION = "PresentMultipartFormParametersAsRegularParametersToApplication";
	public static final String PARAM_HIDE_INTERNAL_SESSION_ATTRIBUTES = "HideInternalSessionAttributes";
	public static final String PARAM_HONEYLINK_PREFIX = "HoneylinkPrefix";
	public static final String PARAM_HONEYLINK_SUFFIX = "HoneylinkSuffix";
	public static final String PARAM_HONEYLINK_MAX_PER_RESPONSE = "HoneylinkMaxPerResponse";
	public static final String PARAM_RANDOMIZE_HONEYLINKS_ON_EVERY_RESPONSE = "HoneylinkRandomizeOnEveryResponse";
	public static final String PARAM_PDF_XSS_PROTECTION = "PdfXssProtection";
	public static final String PARAM_BLOCK_MULTIPART_REQUESTS_FOR_NON_MULTIPART_FORMS = "BlockMultipartRequestsForNonMultipartForms";
	public static final String PARAM_ALLOWED_REQUEST_MIME_TYPES = "AllowedRequestMimeTypes";

	public static final String PARAM_BUFFER_FILE_UPLOADS_TO_DISK = "BufferFileUploadsToDisk";
	public static final String PARAM_APPLY_SET_AFTER_SESSION_WRITE = "ApplySetAfterSessionWrite";

	public static final String PARAM_VALIDATE_CLIENT_ADDRESS_FORMAT = "ValidateClientAddressFormat";

	public static final String PARAM_TRANSPARENT_QUERYSTRING = "TransparentQueryString";
	public static final String LEGACY_PARAM_TRANSPARENT_QUERYSTRING = "TransparentQuerystring";


	public static final String PARAM_USE_TUNED_BLOCK_PARSER = "UseTunedBlockParser";
	public static final String PARAM_USE_RESPONSE_BUFFERING = "UseResponseBuffering";


	public static final String PARAM_SESSION_TIMEOUT_REDIRECT_PAGE = "SessionTimeoutRedirectPage";


	public static final String INTERNAL_CONTENT_PREFIX = "KS_";

	public static final String SESSION_CLIENT_ADDRESS_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_TIE_WEB_SESSION_TO_CLIENT_ADDRESS*/1; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_CLIENT_HEADERS_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_TIE_WEB_SESSION_TO_HEADER_LIST*/2; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_SECRET_RANDOM_TOKEN_KEY_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_SECRET_TOKEN_LINK_INJECTION*/3+"-K"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_SECRET_TOKEN_LINK_INJECTION*/4+"-V"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_PARAMETER_AND_FORM_PROTECTION*/5; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_ENCRYPT_QUERY_STRINGS*/6+"-CD"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_ENCRYPT_QUERY_STRINGS*/7+"-CK"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_ENTRY_POINT_TOUCHED_KEY = INTERNAL_CONTENT_PREFIX+/*PARAM_FORCE_ENTRANCE_THROUGH_ENTRY_POINTS*/8+"-TD"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_REUSABLE_KEY_LIST_KEY = INTERNAL_CONTENT_PREFIX+"SRKLK"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_SELECTBOX_MASKING_PREFIX = INTERNAL_CONTENT_PREFIX+"SSMP-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_CHECKBOX_MASKING_PREFIX = INTERNAL_CONTENT_PREFIX+"SCMP-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_RADIOBUTTON_MASKING_PREFIX = INTERNAL_CONTENT_PREFIX+"SRMP-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String SESSION_SESSION_WRAPPER_REFERENCE = INTERNAL_CONTENT_PREFIX+"SSWR-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String REQUEST_NESTED_FORWARD_CALL = INTERNAL_CONTENT_PREFIX+"NF"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();

	public static final String REQUEST_ALREADY_DECRYPTED_FLAG = INTERNAL_CONTENT_PREFIX+"ALD"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String REQUEST_IS_FORM_SUBMIT_FLAG = INTERNAL_CONTENT_PREFIX+"FSF"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
	public static final String REQUEST_IS_URL_MANIPULATED_FLAG = INTERNAL_CONTENT_PREFIX+"IUM"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();



	public static final char INTERNAL_URL_DELIMITER = '$'; // just something that is not part of regular URLs (the $ is reserved character and must be encoded in URIs itself and therefore safe to use)
	public static final char INTERNAL_METHOD_TYPE_POST = '6'; // just something meaningless but unique
	public static final char INTERNAL_METHOD_TYPE_GET = '3'; // just something meaningless but unique
	public static final char INTERNAL_METHOD_TYPE_UNDEFINED = '4'; // just something meaningless but unique

	// must be String for equals check
	public static final String INTERNAL_TYPE_URL = "0"; // just something meaningless but unique
	public static final String INTERNAL_TYPE_FORM = "1"; // just something meaningless but unique

	// here as chars for something other
	public static final char INTERNAL_TYPE_LINK_FLAG = '9'; // just something meaningless but unique
	public static final char INTERNAL_TYPE_FORM_FLAG = '7'; // just something meaningless but unique

	// here as chars for something other
	public static final char INTERNAL_MULTIPART_YES_FLAG = '2'; // just something meaningless but unique
	public static final char INTERNAL_MULTIPART_NO_FLAG = '5'; // just something meaningless but unique

	// here as chars for something other
	public static final char INTERNAL_RESOURCE_ENDS_WITH_SLASH_YES_FLAG = 'S'; // just something meaningless but unique
	public static final char INTERNAL_RESOURCE_ENDS_WITH_SLASH_NO_FLAG = 'J'; // just something meaningless but unique


	public static final int STATIC_REQUEST_CRYPTODETECTION_INSERTION_POSITION = CryptoUtils.generateRandomNumber(false, 0,150);

	public static final Pattern PATTERN_VALID_CLIENT_ADDRESS = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}|((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\\b((25[0-5])|(1\\d{2})|(2[0-4]\\d)|(\\d{1,2}))\\b)\\.){3}(\\b((25[0-5])|(1\\d{2})|(2[0-4]\\d)|(\\d{1,2}))\\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\\b((25[0-5])|(1\\d{2})|(2[0-4]\\d)|(\\d{1,2}))\\b)\\.){3}(\\b((25[0-5])|(1\\d{2})|(2[0-4]\\d)|(\\d{1,2}))\\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\\b((25[0-5])|(1\\d{2})|(2[0-4]\\d)|(\\d{1,2}))\\b)\\.){3}(\\b((25[0-5])|(1\\d{2})|(2[0-4]\\d)|(\\d{1,2}))\\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))(%[0-9A-Fa-f]{1,4})?");
	public static final boolean USE_WEB_SERVER_LOG = true;

	/**
	 * Should only be enabled for internal debug purposes
	 */
	public static final boolean INTERNALLY_DUMP_REQUEST_PARAM_NAMES_VERBOSE = false;
	public static final boolean DEBUG_PRINT_UNCOVERING_DETAILS = false;



}
