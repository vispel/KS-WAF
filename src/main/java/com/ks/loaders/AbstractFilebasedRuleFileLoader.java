package com.ks.loaders;

import com.ks.exceptions.FilterConfigurationException;
import com.ks.utils.ConfigurationUtils;

import javax.servlet.FilterConfig;

public class AbstractFilebasedRuleFileLoader extends AbstractRuleFileLoader {

	public static final String PARAM_RULE_FILES_SUFFIX = "RuleFilesSuffix";
	protected static final String SUFFIX = "wcr";
	protected String suffix = "wcr";

	public void setFilterConfig(FilterConfig filterConfig)
			throws FilterConfigurationException
	{
		this.suffix = ConfigurationUtils.extractOptionalConfigValue(filterConfig, "RuleFilesSuffix", "wcr");
		if (!this.suffix.startsWith(".")) {
			this.suffix = ("." + this.suffix);
		}
	}

	protected boolean isMatchingSuffix(String filename)
	{
		if (filename == null) {
			return false;
		}
		int pos = filename.lastIndexOf('.');
		if (pos == -1) {
			return false;
		}
		return filename.substring(pos).equalsIgnoreCase(this.suffix);
	}

}
