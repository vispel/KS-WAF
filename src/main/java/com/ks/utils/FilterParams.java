package com.ks.utils;

import com.ks.filter.FilterInitData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ks.KsWafFilter.*;


public class FilterParams {

    public static final List<FilterInitData> FILTER_PARAMS;

     static {
         List<FilterInitData> aMap = new ArrayList<>();
         aMap.add(new FilterInitData("ShowTimings", "false", showTimings));
         aMap.add(new FilterInitData("RedirectWelcomePage", "", redirectWelcomePage));
         aMap.add(new FilterInitData("SessionTimeoutRedirectPage", "", sessionTimeoutRedirectPage));
         aMap.add(new FilterInitData("CharacterEncoding", "UTF-8", requestCharacterEncoding));
         aMap.add(new FilterInitData("LogSessionValuesOnAttack", "false", logSessionValuesOnAttack));
         aMap.add(new FilterInitData("LearningModeAggregationDirectory", "", learningModeAggregationDirectory));
         aMap.add(new FilterInitData("ApplicationName", "DEFAULT", applicationName));
         aMap.add(new FilterInitData("RuleLoader", "com.ks.loaders.ClasspathZipRuleFileLoader", ruleFileLoaderClass));
         aMap.add(new FilterInitData("ProductionModeChecker", "com.ks.DefaultProductionModeChecker", productionModeCheckerClass));

         FILTER_PARAMS = Collections.unmodifiableList(aMap);
    }
}
