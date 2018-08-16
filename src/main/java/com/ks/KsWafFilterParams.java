package com.ks;

import com.ks.utils.CryptoUtils;

interface KsWafFilterParams {

    String PARAM_DEBUG = "Debug";
    String PARAM_SHOW_TIMINGS = "ShowTimings";
    String PARAM_BLOCK_ATTACKING_CLIENTS_THRESHOLD = "BlockAttackingClientsThreshold";

    String PARAM_DEV_ATTACK_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "DevelopmentAttackReplyStatusCodeOrMessageResource";
    String PARAM_PROD_ATTACK_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "ProductionAttackReplyStatusCodeOrMessageResource";
    String PARAM_DEV_EXCEPTION_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "DevelopmentExceptionReplyStatusCodeOrMessageResource";
    String PARAM_PROD_EXCEPTION_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "ProductionExceptionReplyStatusCodeOrMessageResource";
    String PARAM_DEV_CONFIG_MISSING_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "DevelopmentConfigurationMissingReplyStatusCodeOrMessageResource";
    String PARAM_PROD_CONFIG_MISSING_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE = "ProductionConfigurationMissingReplyStatusCodeOrMessageResource";

    String PARAM_FLUSH_RESPONSE = "FlushResponse";
    String PARAM_INVALIDATE_SESSION_ON_ATTACK = "InvalidateSessionOnAttack";
    String PARAM_TIE_WEB_SESSION_TO_CLIENT_ADDRESS = "TieWebSessionToClientAddress";
    String PARAM_TIE_WEB_SESSION_TO_HEADER_LIST = "TieWebSessionToHeaderList";
    String PARAM_BLOCK_RESPONSE_HEADERS_WITH_CRLF = "BlockResponseHeadersWithCRLF";
    String PARAM_BLOCK_FUTURE_LAST_MODIFIED_HEADERS = "BlockFutureLastModifiedResponseHeaders";
    String PARAM_BLOCK_INVALID_LAST_MODIFIED_HEADERS = "BlockInvalidLastModifiedResponseHeaders";
    String PARAM_BLOCK_REQUESTS_WITH_UNKNOWN_REFERRER = "BlockRequestsWithUnknownReferrer";
    String PARAM_BLOCK_REQUESTS_WITH_MISSING_REFERRER = "BlockRequestsWithMissingReferrer";
    String PARAM_BLOCK_REQUESTS_WITH_DUPLICATE_HEADERS = "BlockRequestsWithDuplicateHeaders";
    String PARAM_BLOCK_NON_LOCAL_REDIRECTS = "BlockNonLocalRedirects";
    String PARAM_400_OR_404_ATTACK_THRESHOLD = "HttpInvalidRequestOrNotFoundStatusCodeAttackThreshold";
    String PARAM_400_OR_404_ATTACK_THRESHOLD__CLUSTER_AWARE = "HttpInvalidRequestOrNotFoundStatusCodeClusterAware";
    String PARAM_SESSION_CREATION_ATTACK_THRESHOLD = "SessionCreationAttackThreshold";
    String PARAM_SESSION_CREATION_ATTACK_THRESHOLD__CLUSTER_AWARE = "SessionCreationClusterAware";

    String PARAM_SECRET_TOKEN_LINK_INJECTION = "SecretTokenLinkInjection";
    String PARAM_ENCRYPT_QUERY_STRINGS = "QueryStringEncryption";
    String PARAM_PARAMETER_AND_FORM_PROTECTION = "ParameterAndFormProtection";
    String PARAM_EXTRA_DISABLED_FORM_FIELD_PROTECTION = "ExtraDisabledFormFieldProtection";
    String PARAM_EXTRA_READONLY_FORM_FIELD_PROTECTION = "ExtraReadonlyFormFieldProtection";
    String PARAM_EXTRA_REQUEST_PARAM_VALUE_COUNT_PROTECTION = "ExtraRequestParamValueCountProtection";
    String PARAM_EXTRA_HIDDEN_FORM_FIELD_PROTECTION = "ExtraHiddenFormFieldProtection";
    String PARAM_EXTRA_SELECTBOX_PROTECTION = "ExtraSelectboxProtection";
    String PARAM_EXTRA_RADIOBUTTON_PROTECTION = "ExtraRadiobuttonProtection";
    String PARAM_EXTRA_CHECKBOX_PROTECTION = "ExtraCheckboxProtection";
    String PARAM_EXTRA_SELECTBOX_VALUE_MASKING = "ExtraSelectboxValueMasking";
    String PARAM_EXTRA_RADIOBUTTON_VALUE_MASKING = "ExtraRadiobuttonValueMasking";
    String PARAM_EXTRA_CHECKBOX_VALUE_MASKING = "ExtraCheckboxValueMasking";
    String PARAM_EXTRA_HASH_PROTECTION = "ExtraEncryptedValueHashProtection";
    String PARAM_EXTRA_FULL_PATH_PROTECTION = "ExtraEncryptedFullPathProtection";
    String PARAM_EXTRA_MEDIUM_PATH_REMOVAL = "ExtraEncryptedMediumPathRemoval";
    String PARAM_EXTRA_FULL_PATH_REMOVAL = "ExtraEncryptedFullPathRemoval";
    String PARAM_EXTRA_STRICT_PARAMETER_CHECKING_FOR_ENCRYPTED_LINKS = "ExtraStrictParameterCheckingForLinks";
    String PARAM_EXTRA_IMAGE_MAP_PARAMETER_EXCLUDE = "ExtraImageMapParameterExclude";
    String PARAM_EXTRA_SESSION_TIMEOUT_HANDLING = "ExtraSessionTimeoutHandling";
    String PARAM_SESSION_TIMEOUT_REDIRECT_PAGE = "SessionTimeoutRedirectPage";


    String PARAM_PATH_TO_BAD_REQUEST_FILES = "PathToBadRequestFiles";
    String PARAM_PATH_TO_WHITELIST_REQUESTS_FILES = "PathToWhitelistRequestFiles";
    String PARAM_PATH_TO_ENTRY_POINT_FILES = "PathToEntryPointFiles";
    String PARAM_PATH_TO_OPTIMIZATION_HINT_FILES = "PathToOptimizationHintFiles";
    String PARAM_PATH_TO_DOS_LIMIT_FILES = "PathToDenialOfServiceLimitFiles";
    String PARAM_PATH_TO_RENEW_SESSION_AND_TOKEN_POINT_FILES = "PathToRenewSessionAndTokenPointFiles";
    String PARAM_PATH_TO_CAPTCHA_POINT_FILES = "PathToCaptchaPointFiles";
    String PARAM_PATH_TO_INCOMING_PROTECTION_EXCLUDE_FILES = "PathToIncomingProtectionExcludeFiles";
    String PARAM_PATH_TO_RESPONSE_MODIFICATION_FILES = "PathToResponseModificationFiles"; // TODO: rename (legacy-safe) to link-patterns
    String PARAM_PATH_TO_CONTENT_MODIFICATION_EXCLUDE_FILES = "PathToContentModificationExcludeFiles";
    String PARAM_PATH_TO_TOTAL_EXCLUDE_FILES = "PathToTotalExcludeFiles";
    String PARAM_PATH_TO_SIZE_LIMIT_FILES = "PathToSizeLimitFiles";
    String PARAM_PATH_TO_MULTIPART_SIZE_LIMIT_FILES = "PathToMultipartSizeLimitFiles";
    String PARAM_PATH_TO_DECODING_PERMUTATION_FILES = "PathToDecodingPermutationFiles";
    String PARAM_PATH_TO_FORM_FIELD_MASKING_EXCLUDE_FILES = "PathToFormFieldMaskingExcludeFiles";

    String RESPONSE_MODIFICATIONS_DEFAULT = "response-modifications";
    String MODIFICATION_EXCLUDES_DEFAULT = "content-modification-excludes";

    String PARAM_MASK_AMPERSANDS_IN_LINK_ADDITIONS = "MaskAmpersandsInLinkAdditions";
    String PARAM_STRIP_HTML_COMMENTS = "StripHtmlComments";
    String PARAM_FORCED_SESSION_INVALIDATION_PERIOD_MINUTES = "ForcedSessionInvalidationPeriod";
    String PARAM_ATTACK_LOGGER = "AttackLogger";
    String PARAM_BLOCK_ATTACKING_CLIENTS_DURATION = "BlockAttackingClientsDuration";
    String PARAM_RESET_PERIOD_ATTACK = "ResetPeriodAttack";
    String PARAM_RESET_PERIOD_SESSION_CREATION = "ResetPeriodSessionCreation";
    String PARAM_RESET_PERIOD_BAD_RESPONSE_CODE = "ResetPeriodBadResponseCode";
    String PARAM_RESET_PERIOD_REDIRECT_THRESHOLD = "ResetPeriodRedirectThreshold";
    String PARAM_HOUSEKEEPING_INTERVAL = "HousekeepingInterval";
    String PARAM_BLOCK_INVALID_ENCODED_QUERY_STRING = "BlockInvalidEncodedQueryString";
    String PARAM_APPLICATION_NAME = "ApplicationName";
    String PARAM_LEARNING_MODE_AGGREGATION_DIRECTORY = "LearningModeAggregationDirectory";
    String PARAM_LOG_SESSION_VALUES_ON_ATTACK = "LogSessionValuesOnAttack";
    String PARAM_RULE_RELOADING_INTERVAL = "RuleReloadingInterval";
    String LEGACY_PARAM_RULE_FILE_RELOADING_INTERVAL = "RuleFileReloadingInterval";
    String PARAM_CONFIG_RELOADING_INTERVAL = "ConfigurationReinitializationInterval";
    String PARAM_ANTI_CACHE_RESPONSE_HEADER_INJECTION_CONTENT_TYPES = "AntiCacheResponseHeaderInjectionContentTypes";
    String PARAM_RESPONSE_MODIFICATION_CONTENT_TYPES = "ResponseBodyModificationContentTypes";
    String PARAM_FORCE_ENTRANCE_THROUGH_ENTRY_POINTS = "ForceEntranceThroughEntryPoints";
    String PARAM_REDIRECT_WELCOME_PAGE = "RedirectWelcomePage";
    String PARAM_CHARACTER_ENCODING = "CharacterEncoding";
    String PARAM_HANDLE_UNCAUGHT_EXCEPTIONS = "HandleUncaughtExceptions";
    String PARAM_LOG_VERBOSE_FOR_DEVELOPMENT_MODE = "LogVerboseForDevelopmentMode";
    String PARAM_BLOCK_REPEATED_REDIRECTS_THRESHOLD = "BlockRepeatedRedirectsThreshold";
    String PARAM_REMOVE_SENSITIVE_DATA_REQUEST_PARAM_NAME_PATTERN = "RemoveSensitiveDataRequestParamNamePattern";
    String PARAM_REMOVE_SENSITIVE_DATA_VALUE_PATTERN = "RemoveSensitiveDataValuePattern";
    String PARAM_TREAT_NON_MATCHING_SERVLET_PATH_AS_MATCH_FOR_WHITELIST_RULES = "TreatNonMatchingServletPathAsMatchForWhitelistRules";
    String PARAM_REMEMBER_LAST_CAPTCHA_FOR_MULTI_SUBMITS = "RememberLastCaptchaForMultiSubmits";
    String PARAM_LOG_CLIENT_USER_DATA = "LogClientUserData";
    String PARAM_APPEND_QUESTIONMARK_OR_AMPERSAND_TO_LINKS = "AppendQuestionmarkOrAmpersandToLinks";
    String PARAM_APPEND_SESSIONID_TO_LINKS = "AppendSessionIdToLinks";
    String PARAM_CLUSTER_INITIAL_CONTEXT_FACTORY = "ClusterInitialContextFactory";
    String PARAM_CLUSTER_BROADCAST_PERIOD = "ClusterBroadcastPeriod";
    String PARAM_CLUSTER_JMS_PROVIDER_URL = "ClusterJmsProviderUrl";
    String PARAM_CLUSTER_JMS_CONNECTION_FACTORY = "ClusterJmsConnectionFactory";
    String PARAM_CLUSTER_JMS_TOPIC = "ClusterJmsTopic";
    String PARAM_REUSE_SESSION_CONTENT = "ReuseSessionContent";
    String PARAM_PARSE_MULTI_PART_FORMS = "InspectMultipartFormSubmits"; // for forms that have file uploads.... also useful to prvent attackers from changing form enctype to multipart/form-data and then circumventing servlet filters
    String PARAM_PRESENT_MULTIPART_FORM_PARAMS_AS_REGULAR_PARAMS_TO_APPLICATION = "PresentMultipartFormParametersAsRegularParametersToApplication";
    String PARAM_HIDE_INTERNAL_SESSION_ATTRIBUTES = "HideInternalSessionAttributes";
    String PARAM_HONEYLINK_PREFIX = "HoneylinkPrefix";
    String PARAM_HONEYLINK_SUFFIX = "HoneylinkSuffix";
    String PARAM_HONEYLINK_MAX_PER_RESPONSE = "HoneylinkMaxPerResponse";
    String PARAM_RANDOMIZE_HONEYLINKS_ON_EVERY_RESPONSE = "HoneylinkRandomizeOnEveryResponse";
    String PARAM_PDF_XSS_PROTECTION = "PdfXssProtection";
    String PARAM_BLOCK_MULTIPART_REQUESTS_FOR_NON_MULTIPART_FORMS = "BlockMultipartRequestsForNonMultipartForms";
    String PARAM_ALLOWED_REQUEST_MIME_TYPES = "AllowedRequestMimeTypes";

    String PARAM_BUFFER_FILE_UPLOADS_TO_DISK = "BufferFileUploadsToDisk";
    String PARAM_APPLY_SET_AFTER_SESSION_WRITE = "ApplySetAfterSessionWrite";

    String PARAM_VALIDATE_CLIENT_ADDRESS_FORMAT = "ValidateClientAddressFormat";

    String PARAM_TRANSPARENT_QUERYSTRING = "TransparentQueryString";
    String LEGACY_PARAM_TRANSPARENT_QUERYSTRING = "TransparentQuerystring";
    String PARAM_TRANSPARENT_FORWARDING = "TransparentForwarding";


    // tuning configs
    String PARAM_USE_TUNED_BLOCK_PARSER = "UseTunedBlockParser";
    String PARAM_USE_RESPONSE_BUFFERING = "UseResponseBuffering";


    String INTERNAL_CONTENT_PREFIX = "WC_";

    String SESSION_CLIENT_ADDRESS_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_TIE_WEB_SESSION_TO_CLIENT_ADDRESS*/1; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_CLIENT_HEADERS_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_TIE_WEB_SESSION_TO_HEADER_LIST*/2; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_SECRET_RANDOM_TOKEN_KEY_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_SECRET_TOKEN_LINK_INJECTION*/3 + "-K"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_SECRET_RANDOM_TOKEN_VALUE_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_SECRET_TOKEN_LINK_INJECTION*/4 + "-V"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_PARAMETER_AND_FORM_PROTECTION_RANDOM_TOKEN_KEY_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_PARAMETER_AND_FORM_PROTECTION*/5; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_ENCRYPT_QUERY_STRINGS_CRYPTODETECTION_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_ENCRYPT_QUERY_STRINGS*/6 + "-CD"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_ENCRYPT_QUERY_STRINGS_CRYPTOKEY_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_ENCRYPT_QUERY_STRINGS*/7 + "-CK"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_ENTRY_POINT_TOUCHED_KEY = INTERNAL_CONTENT_PREFIX +/*PARAM_FORCE_ENTRANCE_THROUGH_ENTRY_POINTS*/8 + "-TD"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_REUSABLE_KEY_LIST_KEY = INTERNAL_CONTENT_PREFIX + "SRKLK"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_CAPTCHA_IMAGES = INTERNAL_CONTENT_PREFIX + "SCI-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_CAPTCHA_FAILED_COUNTER = INTERNAL_CONTENT_PREFIX + "SCFC-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_SELECTBOX_MASKING_PREFIX = INTERNAL_CONTENT_PREFIX + "SSMP-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_CHECKBOX_MASKING_PREFIX = INTERNAL_CONTENT_PREFIX + "SCMP-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_RADIOBUTTON_MASKING_PREFIX = INTERNAL_CONTENT_PREFIX + "SRMP-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String SESSION_SESSION_WRAPPER_REFERENCE = INTERNAL_CONTENT_PREFIX + "SSWR-"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String REQUEST_NESTED_FORWARD_CALL = INTERNAL_CONTENT_PREFIX + "NF"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();

    String REQUEST_ALREADY_DECRYPTED_FLAG = INTERNAL_CONTENT_PREFIX + "ALD"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String REQUEST_IS_FORM_SUBMIT_FLAG = INTERNAL_CONTENT_PREFIX + "FSF"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();
    String REQUEST_IS_URL_MANIPULATED_FLAG = INTERNAL_CONTENT_PREFIX + "IUM"; //NOT RANDOM HERE TO ALLOW SHARED SESSIONS ACROSS APPS +CryptoUtils.generateRandomToken();


    char INTERNAL_URL_DELIMITER = '$'; // just something that is not part of regular URLs (the $ is reserved character and must be encoded in URIs itself and therefore safe to use)
    char INTERNAL_METHOD_TYPE_POST = '6'; // just something meaningless but unique
    char INTERNAL_METHOD_TYPE_GET = '3'; // just something meaningless but unique
    char INTERNAL_METHOD_TYPE_UNDEFINED = '4'; // just something meaningless but unique

    // must be String for equals check
    String INTERNAL_TYPE_URL = "0"; // just something meaningless but unique
    String INTERNAL_TYPE_FORM = "1"; // just something meaningless but unique

    // here as chars for something other
    char INTERNAL_TYPE_LINK_FLAG = '9'; // just something meaningless but unique
    char INTERNAL_TYPE_FORM_FLAG = '7'; // just something meaningless but unique

    // here as chars for something other
    char INTERNAL_MULTIPART_YES_FLAG = '2'; // just something meaningless but unique
    char INTERNAL_MULTIPART_NO_FLAG = '5'; // just something meaningless but unique

    // here as chars for something other
    char INTERNAL_RESOURCE_ENDS_WITH_SLASH_YES_FLAG = 'S'; // just something meaningless but unique
    char INTERNAL_RESOURCE_ENDS_WITH_SLASH_NO_FLAG = 'J'; // just something meaningless but unique

    int STATIC_REQUEST_CRYPTODETECTION_INSERTION_POSITION = CryptoUtils.generateRandomNumber(false, 0, 150);

}
