package com.ks.pojo;

import com.ks.tasks.CleanupBlacklistTask;
import com.ks.utils.CryptoUtils;

import java.util.*;

public class ClientBlacklist {

	private final Map blockedClients = Collections.synchronizedMap(new HashMap());

	private final long blockPeriodMillis;

	private Timer cleanupTimer;
	private TimerTask task;


	public ClientBlacklist(final long cleanupIntervalMillis, final long blockPeriodMillis) {
		this.blockPeriodMillis = blockPeriodMillis;
		initTimers(cleanupIntervalMillis);
	}

	private void initTimers(final long cleanupIntervalMillis) {
		this.cleanupTimer = new Timer(/*"ClientBlacklist-cleanup", */true);
		this.task = new CleanupBlacklistTask("ClientBlacklist",this.blockedClients);
		this.cleanupTimer.scheduleAtFixedRate(task, CryptoUtils.generateRandomNumber(false,60000,300000), cleanupIntervalMillis);
	}

	public void destroy() {
		this.blockedClients.clear();
		if (this.task != null) {
			this.task.cancel();
			this.task = null;
		}
		if (this.cleanupTimer != null) {
			this.cleanupTimer.cancel();
			this.cleanupTimer = null;
			this.blockedClients.clear();
		}
	}


	public boolean isBlacklisted(final String ip) {
		if (!this.blockedClients.containsKey(ip)) return false;
		synchronized (this.blockedClients) {
			final Long blockedUntilMillis = (Long) this.blockedClients.get(ip);
			if (blockedUntilMillis == null) return false;
			if (System.currentTimeMillis() < blockedUntilMillis) {
				return true;
			}
			// OK, blocking is over, so remove the blockade
			this.blockedClients.remove(ip);
			return false;
		}
	}

	public void blacklistClient(final String ip) {
		if (this.cleanupTimer != null) {
			final Long blockedUntilMillis = new Long( System.currentTimeMillis() + this.blockPeriodMillis );
			this.blockedClients.put(ip, blockedUntilMillis);
		}
	}

}