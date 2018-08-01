package com.ks;

import com.ks.attack.AttackHandler;
import com.ks.attack.AttackLogger;
import com.ks.config.ConfigurationManager;
import com.ks.exceptions.AttackLoggingException;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.utils.ConfigurationUtils;
import com.ks.utils.ParamConsts;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.FilterConfig;
import java.io.File;
import java.util.logging.*;

public final class DefaultAttackLogger implements AttackLogger {
	public static final String PARAM_DIRECTORY = "DefaultAttackLoggerDirectory";
	public static final String LEGACY_PARAM_DIRECTORY = "AttackLogDirectory";
	public static final String PARAM_COUNT = "DefaultAttackLoggerPreAndPostCount";
	public static final String LEGACY_PARAM_COUNT = "PreAndPostAttackLogCount";


	private String directory = "";
	private String DEFAULT_LOGGER_DIRECTORY = "/ks-waf/logs/";
	private int prePostCount = 0; // 0 = disabled = the fastest setting (pre/post count)

	private Logger securityLogger;
	private Handler handlerForSecurityLogging;
	private MemoryHandler memoryHandlerPointerForSecurityLogging; // pointer to concrete memory-handler (also set as more abstract this.handler when a memory-handler is used)
	private FileHandler fileHandlerPointerForSecurityLogging; // pointer to concrete file-handler (also set as more abstract this.handler when no memory-handler is used)
	// number of requests to log *after* an attack has happended
	private volatile int currentPostAttackLogCounter;


	public void setFilterConfig(final FilterConfig filterConfig) throws FilterConfigurationException {
		if (filterConfig == null) throw new IllegalArgumentException("filterConfig must not be null");
		final ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
		{
			String value = ConfigurationUtils.extractMandatoryConfigValue(configManager,PARAM_DIRECTORY);
			if (value == null) value = DEFAULT_LOGGER_DIRECTORY;
			this.directory = value.trim();
		}
		{
			String value = configManager.getConfigurationValue(PARAM_COUNT);
			if (value == null) value = "0";
			try {
				this.prePostCount = Integer.parseInt(value.trim());
				if (this.prePostCount < 0) throw new FilterConfigurationException("Configured 'pre/post-attack log size' must not be negative: "+value);
			} catch(NumberFormatException e) {
				throw new FilterConfigurationException("Unable to number-parse configured 'pre/post-attack log size': "+value);
			}
		}
	}



	@Override
	public void init(final String application, final boolean logVerboseForDevelopmentMode) {
		this.securityLogger =  Logger.getLogger("KsWaf-Security."+application);
		if(StringUtils.isEmpty(this.directory)){
			this.directory = DEFAULT_LOGGER_DIRECTORY;
		}
		// create file logging
		final File file = new File(directory);
		file.mkdirs();
		if (!file.exists()) file.mkdirs(); System.out.println("KS-WAF log directory was created: "+file.getAbsolutePath());
		if (!file.exists()) System.out.println("KS-WAF log directory doesn't exist: "+file.getAbsolutePath());
		final String applicationAdjusted;
		if (application == null || application.trim().length() == 0) {
			applicationAdjusted = "";
			System.out.println("KS-WAF logs attacks for this application to "+file.getAbsolutePath());
		} else {
			System.out.println("KS-WAF logs attacks for application "+application.trim()+" to "+file.getAbsolutePath());
			applicationAdjusted = "."+application.trim();
		}
		directory = AttackHandler.getAbsolutePathLoggingSafe(file);
		try {
			// be secure and avoid logging security stuff at any other locations (i.e. parent loggers) too when logging in custom file
			this.securityLogger.setUseParentHandlers(false);
			fileHandlerPointerForSecurityLogging = new FileHandler(directory+"/ks-waf-Security"+applicationAdjusted+"-%g-%u.log", 1024*1024*5, 20, false);
			this.fileHandlerPointerForSecurityLogging.setEncoding(ParamConsts.DEFAULT_CHARACTER_ENCODING);
			final Formatter formatter = new SimpleFormatter();
			fileHandlerPointerForSecurityLogging.setFormatter(formatter);
			if (logVerboseForDevelopmentMode) {
				this.handlerForSecurityLogging = fileHandlerPointerForSecurityLogging; //= use without MemoryHandler wrapper
				// set logger level to fine to be verbose
				securityLogger.setLevel(Level.FINE);
			} else {
				// filter through memory-handler when defined
				if (this.prePostCount > 0) {
					this.memoryHandlerPointerForSecurityLogging = new MemoryHandler(fileHandlerPointerForSecurityLogging, prePostCount+1, Level.WARNING); // +1 since the attack itself is also counted
					this.memoryHandlerPointerForSecurityLogging.setEncoding(ParamConsts.DEFAULT_CHARACTER_ENCODING);
					this.handlerForSecurityLogging = this.memoryHandlerPointerForSecurityLogging;
					securityLogger.setLevel(Level.FINE); // to have the FINE logged pre-attack requests being written to the file on an attack
				} else this.handlerForSecurityLogging = fileHandlerPointerForSecurityLogging; //= use without MemoryHandler wrapper
			}
			securityLogger.addHandler(this.handlerForSecurityLogging);
		} catch (Exception e) {
			System.err.println("Unable to initialize security logging: "+ e.getMessage());
			e.printStackTrace();
		}
	}

	public void destroy() {
		// cleanup logging stuff
		if (this.securityLogger != null && this.handlerForSecurityLogging != null) {
			this.handlerForSecurityLogging.close();
			securityLogger.removeHandler(this.handlerForSecurityLogging);
			this.handlerForSecurityLogging = null;
			this.securityLogger = null;
		}
	}

	public int getPrePostCount() {
		return prePostCount;
	}

	private void decreasePostAttackLogCounter() {
		if (this.memoryHandlerPointerForSecurityLogging != null) {
			if (this.currentPostAttackLogCounter > 0) this.currentPostAttackLogCounter--;
			// to stop the post-attack logging feature, set the memory handler's push-level back to WARNING
			if (this.currentPostAttackLogCounter <= 0 && this.memoryHandlerPointerForSecurityLogging.getPushLevel() != Level.WARNING) this.memoryHandlerPointerForSecurityLogging.setPushLevel(Level.WARNING);
		}
	}



	public void log(boolean warning, String message) throws AttackLoggingException {
		if (this.securityLogger == null) return;

		if (warning) {
			// post-attack logging stuff
			if (this.prePostCount > 0 && this.memoryHandlerPointerForSecurityLogging != null) {
				// set post-attack-counter to number of regular requests to log *after* this attack
				this.currentPostAttackLogCounter = this.prePostCount;
				// to have the post-attack logging feature, set the memory handler's push-level to FINE
				this.memoryHandlerPointerForSecurityLogging.setPushLevel(Level.FINE);
			}
		} else {
			decreasePostAttackLogCounter();
		}

		final LogRecord record = new LogRecord(warning? Level.WARNING:Level.FINE, message);
		record.setSourceClassName("KsWaf");
		record.setSourceMethodName("log");
		this.securityLogger.log(record);
	}

}
