package com.ks.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Consts {

	public static final String ERROR = "error";
	public static final String UTF_8 = "utf-8";
	public static final String ERROR_MSG = "errorMsg";
	public static final String INFO_MSG = "infoMsg";
	public static final String INFO_SAVE_MSG = "Poprawnie zapisano dane";

	public static final Map <String, String>  RULE_TYPES;

		static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("Bad request", "bad-request");
        aMap.put("Content Modification excludes", "content-modification-excludes");
        aMap.put("Decoding permutations", "decoding-permutations");
        aMap.put("Denial of service limits", "denial-of-service-limits");
        aMap.put("Entry points", "entry-points");
        aMap.put("Form field masking excludes", "form-field-masking-excludes");
        aMap.put("Incoming protection excludes", "incoming-protection-excludes");
        aMap.put("Renew session and-token points", "renew-session-and-token-points");
        aMap.put("Response modifications", "response-modifications");
        aMap.put("Size limits", "size-limits");
        aMap.put("Total excludes", "total-excludes");
        aMap.put("Whitelist requests", "whitelist-requests");
        RULE_TYPES = Collections.unmodifiableMap(aMap);
    }



}
