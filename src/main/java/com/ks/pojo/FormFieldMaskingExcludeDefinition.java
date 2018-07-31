package com.ks.pojo;

import java.util.regex.Pattern;


public final class FormFieldMaskingExcludeDefinition
		extends SimpleDefinition {
	private static final long serialVersionUID = 1L;
	private WordDictionary formNamePrefilter;
	private WordDictionary fieldNamePrefilter;
	private Pattern formNamePattern;
	private Pattern fieldNamePattern;

	public FormFieldMaskingExcludeDefinition(boolean enabled, String identification, String description, WordDictionary servletPathOrRequestURIPrefilter, Pattern servletPathOrRequestURIPattern) {
		super(enabled, identification, description, servletPathOrRequestURIPrefilter, servletPathOrRequestURIPattern);
	}

	public WordDictionary getFieldNamePrefilter() {
		return this.fieldNamePrefilter;
	}

	public void setFieldNamePrefilter(WordDictionary fieldNamePrefilter) {
		this.fieldNamePrefilter = fieldNamePrefilter;
	}

	public WordDictionary getFormNamePrefilter() {
		return this.formNamePrefilter;
	}

	public void setFormNamePrefilter(WordDictionary formNamePrefilter) {
		this.formNamePrefilter = formNamePrefilter;
	}


	public Pattern getFieldNamePattern() {
		return this.fieldNamePattern;
	}

	public void setFieldNamePattern(Pattern fieldNamePattern) {
		this.fieldNamePattern = fieldNamePattern;
	}

	public Pattern getFormNamePattern() {
		return this.formNamePattern;
	}

	public void setFormNamePattern(Pattern formNamePattern) {
		this.formNamePattern = formNamePattern;
	}
}

