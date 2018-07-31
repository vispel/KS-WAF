package com.ks.filter;

import com.ks.container.*;

public class RuleDefinitions {

    private WhitelistRequestDefinitionContainer whiteListDefinitions;
    private BadRequestDefinitionContainer badRequestDefinitions;
    private DenialOfServiceLimitDefinitionContainer denialOfServiceLimitDefinitions;
    private EntryPointDefinitionContainer entryPointDefinitions;
    private OptimizationHintDefinitionContainer optimizationHintDefinitions;
    private RenewSessionAndTokenPointDefinitionContainer renewSessionPointDefinitions;
    private IncomingProtectionExcludeDefinitionContainer incomingProtectionExcludeDefinitions;
    private ResponseModificationDefinitionContainer responseModificationDefinitions;
    private FormFieldMaskingExcludeDefinitionContainer formFieldMaskingExcludeDefinitions;
    private TotalExcludeDefinitionContainer totalExcludeDefinitions;
    private ContentModificationExcludeDefinitionContainer contentModificationExcludeDefinitions;
    private SizeLimitDefinitionContainer sizeLimitDefinitions;
    private MultipartSizeLimitDefinitionContainer multipartSizeLimitDefinitions;
    private DecodingPermutationDefinitionContainer decodingPermutationDefinitions;

    public WhitelistRequestDefinitionContainer getWhiteListDefinitions() {
        return whiteListDefinitions;
    }

    public void setWhiteListDefinitions(WhitelistRequestDefinitionContainer whiteListDefinitions) {
        this.whiteListDefinitions = whiteListDefinitions;
    }

    public BadRequestDefinitionContainer getBadRequestDefinitions() {
        return badRequestDefinitions;
    }

    public void setBadRequestDefinitions(BadRequestDefinitionContainer badRequestDefinitions) {
        this.badRequestDefinitions = badRequestDefinitions;
    }

    public DenialOfServiceLimitDefinitionContainer getDenialOfServiceLimitDefinitions() {
        return denialOfServiceLimitDefinitions;
    }

    public void setDenialOfServiceLimitDefinitions(DenialOfServiceLimitDefinitionContainer denialOfServiceLimitDefinitions) {
        this.denialOfServiceLimitDefinitions = denialOfServiceLimitDefinitions;
    }

    public EntryPointDefinitionContainer getEntryPointDefinitions() {
        return entryPointDefinitions;
    }

    public void setEntryPointDefinitions(EntryPointDefinitionContainer entryPointDefinitions) {
        this.entryPointDefinitions = entryPointDefinitions;
    }

    public OptimizationHintDefinitionContainer getOptimizationHintDefinitions() {
        return optimizationHintDefinitions;
    }

    public void setOptimizationHintDefinitions(OptimizationHintDefinitionContainer optimizationHintDefinitions) {
        this.optimizationHintDefinitions = optimizationHintDefinitions;
    }

    public RenewSessionAndTokenPointDefinitionContainer getRenewSessionPointDefinitions() {
        return renewSessionPointDefinitions;
    }

    public void setRenewSessionPointDefinitions(RenewSessionAndTokenPointDefinitionContainer renewSessionPointDefinitions) {
        this.renewSessionPointDefinitions = renewSessionPointDefinitions;
    }

    public IncomingProtectionExcludeDefinitionContainer getIncomingProtectionExcludeDefinitions() {
        return incomingProtectionExcludeDefinitions;
    }

    public void setIncomingProtectionExcludeDefinitions(IncomingProtectionExcludeDefinitionContainer incomingProtectionExcludeDefinitions) {
        this.incomingProtectionExcludeDefinitions = incomingProtectionExcludeDefinitions;
    }

    public ResponseModificationDefinitionContainer getResponseModificationDefinitions() {
        return responseModificationDefinitions;
    }

    public void setResponseModificationDefinitions(ResponseModificationDefinitionContainer responseModificationDefinitions) {
        this.responseModificationDefinitions = responseModificationDefinitions;
    }

    public FormFieldMaskingExcludeDefinitionContainer getFormFieldMaskingExcludeDefinitions() {
        return formFieldMaskingExcludeDefinitions;
    }

    public void setFormFieldMaskingExcludeDefinitions(FormFieldMaskingExcludeDefinitionContainer formFieldMaskingExcludeDefinitions) {
        this.formFieldMaskingExcludeDefinitions = formFieldMaskingExcludeDefinitions;
    }

    public TotalExcludeDefinitionContainer getTotalExcludeDefinitions() {
        return totalExcludeDefinitions;
    }

    public void setTotalExcludeDefinitions(TotalExcludeDefinitionContainer totalExcludeDefinitions) {
        this.totalExcludeDefinitions = totalExcludeDefinitions;
    }

    public ContentModificationExcludeDefinitionContainer getContentModificationExcludeDefinitions() {
        return contentModificationExcludeDefinitions;
    }

    public void setContentModificationExcludeDefinitions(ContentModificationExcludeDefinitionContainer contentModificationExcludeDefinitions) {
        this.contentModificationExcludeDefinitions = contentModificationExcludeDefinitions;
    }

    public SizeLimitDefinitionContainer getSizeLimitDefinitions() {
        return sizeLimitDefinitions;
    }

    public void setSizeLimitDefinitions(SizeLimitDefinitionContainer sizeLimitDefinitions) {
        this.sizeLimitDefinitions = sizeLimitDefinitions;
    }

    public MultipartSizeLimitDefinitionContainer getMultipartSizeLimitDefinitions() {
        return multipartSizeLimitDefinitions;
    }

    public void setMultipartSizeLimitDefinitions(MultipartSizeLimitDefinitionContainer multipartSizeLimitDefinitions) {
        this.multipartSizeLimitDefinitions = multipartSizeLimitDefinitions;
    }

    public DecodingPermutationDefinitionContainer getDecodingPermutationDefinitions() {
        return decodingPermutationDefinitions;
    }

    public void setDecodingPermutationDefinitions(DecodingPermutationDefinitionContainer decodingPermutationDefinitions) {
        this.decodingPermutationDefinitions = decodingPermutationDefinitions;
    }
}
