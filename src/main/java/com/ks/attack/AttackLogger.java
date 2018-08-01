package com.ks.attack;

import com.ks.exceptions.AttackLoggingException;
import com.ks.pojo.interfaces.Configurable;

public interface AttackLogger extends Configurable {

	void init(final String application,final boolean isProductionMode, final boolean logVerboseForDevelopmentMode);

	void log(boolean paramBoolean, String paramString)
			throws AttackLoggingException;

	void destroy();

	int getPrePostCount();
}
