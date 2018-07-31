package com.ks.loaders;

import com.ks.exceptions.RuleLoadingException;
import com.ks.pojo.RuleFile;
import com.ks.pojo.interfaces.Configurable;

public interface RuleFileLoader extends Configurable {

	void setPath(String paramString);

	String getPath();

	RuleFile[] loadRuleFiles()
			throws RuleLoadingException;

}
