package com.ks.pojo;

import java.util.regex.Pattern;

public class MultipartSizeLimitDefinition extends SimpleDefinition {

	private boolean multipartAllowed;
	private int maxInputStreamLength, maxFileUploadCount, maxFileUploadSize, maxFileNameLength, zipBombThresholdTotalSize, zipBombThresholdFileCount;

	public MultipartSizeLimitDefinition(final boolean enabled, final String identification, final String description, final WordDictionary servletPathOrRequestURIPrefilter, final Pattern servletPathOrRequestURIPattern) {
		super(enabled, identification, description, servletPathOrRequestURIPrefilter, servletPathOrRequestURIPattern);
	}

	public boolean isMultipartAllowed() {
		return multipartAllowed;
	}
	public void setMultipartAllowed(boolean multipartAllowed) {
		this.multipartAllowed = multipartAllowed;
	}

	public int getMaxFileNameLength() {
		return maxFileNameLength;
	}
	public void setMaxFileNameLength(int maxFileNameLength) {
		this.maxFileNameLength = maxFileNameLength;
	}

	public int getMaxFileUploadCount() {
		return maxFileUploadCount;
	}
	public void setMaxFileUploadCount(int maxFileUploadCount) {
		this.maxFileUploadCount = maxFileUploadCount;
	}

	public int getMaxFileUploadSize() {
		return maxFileUploadSize;
	}
	public void setMaxFileUploadSize(int maxFileUploadSize) {
		this.maxFileUploadSize = maxFileUploadSize;
	}

	public int getMaxInputStreamLength() {
		return maxInputStreamLength;
	}
	public void setMaxInputStreamLength(int maxInputStreamLength) {
		this.maxInputStreamLength = maxInputStreamLength;
	}



	public int getZipBombThresholdTotalSize() {
		return this.zipBombThresholdTotalSize;
	}
	public void setZipBombThresholdTotalSize(final int limit) {
		this.zipBombThresholdTotalSize = limit;
	}

	public int getZipBombThresholdFileCount() {
		return this.zipBombThresholdFileCount;
	}
	public void setZipBombThresholdFileCount(final int limit) {
		this.zipBombThresholdFileCount = limit;
	}

}
