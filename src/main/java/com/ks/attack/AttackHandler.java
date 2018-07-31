package com.ks.attack;

import com.ks.Version;
import com.ks.pojo.ClientBlacklist;
import com.ks.pojo.IncrementingCounter;
import com.ks.pojo.interfaces.Counter;
import com.ks.tasks.CleanupIncrementingCounterTask;
import com.ks.utils.CryptoUtils;
import com.ks.utils.IdGeneratorUtils;
import com.ks.utils.RequestUtils;
import com.ks.utils.ServerUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.regex.Pattern;

public final class AttackHandler {

	private final Map attackCounter = Collections.synchronizedMap(new HashMap());
	private final Map redirectCounter = Collections.synchronizedMap(new HashMap());

	private final int blockAttackingClientsThreshold;
	private final int blockRepeatedRedirectsThreshold;
	private final long resetPeriodMillisAttack;
	private final long resetPeriodMillisRedirectThreshold;
	private final boolean logSessionValuesOnAttack;
	private final boolean invalidateSessionOnAttack;
	private final boolean logVerboseForDevelopmentMode;
	private final boolean logClientUserData;
	private final String blockMessage;
	private final Pattern removeSensitiveDataRequestParamNamePattern;
	private final Pattern removeSensitiveDataRequestParamNameAndValueUrlPattern;
	private final Pattern removeSensitiveDataValuePattern;
	private ClientBlacklist clientBlacklist;
	private Timer cleanupTimerAttackTracking;
	private Timer cleanupTimerRedirectTracking;
	private TimerTask taskAttackTracking;
	private TimerTask taskRedirectTracking;
	private Logger learningModeLogger;
	private FileHandler handlerForLearningModeLogging;
	private final AttackLogger attackLogger;

	public AttackHandler(AttackLogger attackLogger, int threshold, long cleanupIntervalMillis, long blockPeriodMillis, long resetPeriodMillisAttack, long resetPeriodMillisRedirectThreshold, String learingModeAggregationDirectory, String applicationName, boolean logSessionValuesOnAttack, boolean invalidateSessionOnAttack, int blockRepeatedRedirectsThreshold, boolean logVerboseForDevelopmentMode, boolean logClientUserData) {
		if (threshold < 0) throw new IllegalArgumentException("Threshold must not be negative");
		this.attackLogger = attackLogger;
		this.blockAttackingClientsThreshold = threshold;
		this.resetPeriodMillisAttack = resetPeriodMillisAttack;
		this.resetPeriodMillisRedirectThreshold = resetPeriodMillisRedirectThreshold;
		this.logSessionValuesOnAttack = logSessionValuesOnAttack;
		this.invalidateSessionOnAttack = invalidateSessionOnAttack;
		this.logVerboseForDevelopmentMode = logVerboseForDevelopmentMode;
		this.blockMessage = (Math.round(blockPeriodMillis / 1000.0D) + " seconds");
		this.blockRepeatedRedirectsThreshold = blockRepeatedRedirectsThreshold;
		if (this.blockAttackingClientsThreshold > 0)
			this.clientBlacklist = new ClientBlacklist(cleanupIntervalMillis, blockPeriodMillis);
		this.removeSensitiveDataRequestParamNamePattern = Pattern.compile("(?i)p(?:ass)?(?:wor[dt]|phrase|wd)|kennwort");
		this.removeSensitiveDataRequestParamNameAndValueUrlPattern = Pattern.compile("(?:" + removeSensitiveDataRequestParamNamePattern.pattern() + ")=[^\\&]*");
		this.removeSensitiveDataValuePattern = Pattern.compile("(?:\\d{4}[- \\+]){3}\\d{4}|(?:(?!000)([0-6]\\d{2}|7([0-6]\\d|7[012]))([ -]?)(?!00)\\d\\d\\3(?!0000)\\d{4})");
		this.logClientUserData = logClientUserData;
		initTimers(cleanupIntervalMillis);
		initLogging(learingModeAggregationDirectory, applicationName);
	}

	private void initTimers(long cleanupIntervalMillis) {
		if (this.blockAttackingClientsThreshold > 0) {
			this.cleanupTimerAttackTracking = new Timer("AttackHandler-cleanup-attacks", true);
			this.taskAttackTracking = new CleanupIncrementingCounterTask("AttackHandler-cleanup-attacks", this.attackCounter);
			this.cleanupTimerAttackTracking.scheduleAtFixedRate(this.taskAttackTracking, CryptoUtils.generateRandomNumber(false, 60000, 300000), cleanupIntervalMillis);
		}

		if (this.blockRepeatedRedirectsThreshold > 0) {
			this.cleanupTimerRedirectTracking = new Timer("AttackHandler-cleanup-redirects", true);
			this.taskRedirectTracking = new CleanupIncrementingCounterTask("AttackHandler-cleanup-redirects", this.redirectCounter);
			this.cleanupTimerRedirectTracking.scheduleAtFixedRate(this.taskRedirectTracking, CryptoUtils.generateRandomNumber(false, 60000, 300000), cleanupIntervalMillis);
		}
	}


	public void destroy() {
		this.attackCounter.clear();
		if (this.taskAttackTracking != null) {
			this.taskAttackTracking.cancel();
			this.taskAttackTracking = null;
		}
		if (this.cleanupTimerAttackTracking != null) {
			this.cleanupTimerAttackTracking.cancel();
			this.cleanupTimerAttackTracking = null;
			this.attackCounter.clear();
		}

		this.redirectCounter.clear();
		if (this.taskRedirectTracking != null) {
			this.taskRedirectTracking.cancel();
			this.taskRedirectTracking = null;
		}
		if (this.cleanupTimerRedirectTracking != null) {
			this.cleanupTimerRedirectTracking.cancel();
			this.cleanupTimerRedirectTracking = null;
			this.redirectCounter.clear();
		}

		if (this.clientBlacklist != null) {
			this.clientBlacklist.destroy();
		}
		this.attackLogger.destroy();
		if ((this.learningModeLogger != null) && (this.handlerForLearningModeLogging != null)) {
			this.handlerForLearningModeLogging.close();
			this.learningModeLogger.removeHandler(this.handlerForLearningModeLogging);
			this.handlerForLearningModeLogging = null;
			this.learningModeLogger = null;
		}
	}


	public int getBlockAttackingClientsThreshold() {
		return this.blockAttackingClientsThreshold;
	}


	public boolean shouldBeBlocked(String ip) {
		if (this.blockAttackingClientsThreshold == 0) return false;
		return (this.clientBlacklist != null) && (this.clientBlacklist.isBlacklisted(ip));
	}


	public boolean isRedirectThresholdReached(String ip) {
		if (this.blockRepeatedRedirectsThreshold == 0) return false;
		if ((this.blockRepeatedRedirectsThreshold > 0) && (this.cleanupTimerRedirectTracking != null)) {
			return trackRedirecting(ip);
		}
		return false;
	}

	public int getRedirectThreshold() {
		return this.blockRepeatedRedirectsThreshold;
	}

	public long getRedirectThresholdResetPeriod() {
		return this.resetPeriodMillisRedirectThreshold;
	}


	public void logWarningRequestMessage(String message) {
		if (this.attackLogger != null)
			try {
				StringBuilder logMessage = new StringBuilder("Warning message: ").append(Version.versionNumber()).append(" ").append("[\n");
				logMessage.append("\t").append(message).append("\n");
				logMessage.append("]");
				this.attackLogger.log(true, logMessage.toString());
			} catch (Exception e) {
				System.err.println("Unable to log request message: " + e.getMessage());
			}
	}

	public void logRegularRequestMessage(String message) {
		if ((this.attackLogger != null) && ((this.attackLogger.getPrePostCount() > 0) || (this.logVerboseForDevelopmentMode))) {
			try {
				StringBuilder logMessage = new StringBuilder("Regular message (pre/post-attack logging): ").append(Version.versionNumber()).append(" ").append("[\n");
				logMessage.append("\t").append(message).append("\n");
				logMessage.append("]");
				this.attackLogger.log(false, logMessage.toString());
			} catch (Exception e) {
				System.err.println("Unable to log request message: " + e.getMessage());
			}
		}
	}

	public void handleRegularRequest(HttpServletRequest request, String ip) {
		if ((this.attackLogger != null) && ((this.attackLogger.getPrePostCount() > 0) || ((this.logVerboseForDevelopmentMode)))) {
			try {
				StringBuilder logMessage = new StringBuilder("Regular request (pre/post-attack logging): ").append(Version.versionNumber()).append(" ").append("[\n");
				logMessage.append(RequestUtils.extractSecurityRelevantRequestContent(request, ip, false, this.removeSensitiveDataRequestParamNamePattern, this.removeSensitiveDataRequestParamNameAndValueUrlPattern, this.removeSensitiveDataValuePattern, this.logClientUserData));
				logMessage.append("]");
				this.attackLogger.log(false, logMessage.toString());
			} catch (Exception e) {
				System.err.println("Unable to log request details: " + e.getMessage());
			}
		}
	}


	public Attack handleAttack(HttpServletRequest request, String ip, String message) {
		boolean blocked = false;
		if ((this.blockAttackingClientsThreshold > 0) && (this.cleanupTimerAttackTracking != null) && (this.clientBlacklist != null)) {
			blocked = trackBlocking(ip);
		}


		String logReferenceId = IdGeneratorUtils.createId();
		StringBuilder logMessage = new StringBuilder("Reference ").append(logReferenceId).append(Version.versionNumber()).append("\n").append(message).append(" [\n");
		logMessage.append(RequestUtils.extractSecurityRelevantRequestContent(request, ip, this.logSessionValuesOnAttack, this.removeSensitiveDataRequestParamNamePattern, this.removeSensitiveDataRequestParamNameAndValueUrlPattern, this.removeSensitiveDataValuePattern, this.logClientUserData));
		logMessage.append("]");


		if (this.invalidateSessionOnAttack) {
			try {
				HttpSession session = request.getSession(false);
				if (session != null) {
					session.invalidate();
					logMessage.append(" ==> emergency action: session invalidated");
				}
			} catch (Exception e) {
				logMessage.append(" ==> emergency action failed: unable to invalidate session: ").append(e.getMessage());
			}
		}


		if (blocked) {
			logMessage.append(" ==> further protection: client will be blocked for ").append(this.blockMessage);
		}

		String logMessageString = logMessage.toString();
		if (this.attackLogger != null)
			this.attackLogger.log(true, logMessageString);
		else
			System.out.println(logMessageString);
		return new Attack(logMessageString, logReferenceId);
	}


	private void logMessage(Logger logger, Level level, String logMessage) {
		if ((level == null) || (logMessage == null)) return;
		LogRecord record = new LogRecord(level, logMessage);
		record.setSourceClassName("KsWaf");
		record.setSourceMethodName("log");
		logger.log(record);
	}

	private boolean trackBlocking(String ip) {
		if (this.clientBlacklist == null) throw new IllegalStateException("Client blacklist not initialized");
		boolean blocked = false;
		synchronized (this.attackCounter) {
			Counter counter = (Counter) this.attackCounter.get(ip);
			if (counter == null) {
				counter = new IncrementingCounter(this.resetPeriodMillisAttack);
				this.attackCounter.put(ip, counter);
			} else {
				counter.increment();
			}
			if (counter.getCounter() >= this.blockAttackingClientsThreshold) {
				this.attackCounter.remove(ip);
				blocked = true;
			}
		}
		if (blocked) this.clientBlacklist.blacklistClient(ip);
		return blocked;
	}


	private boolean trackRedirecting(String ip) {
		synchronized (this.redirectCounter) {
			Counter counter = (Counter) this.redirectCounter.get(ip);
			if (counter == null) {
				counter = new IncrementingCounter(this.resetPeriodMillisRedirectThreshold);
				this.redirectCounter.put(ip, counter);
			} else {
				counter.increment();
			}
			if (counter.getCounter() >= this.blockRepeatedRedirectsThreshold) {
				this.redirectCounter.remove(ip);
				return true;
			}
		}
		return false;
	}


	public void handleLearningModeRequestAggregation(HttpServletRequest requestAsSeenByTheApplication) {
		if ((this.learningModeLogger != null) && (requestAsSeenByTheApplication != null)) {
			try {
				String servletPath = requestAsSeenByTheApplication.getServletPath();
				if (servletPath != null) {
					StringBuilder logMessage = new StringBuilder("Regular request (learning mode): ").append(Version.versionNumber()).append(" ").append("[\n");
					RequestUtils.appendValueToMessage(logMessage, "servletPath", ServerUtils.urlEncode(servletPath));
					RequestUtils.appendValueToMessage(logMessage, "method", ServerUtils.urlEncode(requestAsSeenByTheApplication.getMethod()));
					RequestUtils.appendValueToMessage(logMessage, "mimeType", ServerUtils.urlEncode(requestAsSeenByTheApplication.getContentType()));
					RequestUtils.appendValueToMessage(logMessage, "contentLength", "" + requestAsSeenByTheApplication.getContentLength());
					RequestUtils.appendValueToMessage(logMessage, "encoding", ServerUtils.urlEncode(requestAsSeenByTheApplication.getCharacterEncoding()));
					RequestUtils.appendValueToMessage(logMessage, "referer", ServerUtils.urlEncode(requestAsSeenByTheApplication.getHeader("referer")));
					Enumeration names = requestAsSeenByTheApplication.getParameterNames();
					if (names != null) {
						while (names.hasMoreElements()) {
							String name = (String) names.nextElement();
							String[] values = requestAsSeenByTheApplication.getParameterValues(name);
							if (values != null) {
								for (String value : values) {
									RequestUtils.appendValueToMessage(logMessage, "requestParam: " + ServerUtils.urlEncode(name), ServerUtils.urlEncode(value));
								}
							}
						}
					}
					System.err.println("This servlet-container does not allow the access of request params... VERY STRANGE");

					logMessage.append("]");
					logMessage(this.learningModeLogger, Level.FINE, logMessage.toString());
				}
			} catch (Exception e) {
				System.err.println("Unable to learn from (log) request details: " + e.getMessage());
			}
		}
	}


	private void initLogging(String learningModeAggregationDirectory, String application) {
		this.attackLogger.init(application,this.logVerboseForDevelopmentMode);


		if ((learningModeAggregationDirectory != null) && (learningModeAggregationDirectory.trim().length() != 0)) {
			this.learningModeLogger = Logger.getLogger("KsWaf-LearningMode." + application);

			File file = new File(learningModeAggregationDirectory);
			String applicationAdjusted = "";
			if ((application == null) || (application.trim().length() == 0)) {
				System.out.println("KsWaf logs learning mode data for this application to " + file.getAbsolutePath());
			} else {
				System.out.println("KsWaf logs learning mode data for application " + application.trim() + " to " + file.getAbsolutePath());
				applicationAdjusted = "." + application.trim();
			}
			learningModeAggregationDirectory = getAbsolutePathLoggingSafe(file);
			try {
				this.learningModeLogger.setUseParentHandlers(false);

				this.handlerForLearningModeLogging = new FileHandler(learningModeAggregationDirectory + "/KsWaf-LearningMode" + applicationAdjusted + "-%g-%u.log", 10485760, 50, false);
				this.handlerForLearningModeLogging.setEncoding("UTF-8");
				Formatter formatter = new SimpleFormatter();
				this.handlerForLearningModeLogging.setFormatter(formatter);

				this.learningModeLogger.setLevel(Level.FINE);
				this.learningModeLogger.addHandler(this.handlerForLearningModeLogging);
			} catch (Exception e) {
				System.err.println("Unable to initialize learning mode data logging: " + e.getMessage());
			}
		}
	}


	public static final String getAbsolutePathLoggingSafe(File file) {
		String result = file.getAbsolutePath();

		result = result.replaceAll("\\\\", "/");
		result = result.replaceAll("%", "%%");
		return result;
	}
}
