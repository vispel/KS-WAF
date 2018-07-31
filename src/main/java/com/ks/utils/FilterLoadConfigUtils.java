package com.ks.utils;

import com.ks.config.ConfigurationManager;
import com.ks.container.*;
import com.ks.loaders.RuleFileLoader;
import org.apache.commons.lang3.StringUtils;

public final class FilterLoadConfigUtils {

    public static final boolean loadConfigFlag(ConfigurationManager configurationManager, String param){
        String value = configurationManager.getConfigurationValue(param);
        return StringUtils.isEmpty(value)? false : Boolean.getBoolean(value);
    }

    public static final String loadConfig(ConfigurationManager configurationManager, String param, String defaultValue){
        String value = configurationManager.getConfigurationValue(param);
        return StringUtils.isEmpty(value)? defaultValue : value.trim();
    }

    public static RequestDefinitionContainer createNewRuleContainer(String type, RuleFileLoader ruleFileLoader){
        RequestDefinitionContainer requestDefinitionContainer = null;
        switch (type) {
            case "entry-points":
                requestDefinitionContainer = new EntryPointDefinitionContainer(ruleFileLoader);
                break;
            case "optimization-hints":
                requestDefinitionContainer = new OptimizationHintDefinitionContainer(ruleFileLoader);
                break;
            case "renew-session-and-token-points":
                requestDefinitionContainer = new RenewSessionAndTokenPointDefinitionContainer(ruleFileLoader);
                break;
            case "incoming-protection-excludes":
                requestDefinitionContainer = new IncomingProtectionExcludeDefinitionContainer(ruleFileLoader);
                break;
            case "response-modification":
                requestDefinitionContainer = new ResponseModificationDefinitionContainer(ruleFileLoader);
                break;
            case "whitelist-requests":
                requestDefinitionContainer = new WhitelistRequestDefinitionContainer(ruleFileLoader);
                break;
            case "bad-requests":
                requestDefinitionContainer = new BadRequestDefinitionContainer(ruleFileLoader);
                break;
        }
        return requestDefinitionContainer;
    }

    public static SimpleDefinitionContainer createNewContainer(String type, RuleFileLoader ruleFileLoader){
        SimpleDefinitionContainer simpleDefinitionContainer = null;
        switch (type) {
            case "total-excludes":
                simpleDefinitionContainer = new TotalExcludeDefinitionContainer(ruleFileLoader);
                break;
            case "content-modification-excludes":
                simpleDefinitionContainer = new ContentModificationExcludeDefinitionContainer(ruleFileLoader);
                break;
            case "size-limits":
                simpleDefinitionContainer = new SizeLimitDefinitionContainer(ruleFileLoader);
                break;
            case "multipart-size-limits":
                simpleDefinitionContainer = new MultipartSizeLimitDefinitionContainer(ruleFileLoader);
                break;
            case "decoding-permutations":
                simpleDefinitionContainer = new DecodingPermutationDefinitionContainer(ruleFileLoader);
                break;
            case "form-field-masking-excludes":
                simpleDefinitionContainer = new FormFieldMaskingExcludeDefinitionContainer(ruleFileLoader);
                break;
        }
        return simpleDefinitionContainer;
    }


}
