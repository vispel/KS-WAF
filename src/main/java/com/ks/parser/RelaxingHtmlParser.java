package com.ks.parser;

import java.io.IOException;
import java.util.regex.Pattern;

public interface RelaxingHtmlParser {
	boolean IGNORE_URL_PARAMETERS_ON_FORM_ACTION_WITH_METHOD_GET = true;
	Pattern PATTERN_FORM_METHOD_POST = Pattern.compile("(?i)(?s)method\\s*=\\s*[\"']?POST[\"']?");

	Pattern PATTERN_REQUIRED_INPUT_FORM_FIELD_EXCLUDING_HIDDEN_FIELDS = Pattern.compile("(?i)(?s)type\\s*=\\s*[\"']?(text|password)[\"']?");
	Pattern PATTERN_REQUIRED_INPUT_FORM_FIELD = Pattern.compile("(?i)(?s)type\\s*=\\s*[\"']?(text|password|hidden)[\"']?");

	Pattern PATTERN_HIDDEN_FORM_FIELD = Pattern.compile("(?i)(?s)type\\s*=\\s*[\"']?hidden[\"']?");


	Pattern PATTERN_CHECKBOX = Pattern.compile("(?i)(?s)type\\s*=\\s*[\"']?checkbox[\"']?");
	Pattern PATTERN_RADIOBUTTON = Pattern.compile("(?i)(?s)type\\s*=\\s*[\"']?radio[\"']?");

	boolean USE_DIRECT_ARRAY_LOOKUPS_INSTEAD_OF_STARTS_WITH = true;

	char SLASH = '/';

	char TAG_START = '<';

	char TAG_END = '>';

	String COMMENT_START = "<!--";

	String COMMENT_END = "-->";

	int[] COMMENT_END__ARRAY = {45, 45, 62};

	void handleTag(String paramString)
			throws IOException;

	void handleTagClose(String paramString)
			throws IOException;

	void handlePseudoTagRestart(char[] paramArrayOfChar)
			throws IOException;

	void handleText(int paramInt)
			throws IOException;

	void handleText(String paramString)
			throws IOException;

	void writeToUnderlyingSink(String paramString)
			throws IOException;

	void writeToUnderlyingSink(char[] paramArrayOfChar, int paramInt1, int paramInt2)
			throws IOException;

	void writeToUnderlyingSink(int paramInt)
			throws IOException;
}


