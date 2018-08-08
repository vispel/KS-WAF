package com.ks.utils;

import java.util.*;


public class FilterParams {

    public static final Map<String, List<String>> FILTER_PARAMS;

     static {
         Map<String, List<String>> aMap = new HashMap<>();
         //[0] - Param value, [1] - Default param value
         aMap.put("PARAM_DEBUG", Arrays.asList(new String[]{"Debug", "false"}));
         aMap.put("PARAM_SHOW_TIMINGS", Arrays.asList(new String[]{"ShowTimings", "false"}));
         aMap.put("PARAM_REDIRECT_WELCOME_PAGE", Arrays.asList(new String[]{"RedirectWelcomePage", ""}));
         aMap.put("PARAM_SESSION_TIMEOUT_REDIRECT_PAGE", Arrays.asList(new String[]{"SessionTimeoutRedirectPage", ""}));
         aMap.put("PARAM_CHARACTER_ENCODING", Arrays.asList(new String[]{"CharacterEncoding", "UTF-8"}));
         aMap.put("PARAM_LOG_SESSION_VALUES_ON_ATTACK", Arrays.asList(new String[]{"LogSessionValuesOnAttack", "false"}));
         aMap.put("PARAM_LEARNING_MODE_AGGREGATION_DIRECTORY", Arrays.asList(new String[]{"LearningModeAggregationDirectory", ""}));
         aMap.put("PARAM_APPLICATION_NAME", Arrays.asList(new String[]{"ApplicationName", "DEFAULT"}));
         aMap.put("PARAM_RULE_LOADER", Arrays.asList(new String[]{"RuleLoader", "com.ks.loaders.ClasspathZipRuleFileLoader"}));
         aMap.put("PARAM_PRODUCTION_MODE_CHECKER", Arrays.asList(new String[]{"ProductionModeChecker", "com.ks.DefaultProductionModeChecker"}));

         FILTER_PARAMS = Collections.unmodifiableMap(aMap);
    }
}
