 package com.ks.response;

 import com.ks.KsWafFilter;
import com.ks.crypto.CryptoKeyAndSalt;
import com.ks.crypto.ParameterAndFormProtection;
import com.ks.parser.AbstractRelaxingHtmlParserStream;
import com.ks.pojo.FormFieldMaskingExcludeDefinition;
import com.ks.pojo.WordDictionary;
import com.ks.utils.*;
import com.ks.wrapper.RequestWrapper;
import com.ks.wrapper.ResponseWrapper;

import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;


 public final class ResponseFilterStream
   extends AbstractRelaxingHtmlParserStream
 {
   private static final boolean DEBUG = false;
   private final ByteArrayOutputStream scriptBody = new ByteArrayOutputStream();
   private final ByteArrayOutputStream collectedDisplayValue = new ByteArrayOutputStream();
   private final boolean stripHtmlEnabled;
   private final boolean injectSecretTokensEnabled;
   private final boolean protectParamsAndFormsEnabled;
   private final boolean encryptQueryStringsEnabled;
   private final boolean useFullPathForResourceToBeAccessedProtection;
   private final boolean additionalFullResourceRemoval;
   private final boolean additionalMediumResourceRemoval;
   private final boolean appendSessionIdToLinks;
   private final boolean applySetAfterWrite;
   private final String currentRequestUrlToCompareWith;
   private final String servletPath;
   private final String contextPath;
   private final String tokenKey;
   private final String tokenValue;
   private final String protectionTokenKeyKey;
   private final ContentInjectionHelper contentInjectionHelper;
   private final String cryptoDetectionString;
   private final RequestWrapper request;
   private final ResponseWrapper response;
   private final Cipher cipher;
   private final CryptoKeyAndSalt cryptoKey;
   private final boolean applyExtraProtectionForDisabledFormFields;
   private final boolean applyExtraProtectionForReadonlyFormFields;
   private final boolean applyExtraProtectionForRequestParamValueCount;
   private final boolean maskAmpersandsInModifiedLinks;
   private final boolean hiddenFormFieldProtection;
   private final boolean selectboxProtection;
   private final boolean checkboxProtection;
   private final boolean radiobuttonProtection;
   private final boolean selectboxValueMasking;
   private final boolean checkboxValueMasking;
   private final boolean radiobuttonValueMasking;
   private final boolean appendQuestionmarkOrAmpersandToLinks;
   private final boolean reuseSessionContent;
   private final Matcher[] matchersToExcludeLinksWithinScripts;
   private final Matcher[] matchersToExcludeLinksWithinTags;
   private final Matcher[] matchersToExcludeCompleteScript;
   private final Matcher[] matchersToExcludeCompleteTag;
   private final Matcher[] matchersToCaptureLinksWithinScripts;
   private final Matcher[] matchersToCaptureLinksWithinTags;
   private final WordDictionary[] prefiltersToExcludeLinksWithinScripts;
   private final WordDictionary[] prefiltersToExcludeLinksWithinTags;
   private final WordDictionary[] prefiltersToExcludeCompleteScript;
   private final WordDictionary[] prefiltersToExcludeCompleteTag;
   private final WordDictionary[] prefiltersToCaptureLinksWithinScripts;
   private final WordDictionary[] prefiltersToCaptureLinksWithinTags;
   private final int[][] groupNumbersToCaptureLinksWithinScripts;
   private final int[][] groupNumbersToCaptureLinksWithinTags;
   private boolean isWithinScript;
   private boolean isWithinStyle;
   private boolean isWithinForm;
   private boolean isWithinSelectBox;
   private boolean isWithinOption;
   private boolean isCollectingDisplayValueAsOptionValue;
   private ParameterAndFormProtection parameterAndFormProtectionOfCurrentForm = null;
   private String actionUrlOfCurrentForm;
   private String nameOfCurrentSelectBox;
   private String selectBoxMaskingPrefix;
   private String checkBoxMaskingPrefix;
   private String radioButtonMaskingPrefix;
   private boolean isCurrentFormRequestMethodPOST;
   private boolean isWithinHtmlBody;
   private boolean isWithinHtmlTable;
   private boolean isMultipartForm;
   private short honeylinkCount; private short tagPartCounter; private short tagPartCounterTarget = 17;

   private final Random honeylinkRandom;
   private final short honeylinkMaxPerPage;
   private final String honeylinkPrefix;
   private final String honeylinkSuffix;
   private final FormFieldMaskingExcludeDefinition[] matchingFormFieldMaskingExclusions;
   private final List formFieldExclusionsOfCurrentForm = new ArrayList();


   private Matcher matcherFormMethodPost;


   private Matcher matcherRequiredInputFormFieldExcludingHiddenFields;


   private Matcher matcherRequiredInputFormField;


   private Matcher matcherHiddenFormField;


   private Matcher matcherCheckbox;


   private Matcher matcherRadiobutton;


   private HttpSession session;



   public ResponseFilterStream(OutputStream delegate, String encoding, boolean useTunedBlockParser, String currentRequestUrlToCompareWith, String contextPath, String servletPath, String tokenKey, String tokenValue, String protectionTokenKeyKey, Cipher cipher, CryptoKeyAndSalt cryptoKey, ContentInjectionHelper contentInjectionHelper, String cryptoDetectionString, RequestWrapper request, ResponseWrapper response, boolean stripHtmlEnabled, boolean injectSecretTokensEnabled, boolean protectParamsAndFormsEnabled, boolean encryptQueryStringsEnabled, boolean applyExtraProtectionForDisabledFormFields, boolean applyExtraProtectionForReadonlyFormFields, boolean applyExtraProtectionForRequestParamValueCount, WordDictionary[] prefiltersToExcludeCompleteScript, Matcher[] matchersToExcludeCompleteScript, WordDictionary[] prefiltersToExcludeCompleteTag, Matcher[] matchersToExcludeCompleteTag, WordDictionary[] prefiltersToExcludeLinksWithinScripts, Matcher[] matchersToExcludeLinksWithinScripts, WordDictionary[] prefiltersToExcludeLinksWithinTags, Matcher[] matchersToExcludeLinksWithinTags, WordDictionary[] prefiltersToCaptureLinksWithinScripts, Matcher[] matchersToCaptureLinksWithinScripts, WordDictionary[] prefiltersToCaptureLinksWithinTags, Matcher[] matchersToCaptureLinksWithinTags, int[][] groupNumbersToCaptureLinksWithinScripts, int[][] groupNumbersToCaptureLinksWithinTags, boolean useFullPathForResourceToBeAccessedProtection, boolean additionalFullResourceRemoval, boolean additionalMediumResourceRemoval, boolean maskAmpersandsInModifiedLinks, boolean hiddenFormFieldProtection, boolean selectboxProtection, boolean checkboxProtection, boolean radiobuttonProtection, boolean selectboxValueMasking, boolean checkboxValueMasking, boolean radiobuttonValueMasking, boolean appendQuestionmarkOrAmpersandToLinks, boolean appendSessionIdToLinks, boolean reuseSessionContent, String honeylinkPrefix, String honeylinkSuffix, short honeylinkMaxPerPage, boolean randomizeHoneylinksOnEveryRequest, boolean applySetAfterWrite)
   {
     super(delegate, encoding, useTunedBlockParser);

     if (currentRequestUrlToCompareWith == null) throw new IllegalArgumentException("currentRequestUrlToCompareWith must not be null");
     this.currentRequestUrlToCompareWith = currentRequestUrlToCompareWith;

     if (contextPath == null) throw new IllegalArgumentException("contextPath must not be null");
     this.contextPath = contextPath;

     if (servletPath == null) throw new IllegalArgumentException("servletPath must not be null");
     this.servletPath = servletPath;

     if (response == null) throw new IllegalArgumentException("response must not be null");
     this.response = response;

     this.applySetAfterWrite = applySetAfterWrite;

     if (matchersToExcludeCompleteScript == null) throw new IllegalArgumentException("matchersToExcludeCompleteScript must not be null");
     if (matchersToExcludeCompleteTag == null) throw new IllegalArgumentException("matchersToExcludeCompleteTag must not be null");
     this.prefiltersToExcludeCompleteScript = prefiltersToExcludeCompleteScript;
     this.matchersToExcludeCompleteScript = ServerUtils.replaceEmptyMatchersWithNull(matchersToExcludeCompleteScript);
     this.prefiltersToExcludeCompleteTag = prefiltersToExcludeCompleteTag;
     this.matchersToExcludeCompleteTag = ServerUtils.replaceEmptyMatchersWithNull(matchersToExcludeCompleteTag);
     if (matchersToExcludeLinksWithinScripts == null) throw new IllegalArgumentException("matchersToExcludeLinksWithinScripts must not be null");
     if (matchersToExcludeLinksWithinTags == null) throw new IllegalArgumentException("matchersToExcludeLinksWithinTags must not be null");
     this.prefiltersToExcludeLinksWithinScripts = prefiltersToExcludeLinksWithinScripts;
     this.matchersToExcludeLinksWithinScripts = ServerUtils.replaceEmptyMatchersWithNull(matchersToExcludeLinksWithinScripts);
     this.prefiltersToExcludeLinksWithinTags = prefiltersToExcludeLinksWithinTags;
     this.matchersToExcludeLinksWithinTags = ServerUtils.replaceEmptyMatchersWithNull(matchersToExcludeLinksWithinTags);
     if (matchersToCaptureLinksWithinScripts == null) throw new IllegalArgumentException("matchersToCaptureLinksWithinScripts must not be null");
     if (matchersToCaptureLinksWithinTags == null) throw new IllegalArgumentException("matchersToCaptureLinksWithinTags must not be null");
     this.prefiltersToCaptureLinksWithinScripts = prefiltersToCaptureLinksWithinScripts;
     this.matchersToCaptureLinksWithinScripts = matchersToCaptureLinksWithinScripts;
     this.prefiltersToCaptureLinksWithinTags = prefiltersToCaptureLinksWithinTags;
     this.matchersToCaptureLinksWithinTags = matchersToCaptureLinksWithinTags;
     this.groupNumbersToCaptureLinksWithinScripts = groupNumbersToCaptureLinksWithinScripts;
     this.groupNumbersToCaptureLinksWithinTags = groupNumbersToCaptureLinksWithinTags;
     if (matchersToCaptureLinksWithinScripts.length != groupNumbersToCaptureLinksWithinScripts.length) throw new IllegalArgumentException("Lengths of capturing pattern and group-number array must be equal");
     if (matchersToCaptureLinksWithinTags.length != groupNumbersToCaptureLinksWithinTags.length) throw new IllegalArgumentException("Lengths of capturing pattern and group-number array must be equal");
     if (matchersToCaptureLinksWithinScripts.length != matchersToExcludeLinksWithinScripts.length) throw new IllegalArgumentException("Lengths of capturing pattern and exclusion pattern array must be equal");
     if (matchersToCaptureLinksWithinTags.length != matchersToExcludeLinksWithinTags.length) { throw new IllegalArgumentException("Lengths of capturing pattern and exclusion pattern array must be equal");
     }

     this.tokenKey = tokenKey;
     this.tokenValue = tokenValue;
     this.protectionTokenKeyKey = protectionTokenKeyKey;
     this.cipher = cipher;
     this.cryptoKey = cryptoKey;
     if (contentInjectionHelper == null) throw new IllegalArgumentException("contentInjectionHelper must not be null");
     this.contentInjectionHelper = contentInjectionHelper;
     this.cryptoDetectionString = cryptoDetectionString;
     this.request = request;
     this.useFullPathForResourceToBeAccessedProtection = useFullPathForResourceToBeAccessedProtection;
     this.additionalFullResourceRemoval = additionalFullResourceRemoval;
     this.additionalMediumResourceRemoval = additionalMediumResourceRemoval;

     this.stripHtmlEnabled = stripHtmlEnabled;
     this.injectSecretTokensEnabled = injectSecretTokensEnabled;
     this.protectParamsAndFormsEnabled = protectParamsAndFormsEnabled;
     this.encryptQueryStringsEnabled = encryptQueryStringsEnabled;
     this.applyExtraProtectionForDisabledFormFields = applyExtraProtectionForDisabledFormFields;
     this.applyExtraProtectionForReadonlyFormFields = applyExtraProtectionForReadonlyFormFields;
     this.applyExtraProtectionForRequestParamValueCount = applyExtraProtectionForRequestParamValueCount;
     this.maskAmpersandsInModifiedLinks = maskAmpersandsInModifiedLinks;

     this.matchingFormFieldMaskingExclusions = (contentInjectionHelper.getFormFieldMaskingExcludeDefinitions() == null ? new FormFieldMaskingExcludeDefinition[0] : contentInjectionHelper.getFormFieldMaskingExcludeDefinitions().getAllMatchingFormFieldMaskingExcludeDefinitions(servletPath, request.getRequestURI()));

     if ((this.injectSecretTokensEnabled) && ((this.tokenKey == null) || (this.tokenValue == null))) throw new IllegalArgumentException("tokenKey and/or tokenValue must not be null when injectSecretTokensEnabled is set");
     if ((this.protectParamsAndFormsEnabled) && ((this.request == null) || (this.protectionTokenKeyKey == null))) throw new IllegalArgumentException("request and/or protectionTokenKeyKey must not be null when protectParamsAndFormsEnabled is set");
     if ((this.encryptQueryStringsEnabled) && ((this.cryptoDetectionString == null) || (this.cryptoKey == null))) { throw new IllegalArgumentException("cryptoDetectionString and/or cryptoKey must not be null when encryptQueryStringsEnabled is set");
     }
     if ((this.encryptQueryStringsEnabled) && (!this.injectSecretTokensEnabled)) throw new IllegalArgumentException("encryptQueryStringsEnabled also requires to set injectSecretTokensEnabled");
     if ((this.protectParamsAndFormsEnabled) && (!this.encryptQueryStringsEnabled)) throw new IllegalArgumentException("protectParamsAndFormsEnabled also requires to set encryptQueryStringsEnabled");
     if ((this.applyExtraProtectionForDisabledFormFields) && (!this.protectParamsAndFormsEnabled)) throw new IllegalArgumentException("applyExtraProtectionForDisabledFormFields also requires to set protectParamsAndFormsEnabled");
     if ((this.applyExtraProtectionForReadonlyFormFields) && (!this.protectParamsAndFormsEnabled)) throw new IllegalArgumentException("applyExtraProtectionForReadonlyFormFields also requires to set protectParamsAndFormsEnabled");
     if ((this.applyExtraProtectionForRequestParamValueCount) && (!this.protectParamsAndFormsEnabled)) { throw new IllegalArgumentException("applyExtraProtectionForRequestParamValueCount also requires to set protectParamsAndFormsEnabled");
     }
     this.hiddenFormFieldProtection = hiddenFormFieldProtection;
     this.selectboxProtection = selectboxProtection;
     this.checkboxProtection = checkboxProtection;
     this.radiobuttonProtection = radiobuttonProtection;
     this.selectboxValueMasking = selectboxValueMasking;
     this.checkboxValueMasking = checkboxValueMasking;
     this.radiobuttonValueMasking = radiobuttonValueMasking;

     this.appendQuestionmarkOrAmpersandToLinks = appendQuestionmarkOrAmpersandToLinks;
     this.appendSessionIdToLinks = appendSessionIdToLinks;
     this.reuseSessionContent = reuseSessionContent;

     this.honeylinkPrefix = honeylinkPrefix;
     this.honeylinkSuffix = honeylinkSuffix;
     this.honeylinkMaxPerPage = honeylinkMaxPerPage;
     if (honeylinkMaxPerPage > 0) {
       this.honeylinkRandom = (randomizeHoneylinksOnEveryRequest ? null : new Random(this.servletPath.hashCode() + KsWafFilter.customerIdentifier));
       this.tagPartCounterTarget = HoneylinkUtils.nextTagPartCounterTarget(this.honeylinkRandom);
     } else { this.honeylinkRandom = null;
     }
   }

   public void handleTag(String tag)
     throws IOException
   {
     boolean startsWithScriptOpening = false;
     boolean startsWithStyleOpening = false;
     boolean startsWithCommentOpening = false;
     boolean startsWithFormOpening = false;
     boolean startsWithInputOpening = false;
     boolean startsWithButtonOpening = false;
     boolean startsWithTextareaOpening = false;
     boolean startsWithSelectOpening = false;
     boolean startsWithOptionOpening = false;



     if (tag.length() >= 4) {
       startsWithCommentOpening = (tag.charAt(1) == '!') && (tag.charAt(2) == '-') && (tag.charAt(3) == '-');
       if (tag.length() >= 5) {
         startsWithFormOpening = ((tag.charAt(1) == 'f') || (tag.charAt(1) == 'F')) && ((tag.charAt(2) == 'o') || (tag.charAt(2) == 'O')) && ((tag.charAt(3) == 'r') || (tag.charAt(3) == 'R')) && ((tag.charAt(4) == 'm') || (tag.charAt(4) == 'M'));
         if (tag.length() >= 6) {
           startsWithStyleOpening = ((tag.charAt(1) == 's') || (tag.charAt(1) == 'S')) && ((tag.charAt(2) == 't') || (tag.charAt(2) == 'T')) && ((tag.charAt(3) == 'y') || (tag.charAt(3) == 'Y')) && ((tag.charAt(4) == 'l') || (tag.charAt(4) == 'L')) && ((tag.charAt(5) == 'e') || (tag.charAt(5) == 'E'));
           startsWithInputOpening = ((tag.charAt(1) == 'i') || (tag.charAt(1) == 'I')) && ((tag.charAt(2) == 'n') || (tag.charAt(2) == 'N')) && ((tag.charAt(3) == 'p') || (tag.charAt(3) == 'P')) && ((tag.charAt(4) == 'u') || (tag.charAt(4) == 'U')) && ((tag.charAt(5) == 't') || (tag.charAt(5) == 'T'));
           if (tag.length() >= 7) {
             if (((tag.charAt(1) == 's') || (tag.charAt(1) == 'S')) && ((tag.charAt(6) == 't') || (tag.charAt(6) == 'T'))) {
               startsWithScriptOpening = ((tag.charAt(2) == 'c') || (tag.charAt(2) == 'C')) && ((tag.charAt(3) == 'r') || (tag.charAt(3) == 'R')) && ((tag.charAt(4) == 'i') || (tag.charAt(4) == 'I')) && ((tag.charAt(5) == 'p') || (tag.charAt(5) == 'P'));
               startsWithSelectOpening = ((tag.charAt(2) == 'e') || (tag.charAt(2) == 'E')) && ((tag.charAt(3) == 'l') || (tag.charAt(3) == 'L')) && ((tag.charAt(4) == 'e') || (tag.charAt(4) == 'E')) && ((tag.charAt(5) == 'c') || (tag.charAt(5) == 'C'));
             } else if (((tag.charAt(3) == 't') || (tag.charAt(3) == 'T')) && ((tag.charAt(5) == 'o') || (tag.charAt(5) == 'O')) && ((tag.charAt(6) == 'n') || (tag.charAt(6) == 'N'))) {
               startsWithButtonOpening = ((tag.charAt(1) == 'b') || (tag.charAt(1) == 'B')) && ((tag.charAt(2) == 'u') || (tag.charAt(2) == 'U')) && ((tag.charAt(4) == 't') || (tag.charAt(4) == 'T'));
               startsWithOptionOpening = ((tag.charAt(1) == 'o') || (tag.charAt(1) == 'O')) && ((tag.charAt(2) == 'p') || (tag.charAt(2) == 'P')) && ((tag.charAt(4) == 'i') || (tag.charAt(4) == 'I'));
             }
             if (tag.length() >= 9) {
               startsWithTextareaOpening = ((tag.charAt(1) == 't') || (tag.charAt(1) == 'T')) && ((tag.charAt(2) == 'e') || (tag.charAt(2) == 'E')) && ((tag.charAt(3) == 'x') || (tag.charAt(3) == 'X')) && ((tag.charAt(4) == 't') || (tag.charAt(4) == 'T')) && ((tag.charAt(5) == 'a') || (tag.charAt(5) == 'A')) && ((tag.charAt(6) == 'r') || (tag.charAt(6) == 'R')) && ((tag.charAt(7) == 'e') || (tag.charAt(7) == 'E')) && ((tag.charAt(8) == 'a') || (tag.charAt(8) == 'A'));
             }
           }
         }
       }
     }

     if ((this.stripHtmlEnabled) && (!this.isWithinScript) && (!this.isWithinStyle) && (startsWithCommentOpening)) {
       return;
     }

     if (this.honeylinkMaxPerPage > 0)
     {


       if ((tag.length() >= 5) && ((tag.charAt(1) == 'b') || (tag.charAt(1) == 'B')) && ((tag.charAt(2) == 'o') || (tag.charAt(2) == 'O')) && ((tag.charAt(3) == 'd') || (tag.charAt(3) == 'D')) && ((tag.charAt(4) == 'y') || (tag.charAt(4) == 'Y')))
         this.isWithinHtmlBody = true;
       if ((tag.length() >= 6) && ((tag.charAt(1) == 't') || (tag.charAt(1) == 'T')) && ((tag.charAt(2) == 'a') || (tag.charAt(2) == 'A')) && ((tag.charAt(3) == 'b') || (tag.charAt(3) == 'B')) && ((tag.charAt(4) == 'l') || (tag.charAt(4) == 'L')) && ((tag.charAt(5) == 'e') || (tag.charAt(5) == 'E'))) {
         this.isWithinHtmlTable = true;
       }
     }





     if (startsWithStyleOpening) { this.isWithinStyle = true;
     }
     if (startsWithScriptOpening) {
       if (this.isWithinScript) {
         return;
       }
       this.isWithinScript = true;
       this.scriptBody.reset();
     }



     boolean isRequestMethodPOST = false;
     if (startsWithFormOpening) {
       if (this.matcherFormMethodPost == null) this.matcherFormMethodPost = PATTERN_FORM_METHOD_POST.matcher(tag); else this.matcherFormMethodPost.reset(tag);
       this.isMultipartForm = ResponseUtils.isMultipartForm(tag);

       if (this.matcherFormMethodPost.find()) { isRequestMethodPOST = true;
       }
     }


     if ((this.protectParamsAndFormsEnabled) && (this.isWithinForm) && (!this.isWithinScript) && (!this.isWithinStyle) && (this.parameterAndFormProtectionOfCurrentForm != null) && ((startsWithInputOpening) || (startsWithButtonOpening) || (startsWithSelectOpening) || (startsWithTextareaOpening)))
     {



       String extractedFieldName = ResponseUtils.extractFieldName(tag);
       if (extractedFieldName != null)
       {
         if ((!this.applyExtraProtectionForDisabledFormFields) || (!ResponseUtils.isFormFieldDisabled(tag)))
         {
           boolean potentiallyRequiredMatch;
           if (this.hiddenFormFieldProtection) {
             if (this.matcherRequiredInputFormFieldExcludingHiddenFields == null) this.matcherRequiredInputFormFieldExcludingHiddenFields = PATTERN_REQUIRED_INPUT_FORM_FIELD_EXCLUDING_HIDDEN_FIELDS.matcher(tag); else this.matcherRequiredInputFormFieldExcludingHiddenFields.reset(tag);
             potentiallyRequiredMatch = this.matcherRequiredInputFormFieldExcludingHiddenFields.find();
           } else {
             if (this.matcherRequiredInputFormField == null) this.matcherRequiredInputFormField = PATTERN_REQUIRED_INPUT_FORM_FIELD.matcher(tag); else this.matcherRequiredInputFormField.reset(tag);
             potentiallyRequiredMatch = this.matcherRequiredInputFormField.find(); }
           boolean required;
           if (this.applyExtraProtectionForDisabledFormFields) required = (startsWithTextareaOpening) || ((startsWithInputOpening) && (potentiallyRequiredMatch)); else
             required = false;
           String extractedFieldNameDecoded = ServerUtils.decodeBrokenValueExceptUrlEncoding(extractedFieldName);


           if (this.hiddenFormFieldProtection) {
             if (this.matcherHiddenFormField == null) this.matcherHiddenFormField = PATTERN_HIDDEN_FORM_FIELD.matcher(tag); else this.matcherHiddenFormField.reset(tag);
             if ((this.matcherHiddenFormField.find()) && (!isFormFieldMaskingExclusion(extractedFieldNameDecoded))) {
               String expectedValue = ResponseUtils.extractFieldValue(tag);
               if (!"CF".equals(extractedFieldNameDecoded)) {
                 if (!ResponseUtils.isFormFieldDisabled(tag)) this.parameterAndFormProtectionOfCurrentForm.addHiddenFieldRemovedValue(extractedFieldNameDecoded, ServerUtils.decodeBrokenValueExceptUrlEncoding(expectedValue));
                 return;
               }
             }
           }


           if ((this.selectboxProtection) &&
             (!this.isWithinSelectBox) && (startsWithSelectOpening) && (!isFormFieldMaskingExclusion(extractedFieldNameDecoded))) {
             this.isWithinSelectBox = true;
             this.nameOfCurrentSelectBox = extractedFieldNameDecoded;
           }



           if (startsWithInputOpening)
           {
             if (this.checkboxProtection) {
               if (this.matcherCheckbox == null) this.matcherCheckbox = PATTERN_CHECKBOX.matcher(tag); else this.matcherCheckbox.reset(tag);
               if ((this.matcherCheckbox.find()) && (!isFormFieldMaskingExclusion(extractedFieldNameDecoded))) {
                 String value = ResponseUtils.extractFieldValue(tag);
                 if (this.checkboxValueMasking)
                 {
                   if (this.checkBoxMaskingPrefix == null) this.checkBoxMaskingPrefix = RequestUtils.createOrRetrieveRandomTokenFromSession(getSession(), "WC_SCMP-", 5, 7);
                   tag = ResponseUtils.setFieldValue(tag, this.checkBoxMaskingPrefix + this.parameterAndFormProtectionOfCurrentForm.getIndexOfNextCheckboxFieldAllowedValue(extractedFieldNameDecoded));
                 }
                 value = ServerUtils.decodeBrokenValueExceptUrlEncoding(value);
                 if (value == null) value = "";
                 this.parameterAndFormProtectionOfCurrentForm.addCheckboxFieldAllowedValue(extractedFieldNameDecoded, value);
               }
             }



             if (this.radiobuttonProtection) {
               if (this.matcherRadiobutton == null) this.matcherRadiobutton = PATTERN_RADIOBUTTON.matcher(tag); else this.matcherRadiobutton.reset(tag);
               if ((this.matcherRadiobutton.find()) && (!isFormFieldMaskingExclusion(extractedFieldNameDecoded))) {
                 String value = ResponseUtils.extractFieldValue(tag);
                 if (this.radiobuttonValueMasking)
                 {
                   if (this.radioButtonMaskingPrefix == null) this.radioButtonMaskingPrefix = RequestUtils.createOrRetrieveRandomTokenFromSession(getSession(), "WC_SRMP-", 5, 7);
                   tag = ResponseUtils.setFieldValue(tag, this.radioButtonMaskingPrefix + this.parameterAndFormProtectionOfCurrentForm.getIndexOfNextRadiobuttonFieldAllowedValue(extractedFieldNameDecoded));
                 }
                 value = ServerUtils.decodeBrokenValueExceptUrlEncoding(value);
                 if (value == null) value = "";
                 this.parameterAndFormProtectionOfCurrentForm.addRadiobuttonFieldAllowedValue(extractedFieldNameDecoded, value);
               }
             }
           }



           this.parameterAndFormProtectionOfCurrentForm.addParameterName(extractedFieldNameDecoded, required);


           if (this.applyExtraProtectionForReadonlyFormFields) {
             if (ResponseUtils.isFormFieldReadonly(tag)) {
               String expectedValue = ResponseUtils.extractFormFieldValue(tag);
               this.parameterAndFormProtectionOfCurrentForm.addReadonlyFieldExpectedValue(extractedFieldNameDecoded, ServerUtils.decodeBrokenValueExceptUrlEncoding(expectedValue));
             } else { this.parameterAndFormProtectionOfCurrentForm.addReadwriteFieldName(extractedFieldNameDecoded);
             }
           }

           if (this.applyExtraProtectionForRequestParamValueCount) {
             if (startsWithTextareaOpening) {
               this.parameterAndFormProtectionOfCurrentForm.incrementMinimumValueCountForParameterName(extractedFieldNameDecoded, 1);
               this.parameterAndFormProtectionOfCurrentForm.incrementMaximumValueCountForParameterName(extractedFieldNameDecoded, 1);
             } else if (startsWithInputOpening) {
               if (potentiallyRequiredMatch) {
                 this.parameterAndFormProtectionOfCurrentForm.incrementMinimumValueCountForParameterName(extractedFieldNameDecoded, 1);
                 this.parameterAndFormProtectionOfCurrentForm.incrementMaximumValueCountForParameterName(extractedFieldNameDecoded, 1);
               }
               else
               {
                 System.err.println("not implemented");
                 throw new UnsupportedOperationException("not implemented");
               }
             } else if (startsWithSelectOpening) {
               this.parameterAndFormProtectionOfCurrentForm.incrementMinimumValueCountForParameterName(extractedFieldNameDecoded, 1);
               boolean isMultipleSelectbox = ResponseUtils.isFormFieldMultiple(tag);
               if (isMultipleSelectbox)
               {
                 System.err.println("not implemented");
                 throw new UnsupportedOperationException("not implemented");
               }
               this.parameterAndFormProtectionOfCurrentForm.incrementMaximumValueCountForParameterName(extractedFieldNameDecoded, 1);
             }
           }
         }
       }
     }





     if (this.selectboxProtection)
     {
       if (this.isWithinOption) { finishOptionDisplayValueCollecting();
       }
       if ((this.protectParamsAndFormsEnabled) && (this.isWithinForm) && (this.parameterAndFormProtectionOfCurrentForm != null) && (this.isWithinSelectBox) && (!this.isWithinOption) && (startsWithOptionOpening))
       {
         boolean isDirectlyClosedWithoutMatchingEndTag = tag.endsWith("/>");
         this.isWithinOption = (!isDirectlyClosedWithoutMatchingEndTag);
         String value = ResponseUtils.extractFieldValue(tag);
         if (this.selectboxValueMasking)
         {
           if (this.selectBoxMaskingPrefix == null) this.selectBoxMaskingPrefix = RequestUtils.createOrRetrieveRandomTokenFromSession(getSession(), "WC_SSMP-", 5, 7);
           tag = ResponseUtils.setFieldValue(tag, this.selectBoxMaskingPrefix + this.parameterAndFormProtectionOfCurrentForm.getIndexOfNextSelectboxFieldAllowedValue(this.nameOfCurrentSelectBox));
         }

         if ((value == null) && (isDirectlyClosedWithoutMatchingEndTag)) {
           value = "";
         }

         if (value == null) {
           this.isCollectingDisplayValueAsOptionValue = true;
         }
         else {
           value = ServerUtils.decodeBrokenValueExceptUrlEncoding(value);
           this.parameterAndFormProtectionOfCurrentForm.addSelectboxFieldAllowedValue(this.nameOfCurrentSelectBox, value);
         }
       }
     }


















     if ((!this.isWithinScript) && (!this.isWithinStyle)) {
       if (startsWithFormOpening) {
         if (this.isWithinForm) {
           return;
         }

         String actionUrlFetchedDirectlyFromForm = ResponseUtils.extractActionUrlOfCurrentForm(tag, isRequestMethodPOST);
         if (ServerUtils.isInternalHostURL(this.currentRequestUrlToCompareWith, ServerUtils.decodeBrokenValueHtmlOnly(actionUrlFetchedDirectlyFromForm, false)))
         {
           this.isWithinForm = true;
           String extractedFormNameDecoded = ServerUtils.decodeBrokenValueExceptUrlEncoding(ResponseUtils.extractFieldName(tag));
           prefilterMatchingFormMaskingExclusions(extractedFormNameDecoded);
           if (((this.injectSecretTokensEnabled) && (!isRequestMethodPOST)) || (this.encryptQueryStringsEnabled) || (this.protectParamsAndFormsEnabled))
           {
             this.actionUrlOfCurrentForm = actionUrlFetchedDirectlyFromForm;


             if (((this.additionalFullResourceRemoval) || (this.additionalMediumResourceRemoval)) && ((this.actionUrlOfCurrentForm == null) || (this.actionUrlOfCurrentForm.length() == 0)))
             {
               if (this.additionalMediumResourceRemoval) {
                 String relativeLink = ServerUtils.extractFileFromURL(this.currentRequestUrlToCompareWith);
                 this.actionUrlOfCurrentForm = (relativeLink != null ? relativeLink : this.currentRequestUrlToCompareWith);
               } else if (this.additionalFullResourceRemoval) {
                 this.actionUrlOfCurrentForm = this.currentRequestUrlToCompareWith;
               }

               if (this.actionUrlOfCurrentForm != null) this.actionUrlOfCurrentForm = this.response.encodeURL(this.actionUrlOfCurrentForm);
               tag = ResponseUtils.setFieldAction(tag, this.actionUrlOfCurrentForm);
             }

             tag = ResponseUtils.removeQueryStringFromActionUrlOfCurrentForm(tag, this.additionalFullResourceRemoval, this.additionalMediumResourceRemoval, this.contextPath, this.response, this.appendQuestionmarkOrAmpersandToLinks, this.appendSessionIdToLinks);
             this.parameterAndFormProtectionOfCurrentForm = new ParameterAndFormProtection(this.hiddenFormFieldProtection);
             this.isCurrentFormRequestMethodPOST = isRequestMethodPOST;
             tag = applyLinkModifications(tag, this.prefiltersToCaptureLinksWithinScripts, this.matchersToCaptureLinksWithinScripts, this.prefiltersToExcludeCompleteScript, this.matchersToExcludeCompleteScript, this.prefiltersToExcludeLinksWithinScripts, this.matchersToExcludeLinksWithinScripts, this.groupNumbersToCaptureLinksWithinScripts);
           }
           else {
             tag = applyLinkModifications(tag, this.prefiltersToCaptureLinksWithinScripts, this.matchersToCaptureLinksWithinScripts, this.prefiltersToExcludeCompleteScript, this.matchersToExcludeCompleteScript, this.prefiltersToExcludeLinksWithinScripts, this.matchersToExcludeLinksWithinScripts, this.groupNumbersToCaptureLinksWithinScripts);
             tag = applyLinkModifications(tag, this.prefiltersToCaptureLinksWithinTags, this.matchersToCaptureLinksWithinTags, this.prefiltersToExcludeCompleteTag, this.matchersToExcludeCompleteTag, this.prefiltersToExcludeLinksWithinTags, this.matchersToExcludeLinksWithinTags, this.groupNumbersToCaptureLinksWithinTags);
           }
         }
       }
       else
       {
         tag = applyLinkModifications(tag, this.prefiltersToCaptureLinksWithinScripts, this.matchersToCaptureLinksWithinScripts, this.prefiltersToExcludeCompleteScript, this.matchersToExcludeCompleteScript, this.prefiltersToExcludeLinksWithinScripts, this.matchersToExcludeLinksWithinScripts, this.groupNumbersToCaptureLinksWithinScripts);
         tag = applyLinkModifications(tag, this.prefiltersToCaptureLinksWithinTags, this.matchersToCaptureLinksWithinTags, this.prefiltersToExcludeCompleteTag, this.matchersToExcludeCompleteTag, this.prefiltersToExcludeLinksWithinTags, this.matchersToExcludeLinksWithinTags, this.groupNumbersToCaptureLinksWithinTags);
       }



       if ((this.honeylinkMaxPerPage > 0) && (this.isWithinHtmlBody) && (this.honeylinkCount < this.honeylinkMaxPerPage))
       {
         if ((this.tagPartCounter = (short)(this.tagPartCounter + 1)) % this.tagPartCounterTarget == 0) {
           tag = tag + HoneylinkUtils.generateHoneylink(this.honeylinkRandom, this.honeylinkPrefix, this.honeylinkSuffix, this.isWithinHtmlTable);
           this.honeylinkCount = ((short)(this.honeylinkCount + 1));
           this.tagPartCounter = 0;
           this.tagPartCounterTarget = HoneylinkUtils.nextTagPartCounterTarget(this.honeylinkRandom);
         }
       }
     }


     if ((this.isWithinScript) && (!startsWithScriptOpening)) {
       this.scriptBody.write(tag.getBytes(this.encoding));
     } else {
       writeToUnderlyingSink(tag);
     }
   }




   public void handleTagClose(String tag)
     throws IOException
   {
     boolean startsWithScript;
     boolean startsWithForm = false;
     boolean startsWithSelectAndProtectionIsActive = false;


     if (this.honeylinkMaxPerPage > 0)
     {

       if ((this.isWithinHtmlBody) && (tag.length() >= 6) && ((tag.charAt(2) == 'b') || (tag.charAt(2) == 'B')) && ((tag.charAt(3) == 'o') || (tag.charAt(3) == 'O')) && ((tag.charAt(4) == 'd') || (tag.charAt(4) == 'D')) && ((tag.charAt(5) == 'y') || (tag.charAt(5) == 'Y')))
         this.isWithinHtmlBody = false;
       if ((this.isWithinHtmlTable) && (tag.length() >= 7) && ((tag.charAt(2) == 't') || (tag.charAt(2) == 'T')) && ((tag.charAt(3) == 'a') || (tag.charAt(3) == 'A')) && ((tag.charAt(4) == 'b') || (tag.charAt(4) == 'B')) && ((tag.charAt(5) == 'l') || (tag.charAt(5) == 'L')) && ((tag.charAt(6) == 'e') || (tag.charAt(6) == 'E')))
         this.isWithinHtmlTable = false;
     }
     if ((this.isWithinStyle) && (tag.length() >= 7) && ((tag.charAt(2) == 's') || (tag.charAt(2) == 'S')) && ((tag.charAt(3) == 't') || (tag.charAt(3) == 'T')) && ((tag.charAt(4) == 'y') || (tag.charAt(4) == 'Y')) && ((tag.charAt(5) == 'l') || (tag.charAt(5) == 'L')) && ((tag.charAt(6) == 'e') || (tag.charAt(6) == 'E')))
       this.isWithinStyle = false;
     startsWithScript = (this.isWithinScript) && (tag.length() >= 8) && ((tag.charAt(2) == 's') || (tag.charAt(2) == 'S')) && ((tag.charAt(3) == 'c') || (tag.charAt(3) == 'C')) && ((tag.charAt(4) == 'r') || (tag.charAt(4) == 'R')) && ((tag.charAt(5) == 'i') || (tag.charAt(5) == 'I')) && ((tag.charAt(6) == 'p') || (tag.charAt(6) == 'P')) && ((tag.charAt(7) == 't') || (tag.charAt(7) == 'T'));

     startsWithForm = (this.isWithinForm) && (tag.length() >= 6) && ((tag.charAt(2) == 'f') || (tag.charAt(2) == 'F')) && ((tag.charAt(3) == 'o') || (tag.charAt(3) == 'O')) && ((tag.charAt(4) == 'r') || (tag.charAt(4) == 'R')) && ((tag.charAt(5) == 'm') || (tag.charAt(5) == 'M'));

     startsWithSelectAndProtectionIsActive = (this.selectboxProtection) && (this.isWithinSelectBox) && (tag.length() >= 8) && ((tag.charAt(2) == 's') || (tag.charAt(2) == 'S')) && ((tag.charAt(3) == 'e') || (tag.charAt(3) == 'E')) && ((tag.charAt(4) == 'l') || (tag.charAt(4) == 'L')) && ((tag.charAt(5) == 'e') || (tag.charAt(5) == 'E')) && ((tag.charAt(6) == 'c') || (tag.charAt(6) == 'C')) && ((tag.charAt(7) == 't') || (tag.charAt(7) == 'T'));














     if (startsWithScript) {
       this.isWithinScript = false;
       writeScriptBodyWithLinksAdjusted();
       this.scriptBody.reset();
     } else if (startsWithForm)
     {
       if (this.actionUrlOfCurrentForm != null)
       {
         if (((!this.encryptQueryStringsEnabled) || (!ResponseUtils.isAlreadyEncrypted(this.cryptoDetectionString, this.actionUrlOfCurrentForm))) &&
           (this.injectSecretTokensEnabled)) {
           String urlDecoded = ServerUtils.decodeBrokenValueHtmlOnly(this.actionUrlOfCurrentForm, false);
           if (!ServerUtils.startsWithJavaScriptOrMailto(urlDecoded)) {
             this.actionUrlOfCurrentForm = urlDecoded;

             this.actionUrlOfCurrentForm = ResponseUtils.injectParameterIntoURL(this.actionUrlOfCurrentForm, this.tokenKey, this.tokenValue, this.maskAmpersandsInModifiedLinks, this.appendQuestionmarkOrAmpersandToLinks, true);
             if ((this.protectParamsAndFormsEnabled) && (this.parameterAndFormProtectionOfCurrentForm != null)) {
               String parameterAndFormProtectionValue = ResponseUtils.getKeyForParameterAndFormProtection(this.actionUrlOfCurrentForm, this.parameterAndFormProtectionOfCurrentForm, getSession(), this.reuseSessionContent, this.applySetAfterWrite);
               this.actionUrlOfCurrentForm = ResponseUtils.injectParameterIntoURL(this.actionUrlOfCurrentForm, this.protectionTokenKeyKey, parameterAndFormProtectionValue, this.maskAmpersandsInModifiedLinks, this.appendQuestionmarkOrAmpersandToLinks, true);
             }
             this.actionUrlOfCurrentForm = ServerUtils.encodeHtmlSafe(this.actionUrlOfCurrentForm);


             if (this.encryptQueryStringsEnabled)
             {
               this.actionUrlOfCurrentForm = ResponseUtils.encryptQueryStringInURL(this.currentRequestUrlToCompareWith, this.contextPath, this.servletPath, this.actionUrlOfCurrentForm, true, this.isMultipartForm, this.isCurrentFormRequestMethodPOST, this.contentInjectionHelper.isSupposedToBeStaticResource(ResponseUtils.extractURI(this.actionUrlOfCurrentForm)), this.cryptoDetectionString, this.cipher, this.cryptoKey, this.useFullPathForResourceToBeAccessedProtection, this.additionalFullResourceRemoval, this.additionalMediumResourceRemoval, this.response, this.appendQuestionmarkOrAmpersandToLinks);
             }

             if ((this.appendSessionIdToLinks) && (this.actionUrlOfCurrentForm != null)) { this.actionUrlOfCurrentForm = this.response.encodeURL(this.actionUrlOfCurrentForm);
             }
           }
         }

         String queryStringOfActionUrl = ResponseUtils.extractQueryStringOfActionUrl(this.actionUrlOfCurrentForm);
         if (queryStringOfActionUrl != null) {
           int equalsSignPos = queryStringOfActionUrl.indexOf('=');
           String queryStringOfActionUrl_beforeEqualsSign = queryStringOfActionUrl.substring(0, equalsSignPos > -1 ? equalsSignPos : queryStringOfActionUrl.length());
           if ((this.appendQuestionmarkOrAmpersandToLinks) && (queryStringOfActionUrl_beforeEqualsSign.endsWith("&"))) queryStringOfActionUrl_beforeEqualsSign = queryStringOfActionUrl_beforeEqualsSign.substring(0, queryStringOfActionUrl_beforeEqualsSign.length() - 1);
           String queryStringOfActionUrl_afterEqualsSign = queryStringOfActionUrl.substring((equalsSignPos > -1) && (equalsSignPos < queryStringOfActionUrl.length() - 1) ? equalsSignPos + 1 : queryStringOfActionUrl.length());
           if ((this.encryptQueryStringsEnabled) && ("0".equals(queryStringOfActionUrl_afterEqualsSign))) queryStringOfActionUrl_afterEqualsSign = "1";
           writeToUnderlyingSink(" <input type=\"hidden\" name=\"" + queryStringOfActionUrl_beforeEqualsSign + "\" value=\"" + queryStringOfActionUrl_afterEqualsSign + "\" /> ");
         }
       }
       this.isWithinForm = false;
       this.formFieldExclusionsOfCurrentForm.clear();
       this.parameterAndFormProtectionOfCurrentForm = null;
       this.actionUrlOfCurrentForm = null;
     }



     if (this.selectboxProtection)
     {
       if (this.isWithinOption) finishOptionDisplayValueCollecting();
       if (startsWithSelectAndProtectionIsActive) {
         this.isWithinSelectBox = false;
         this.nameOfCurrentSelectBox = null;
       }
     }



     if (this.isWithinScript)
       this.scriptBody.write(tag.getBytes(this.encoding)); else {
       writeToUnderlyingSink(tag);
     }
   }




   public void handlePseudoTagRestart(char[] stuff)
     throws IOException
   {
     if (this.isWithinScript) {
       this.scriptBody.write(new String(stuff).getBytes(this.encoding));
     } else {
       writeToUnderlyingSink(stuff, 0, stuff.length);
     }
   }



   public void handleText(int b)
     throws IOException
   {
     if (this.isWithinScript) {
       this.scriptBody.write(b);
     } else {
       if ((this.selectboxProtection) && (this.isCollectingDisplayValueAsOptionValue)) {
         this.collectedDisplayValue.write(b);
       }

       super.handleText(b);
     }
   }



   public void handleText(String text)
     throws IOException
   {
     if (this.isWithinScript) {
       this.scriptBody.write(text.getBytes(this.encoding));
     } else {
       if ((this.selectboxProtection) && (this.isCollectingDisplayValueAsOptionValue)) {
         this.collectedDisplayValue.write(text.getBytes(this.encoding));
       }

       super.handleText(text);
     }
   }


   private void finishOptionDisplayValueCollecting()
     throws UnsupportedEncodingException
   {
     this.isWithinOption = false;
     if ((this.isCollectingDisplayValueAsOptionValue) && (this.parameterAndFormProtectionOfCurrentForm != null)) {
       String value = this.collectedDisplayValue.toString(this.encoding);
       value = ServerUtils.decodeBrokenValueExceptUrlEncoding(value);
       this.parameterAndFormProtectionOfCurrentForm.addSelectboxFieldAllowedValue(this.nameOfCurrentSelectBox, value);

       this.isCollectingDisplayValueAsOptionValue = false;
       this.collectedDisplayValue.reset();
     }
   }




   private void prefilterMatchingFormMaskingExclusions(String extractedFormNameDecoded)
   {
     if (extractedFormNameDecoded == null) { extractedFormNameDecoded = "";
     }
       for (FormFieldMaskingExcludeDefinition formFieldExclusion : this.matchingFormFieldMaskingExclusions) {
           if (((formFieldExclusion.getFormNamePrefilter() == null) || (WordMatchingUtils.matchesWord(formFieldExclusion.getFormNamePrefilter(), extractedFormNameDecoded, 60))) && (formFieldExclusion.getFormNamePattern().matcher(extractedFormNameDecoded).find()))
               this.formFieldExclusionsOfCurrentForm.add(formFieldExclusion);
       }
   }

   private boolean isFormFieldMaskingExclusion(String extractedFieldNameDecoded) {
     if (extractedFieldNameDecoded == null) extractedFieldNameDecoded = "";
     for (Iterator iter = this.formFieldExclusionsOfCurrentForm.iterator(); iter.hasNext();) {
       FormFieldMaskingExcludeDefinition exclusion = (FormFieldMaskingExcludeDefinition)iter.next();
       if (((exclusion.getFieldNamePrefilter() == null) || (WordMatchingUtils.matchesWord(exclusion.getFieldNamePrefilter(), extractedFieldNameDecoded, 60))) && (exclusion.getFieldNamePattern().matcher(extractedFieldNameDecoded).find()))
         return true;
     }
     return false;
   }















   private void writeScriptBodyWithLinksAdjusted()
     throws IOException
   {
     if ((this.scriptBody != null) && (this.scriptBody.size() > 0))
     {
       String script = this.scriptBody.toString(this.encoding);
       this.scriptBody.reset();
       script = applyLinkModifications(script, this.prefiltersToCaptureLinksWithinScripts, this.matchersToCaptureLinksWithinScripts, this.prefiltersToExcludeCompleteScript, this.matchersToExcludeCompleteScript, this.prefiltersToExcludeLinksWithinScripts, this.matchersToExcludeLinksWithinScripts, this.groupNumbersToCaptureLinksWithinScripts);
       writeToUnderlyingSink(script);
     }
   }











   private String applyLinkModifications(String scriptOrTag, WordDictionary[] capturingPrefilters, Matcher[] capturingMatchers, WordDictionary[] exclusionPrefiltersComplete, Matcher[] exclusionMatchersComplete, WordDictionary[] exclusionPrefiltersWithin, Matcher[] exclusionMatchersWithin, int[][] capturingGroupNumbers)
   {
     if (scriptOrTag == null) return null;
     for (int i = 0; i < capturingMatchers.length; i++)
     {
       scriptOrTag = replaceAllLocations(scriptOrTag, capturingPrefilters[i], capturingMatchers[i], exclusionPrefiltersComplete[i], exclusionMatchersComplete[i], exclusionPrefiltersWithin[i], exclusionMatchersWithin[i], capturingGroupNumbers[i]);
     }
     return scriptOrTag;
   }




   private String replaceAllLocations(String scriptOrTag, WordDictionary capturingPrefilter, Matcher capturingMatcher, WordDictionary exclusionPrefilterComplete, Matcher exclusionMatcherComplete, WordDictionary exclusionPrefilterWithin, Matcher exclusionMatcherWithin, int[] capturingGroupNumberAlternatives)
   {
     if (capturingMatcher == null) return scriptOrTag;
     if ((capturingPrefilter != null) && (!WordMatchingUtils.matchesWord(capturingPrefilter, scriptOrTag, 60))) return scriptOrTag;
     if ((exclusionMatcherComplete != null) && (WordMatchingUtils.matchesWord(exclusionPrefilterComplete, scriptOrTag, 60)) && (exclusionMatcherComplete.reset(scriptOrTag).find())) return scriptOrTag;
     StringBuilder result = new StringBuilder(scriptOrTag.length() + 100);

     capturingMatcher.reset(scriptOrTag);

     int pos = 0;
     while (capturingMatcher.find()) {
       String match = capturingMatcher.group();
       if ((exclusionMatcherWithin == null) || (!WordMatchingUtils.matchesWord(exclusionPrefilterWithin, match, 60)) || (!exclusionMatcherWithin.reset(match).find())) {
         int i = 0;
         int capturingGroupNumber;
         String url; do { capturingGroupNumber = capturingGroupNumberAlternatives[(i++)];
           url = capturingMatcher.group(capturingGroupNumber);
           if (url != null) {
             url = url.trim();
             break;
           }
         } while ((capturingGroupNumber > 0) && (url == null));
         int start = capturingMatcher.start(capturingGroupNumber);




         if (ServerUtils.isInternalHostURL(this.currentRequestUrlToCompareWith, ServerUtils.decodeBrokenValueHtmlOnly(url, false)))
         {
           String extractedURI = ResponseUtils.extractURI(url);
           if (!this.contentInjectionHelper.isMatchingIncomingLinkModificationExclusion(extractedURI)) {
             int end = capturingMatcher.end(capturingGroupNumber);
             result.append(scriptOrTag.substring(pos, start));
             if (((!this.encryptQueryStringsEnabled) || (!ResponseUtils.isAlreadyEncrypted(this.cryptoDetectionString, url))) &&
               (this.injectSecretTokensEnabled)) {
               String urlDecoded = ServerUtils.decodeBrokenValueHtmlOnly(url, false);
               if (!ServerUtils.startsWithJavaScriptOrMailto(urlDecoded)) {
                 url = urlDecoded;
                 url = ResponseUtils.injectParameterIntoURL(url, this.tokenKey, this.tokenValue, this.maskAmpersandsInModifiedLinks, this.appendQuestionmarkOrAmpersandToLinks, true);
                 if ((this.protectParamsAndFormsEnabled) && (!this.contentInjectionHelper.isExtraStrictParameterCheckingForEncryptedLinks()))
                 {
                   String parameterAndFormProtectionValue = ResponseUtils.getKeyForParameterProtectionOnly(url, getSession(), this.hiddenFormFieldProtection, this.reuseSessionContent, this.applySetAfterWrite);
                   url = ResponseUtils.injectParameterIntoURL(url, this.protectionTokenKeyKey, parameterAndFormProtectionValue, this.maskAmpersandsInModifiedLinks, this.appendQuestionmarkOrAmpersandToLinks, true);
                 }
                 url = ServerUtils.encodeHtmlSafe(url);
                 if (this.encryptQueryStringsEnabled) {
                   url = ResponseUtils.encryptQueryStringInURL(this.currentRequestUrlToCompareWith, this.contextPath, this.servletPath, url, false, false, null, this.contentInjectionHelper.isSupposedToBeStaticResource(extractedURI), this.cryptoDetectionString, this.cipher, this.cryptoKey, this.useFullPathForResourceToBeAccessedProtection, this.additionalFullResourceRemoval, this.additionalMediumResourceRemoval, this.response, this.appendQuestionmarkOrAmpersandToLinks);
                 }
                 if ((this.appendSessionIdToLinks) && (url != null)) { url = this.response.encodeURL(url);
                 }
               }
             }
             result.append(url);
             pos = end;
           } } } }
     result.append(scriptOrTag.substring(pos));
     return result.toString();
   }






   private HttpSession getSession()
   {
     if (this.session == null) {
       this.session = this.request.getSession(false);
       if (this.session == null) System.err.println("Strange situation: session is null where it should not be null");
     }
     return this.session;
   }
 }


