package com.ks.utils;

import com.ks.adapters.ServletOutputStreamAdapter;
import com.ks.container.ContentModificationExcludeDefinitionContainer;
import com.ks.container.FormFieldMaskingExcludeDefinitionContainer;
import com.ks.crypto.CryptoKeyAndSalt;
import com.ks.pojo.WordDictionary;
import com.ks.response.ResponseFilterStream;
import com.ks.response.ResponseFilterWriter;
import com.ks.RequestWrapper;
import com.ks.ResponseWrapper;

import javax.crypto.Cipher;
import javax.servlet.ServletOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.regex.Matcher;


public final class ContentInjectionHelper {
	private static final boolean DEBUG = false;
	private boolean injectSecretTokenIntoLinks;
	private boolean stripHtmlComments;
	private boolean protectParametersAndForms;
	private boolean extraProtectDisabledFormFields;
	private boolean extraProtectReadonlyFormFields;
	private boolean extraProtectRequestParamValueCount;
	private boolean encryptQueryStringInLinks;
	private boolean extraFullPathRemoval;
	private boolean extraMediumPathRemoval;
	private boolean extraStrictParameterCheckingForEncryptedLinks;
	private boolean useTunedBlockParser;
	private boolean useResponseBuffering;
	private ContentModificationExcludeDefinitionContainer contentModificationExcludeDefinitions;
	private FormFieldMaskingExcludeDefinitionContainer formFieldMaskingExcludeDefinitions;

	public ContentModificationExcludeDefinitionContainer getContentModificationExcludeDefinitions() {
		return this.contentModificationExcludeDefinitions;
	}

	public void setContentModificationExcludeDefinitions(ContentModificationExcludeDefinitionContainer contentModificationExcludeDefinitions) {
		this.contentModificationExcludeDefinitions = contentModificationExcludeDefinitions;
	}


	public FormFieldMaskingExcludeDefinitionContainer getFormFieldMaskingExcludeDefinitions() {
		return this.formFieldMaskingExcludeDefinitions;
	}

	public void setFormFieldMaskingExcludeDefinitions(FormFieldMaskingExcludeDefinitionContainer formFieldMaskingExcludeDefinitions) {
		this.formFieldMaskingExcludeDefinitions = formFieldMaskingExcludeDefinitions;
	}


	public boolean isStripHtmlComments() {
		return this.stripHtmlComments;
	}

	public void setStripHtmlComments(boolean stripHtmlComments) {
		this.stripHtmlComments = stripHtmlComments;
	}


	public boolean isUseTunedBlockParser() {
		return this.useTunedBlockParser;
	}

	public void setUseTunedBlockParser(boolean useTunedBlockParser) {
		this.useTunedBlockParser = useTunedBlockParser;
	}


	public boolean isUseResponseBuffering() {
		return this.useResponseBuffering;
	}

	public void setUseResponseBuffering(boolean useResponseBuffering) {
		this.useResponseBuffering = useResponseBuffering;
	}


	public boolean isProtectParametersAndForms() {
		return this.protectParametersAndForms;
	}

	public void setProtectParametersAndForms(boolean protectParametersAndForms) {
		this.protectParametersAndForms = protectParametersAndForms;
	}


	public boolean isExtraProtectDisabledFormFields() {
		return this.extraProtectDisabledFormFields;
	}

	public void setExtraProtectDisabledFormFields(boolean extraProtectDisabledFormFields) {
		this.extraProtectDisabledFormFields = extraProtectDisabledFormFields;
	}


	public boolean isExtraProtectReadonlyFormFields() {
		return this.extraProtectReadonlyFormFields;
	}

	public void setExtraProtectReadonlyFormFields(boolean extraProtectReadonlyFormFields) {
		this.extraProtectReadonlyFormFields = extraProtectReadonlyFormFields;
	}


	public boolean isExtraProtectRequestParamValueCount() {
		return this.extraProtectRequestParamValueCount;
	}

	public void setExtraProtectRequestParamValueCount(boolean extraProtectRequestParamValueCount) {
		this.extraProtectRequestParamValueCount = extraProtectRequestParamValueCount;
	}


	public boolean isEncryptQueryStringInLinks() {
		return this.encryptQueryStringInLinks;
	}

	public void setEncryptQueryStringInLinks(boolean encryptQueryStringInLinks) {
		this.encryptQueryStringInLinks = encryptQueryStringInLinks;
	}


	public boolean isExtraMediumPathRemoval() {
		return this.extraMediumPathRemoval;
	}

	public void setExtraMediumPathRemoval(boolean extraMediumPathRemoval) {
		this.extraMediumPathRemoval = extraMediumPathRemoval;
	}


	public boolean isExtraStrictParameterCheckingForEncryptedLinks() {
		return this.extraStrictParameterCheckingForEncryptedLinks;
	}

	public void setExtraStrictParameterCheckingForEncryptedLinks(boolean extraStrictParameterCheckingForEncryptedLinks) {
		this.extraStrictParameterCheckingForEncryptedLinks = extraStrictParameterCheckingForEncryptedLinks;
	}


	public boolean isExtraFullPathRemoval() {
		return this.extraFullPathRemoval;
	}

	public void setExtraFullPathRemoval(boolean extraFullPathRemoval) {
		this.extraFullPathRemoval = extraFullPathRemoval;
	}


	public boolean isInjectSecretTokenIntoLinks() {
		return this.injectSecretTokenIntoLinks;
	}

	public void setInjectSecretTokenIntoLinks(boolean injectSecretTokenIntoLinks) {
		this.injectSecretTokenIntoLinks = injectSecretTokenIntoLinks;
	}


	public ServletOutputStream addActivatedFilters(String responseCharsetEncodingName, ServletOutputStream output, String currentRequestUrlToCompareWith, String contextPath, String servletPath, String secretTokenKey, String secretTokenValue, String cryptoDetectionString, Cipher cipher, CryptoKeyAndSalt cryptoKey, String protectParametersAndFormsTokenKeyKey, RequestWrapper request, ResponseWrapper response, WordDictionary[] prefiltersToExcludeCompleteScript, Matcher[] matchersToExcludeCompleteScript, WordDictionary[] prefiltersToExcludeCompleteTag, Matcher[] matchersToExcludeCompleteTag, WordDictionary[] prefiltersToExcludeLinksWithinScripts, Matcher[] matchersToExcludeLinksWithinScripts, WordDictionary[] prefiltersToExcludeLinksWithinTags, Matcher[] matchersToExcludeLinksWithinTags, WordDictionary[] prefiltersToCaptureLinksWithinScripts, Matcher[] matchersToCaptureLinksWithinScripts, WordDictionary[] prefiltersToCaptureLinksWithinTags, Matcher[] matchersToCaptureLinksWithinTags, int[][] groupNumbersToCaptureLinksWithinScripts, int[][] groupNumbersToCaptureLinksWithinTags, boolean useFullPathForResourceToBeAccessedProtection, boolean additionalFullResourceRemoval, boolean additionalMediumResourceRemoval, boolean maskAmpersandsInModifiedLinks, boolean hiddenFormFieldProtection, boolean selectboxProtection, boolean checkboxProtection, boolean radiobuttonProtection, boolean selectboxValueMasking, boolean checkboxValueMasking, boolean radiobuttonValueMasking, boolean appendQuestionmarkOrAmpersandToLinks, boolean appendSessionIdToLinks, boolean reuseSessionContent, String honeylinkPrefix, String honeylinkSuffix, short honeylinkMaxPerPage, boolean randomizeHoneylinksOnEveryRequest, boolean applySetAfterWrite) {
		if (output == null) return null;
		java.io.OutputStream wrapper = null;

		if (((this.encryptQueryStringInLinks) && (cryptoDetectionString != null) && (cryptoKey != null)) || ((this.protectParametersAndForms) && (protectParametersAndFormsTokenKeyKey != null)) || ((this.injectSecretTokenIntoLinks) && (secretTokenKey != null) && (secretTokenValue != null)) || (this.stripHtmlComments)) {


			wrapper = output;
			wrapper = new ResponseFilterStream(wrapper, responseCharsetEncodingName, this.useTunedBlockParser, currentRequestUrlToCompareWith, contextPath, servletPath, secretTokenKey, secretTokenValue, protectParametersAndFormsTokenKeyKey, cipher, cryptoKey, this, cryptoDetectionString, request, response, this.stripHtmlComments, this.injectSecretTokenIntoLinks, this.protectParametersAndForms, this.encryptQueryStringInLinks, this.extraProtectDisabledFormFields, this.extraProtectReadonlyFormFields, this.extraProtectRequestParamValueCount, prefiltersToExcludeCompleteScript, matchersToExcludeCompleteScript, prefiltersToExcludeCompleteTag, matchersToExcludeCompleteTag, prefiltersToExcludeLinksWithinScripts, matchersToExcludeLinksWithinScripts, prefiltersToExcludeLinksWithinTags, matchersToExcludeLinksWithinTags, prefiltersToCaptureLinksWithinScripts, matchersToCaptureLinksWithinScripts, prefiltersToCaptureLinksWithinTags, matchersToCaptureLinksWithinTags, groupNumbersToCaptureLinksWithinScripts, groupNumbersToCaptureLinksWithinTags, useFullPathForResourceToBeAccessedProtection, additionalFullResourceRemoval, additionalMediumResourceRemoval, maskAmpersandsInModifiedLinks, hiddenFormFieldProtection, selectboxProtection, checkboxProtection, radiobuttonProtection, selectboxValueMasking, checkboxValueMasking, radiobuttonValueMasking, appendQuestionmarkOrAmpersandToLinks, appendSessionIdToLinks, reuseSessionContent, honeylinkPrefix, honeylinkSuffix, honeylinkMaxPerPage, randomizeHoneylinksOnEveryRequest, applySetAfterWrite);


			if (this.useResponseBuffering) {
				wrapper = new java.io.BufferedOutputStream(wrapper);
			}
		}
		if (wrapper != null) return new ServletOutputStreamAdapter(wrapper);
		return output;
	}


	public PrintWriter addActivatedFilters(PrintWriter writer, String currentRequestUrlToCompareWith, String contextPath, String servletPath, String secretTokenKey, String secretTokenValue, String cryptoDetectionString, Cipher cipher, CryptoKeyAndSalt cryptoKey, String protectParametersAndFormsTokenKeyKey, RequestWrapper request, ResponseWrapper response, WordDictionary[] prefiltersToExcludeCompleteScript, Matcher[] matchersToExcludeCompleteScript, WordDictionary[] prefiltersToExcludeCompleteTag, Matcher[] matchersToExcludeCompleteTag, WordDictionary[] prefiltersToExcludeLinksWithinScripts, Matcher[] matchersToExcludeLinksWithinScripts, WordDictionary[] prefiltersToExcludeLinksWithinTags, Matcher[] matchersToExcludeLinksWithinTags, WordDictionary[] prefiltersToCaptureLinksWithinScripts, Matcher[] matchersToCaptureLinksWithinScripts, WordDictionary[] prefiltersToCaptureLinksWithinTags, Matcher[] matchersToCaptureLinksWithinTags, int[][] groupNumbersToCaptureLinksWithinScripts, int[][] groupNumbersToCaptureLinksWithinTags, boolean useFullPathForResourceToBeAccessedProtection, boolean additionalFullResourceRemoval, boolean additionalMediumResourceRemoval, boolean maskAmpersandsInModifiedLinks, boolean hiddenFormFieldProtection, boolean selectboxProtection, boolean checkboxProtection, boolean radiobuttonProtection, boolean selectboxValueMasking, boolean checkboxValueMasking, boolean radiobuttonValueMasking, boolean appendQuestionmarkOrAmpersandToLinks, boolean appendSessionIdToLinks, boolean reuseSessionContent, String honeylinkPrefix, String honeylinkSuffix, short honeylinkMaxPerPage, boolean randomizeHoneylinksOnEveryRequest, boolean applySetAfterWrite) {
		if (writer == null) return null;
		Writer wrapper = null;

		if (((this.encryptQueryStringInLinks) && (cryptoDetectionString != null) && (cryptoKey != null)) || ((this.protectParametersAndForms) && (protectParametersAndFormsTokenKeyKey != null)) || ((this.injectSecretTokenIntoLinks) && (secretTokenKey != null) && (secretTokenValue != null)) || (this.stripHtmlComments)) {


			wrapper = writer;
			wrapper = new ResponseFilterWriter(wrapper, this.useTunedBlockParser, currentRequestUrlToCompareWith, contextPath, servletPath, secretTokenKey, secretTokenValue, protectParametersAndFormsTokenKeyKey, cipher, cryptoKey, this, cryptoDetectionString, request, response, this.stripHtmlComments, this.injectSecretTokenIntoLinks, this.protectParametersAndForms, this.encryptQueryStringInLinks, this.extraProtectDisabledFormFields, this.extraProtectReadonlyFormFields, this.extraProtectRequestParamValueCount, prefiltersToExcludeCompleteScript, matchersToExcludeCompleteScript, prefiltersToExcludeCompleteTag, matchersToExcludeCompleteTag, prefiltersToExcludeLinksWithinScripts, matchersToExcludeLinksWithinScripts, prefiltersToExcludeLinksWithinTags, matchersToExcludeLinksWithinTags, prefiltersToCaptureLinksWithinScripts, matchersToCaptureLinksWithinScripts, prefiltersToCaptureLinksWithinTags, matchersToCaptureLinksWithinTags, groupNumbersToCaptureLinksWithinScripts, groupNumbersToCaptureLinksWithinTags, useFullPathForResourceToBeAccessedProtection, additionalFullResourceRemoval, additionalMediumResourceRemoval, maskAmpersandsInModifiedLinks, hiddenFormFieldProtection, selectboxProtection, checkboxProtection, radiobuttonProtection, selectboxValueMasking, checkboxValueMasking, radiobuttonValueMasking, appendQuestionmarkOrAmpersandToLinks, appendSessionIdToLinks, reuseSessionContent, honeylinkPrefix, honeylinkSuffix, honeylinkMaxPerPage, randomizeHoneylinksOnEveryRequest, applySetAfterWrite);


			if (this.useResponseBuffering) {
				wrapper = new java.io.BufferedWriter(wrapper);
			}
		}
		if (wrapper != null) return new PrintWriter(wrapper);
		return writer;
	}


	public final boolean isSupposedToBeStaticResource(String linkTargetUri) {
		return (this.contentModificationExcludeDefinitions != null) && (this.contentModificationExcludeDefinitions.isMatchingIncomingLinkModificationExclusion(linkTargetUri));
	}

	public final boolean isMatchingIncomingLinkModificationExclusion(String linkTargetUri) {
		boolean result = (this.contentModificationExcludeDefinitions != null) && (this.extraFullPathRemoval ? this.contentModificationExcludeDefinitions.isMatchingIncomingLinkModificationExclusionEvenWhenFullPathRemovalEnabled(linkTargetUri) : this.contentModificationExcludeDefinitions.isMatchingIncomingLinkModificationExclusion(linkTargetUri));


		return result;
	}

	public final boolean isMatchingOutgoingResponseModificationExclusion(String servletPath, String requestURI) {
		boolean result = (this.contentModificationExcludeDefinitions != null) && (this.contentModificationExcludeDefinitions.isMatchingOutgoingResponseModificationExclusion(servletPath, requestURI));

		return result;
	}
}


