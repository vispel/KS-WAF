package com.ks.trackers;

import com.ks.attack.AttackHandler;
import com.ks.exceptions.ServerAttackException;
import com.ks.listener.SnapshotBroadcastListener;
import com.ks.pojo.IncrementingCounter;
import com.ks.pojo.Snapshot;
import com.ks.tasks.CleanupIncrementingCounterTask;
import com.ks.tasks.ClusterPublishIncrementingCounterTask;
import com.ks.utils.CryptoUtils;
import com.ks.utils.IdGeneratorUtils;
import com.ks.utils.JmsUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


public final class SessionCreationTracker {
	private static final String SYSTEM_IDENTIFIER_OF_THIS_BOX = IdGeneratorUtils.createId();
	private final String clusterInitialContextFactory;
	private final String clusterJmsProviderUrl;
	private final String clusterJmsConnectionFactory;
	private final String clusterJmsTopic;
	private final Map sessionCreationCounter = Collections.synchronizedMap(new HashMap());

	private final AttackHandler attackHandler;
	private final int sessionCreationAttackThreshold;
	private final long resetPeriodMillis;
	private Timer cleanupTimer;
	private Timer clusterPublishTimer;
	private TimerTask cleanupTask;
	private TimerTask clusterPublishTask;
	private SnapshotBroadcastListener broadcastListener;

	public SessionCreationTracker(AttackHandler attackHandler, int sessionCreationAttackThreshold, long cleanupIntervalMillis, long resetPeriodMillis, long clusterPublishPeriodMillis, String clusterInitialContextFactory, String clusterJmsProviderUrl, String clusterJmsConnectionFactory, String clusterJmsTopic) {
		if (attackHandler == null) throw new IllegalArgumentException("attackHandler must not be null");
		if (sessionCreationAttackThreshold < 0)
			throw new IllegalArgumentException("sessionCreationAttackThreshold must not be negative");
		this.attackHandler = attackHandler;
		this.sessionCreationAttackThreshold = sessionCreationAttackThreshold;
		this.resetPeriodMillis = resetPeriodMillis;

		this.clusterInitialContextFactory = clusterInitialContextFactory;
		this.clusterJmsProviderUrl = clusterJmsProviderUrl;
		this.clusterJmsConnectionFactory = clusterJmsConnectionFactory;
		this.clusterJmsTopic = clusterJmsTopic;

		initTimers(cleanupIntervalMillis, clusterPublishPeriodMillis);
	}

	private void initTimers(long cleanupIntervalMillis, long clusterPublishPeriodMillis) {
		if (this.sessionCreationAttackThreshold > 0) {
			this.cleanupTimer = new Timer("SessionCreationTracker-cleanup", true);
			this.cleanupTask = new CleanupIncrementingCounterTask("SessionCreationTracker", this.sessionCreationCounter);
			this.cleanupTimer.scheduleAtFixedRate(this.cleanupTask, CryptoUtils.generateRandomNumber(false, 60000, 300000), cleanupIntervalMillis);

			if (clusterPublishPeriodMillis > 0L) {
				this.broadcastListener = new ClusterSubscribeIncrementingCounterClient("SessionCreationTracker", SYSTEM_IDENTIFIER_OF_THIS_BOX, this.sessionCreationCounter);
				JmsUtils.addSnapshotBroadcastListener("SessionCreationTracker", this.broadcastListener);

				this.clusterPublishTimer = new Timer("HttpStatusCodeTracker-clusterPublish", true);
				this.clusterPublishTask = new ClusterPublishIncrementingCounterTask("SessionCreationTracker", SYSTEM_IDENTIFIER_OF_THIS_BOX, this.clusterInitialContextFactory, this.clusterJmsProviderUrl, this.clusterJmsConnectionFactory, this.clusterJmsTopic, this.sessionCreationCounter);
				this.clusterPublishTimer.scheduleAtFixedRate(this.clusterPublishTask, CryptoUtils.generateRandomNumber(false, 30000, 120000), clusterPublishPeriodMillis);
			}
		}
	}

	public void destroy() {
		this.sessionCreationCounter.clear();
		if (this.cleanupTask != null) {
			this.cleanupTask.cancel();
			this.cleanupTask = null;
		}
		if (this.cleanupTimer != null) {
			this.cleanupTimer.cancel();
			this.cleanupTimer = null;
			this.sessionCreationCounter.clear();
		}
		if (this.clusterPublishTask != null) {
			this.clusterPublishTask.cancel();
			this.clusterPublishTask = null;
		}
		if (this.clusterPublishTimer != null) {
			this.clusterPublishTimer.cancel();
			this.clusterPublishTimer = null;
			this.sessionCreationCounter.clear();
		}
		if (this.broadcastListener != null) {
			this.broadcastListener = null;
		}
	}

	public void trackSessionCreation(String ip, HttpServletRequest request) {
		if ((this.sessionCreationAttackThreshold > 0) && (this.cleanupTimer != null)) {
			boolean broadcastRemoval = false;
			try {
				synchronized (this.sessionCreationCounter) {
					IncrementingCounter counter = (IncrementingCounter) this.sessionCreationCounter.get(ip);
					if (counter == null) {
						counter = new IncrementingCounter(this.resetPeriodMillis);
						this.sessionCreationCounter.put(ip, counter);
					} else {
						counter.increment();
					}
					if (counter.getCounter() > this.sessionCreationAttackThreshold) {
						this.sessionCreationCounter.remove(ip);
						String message = "Session creation per-client threshold exceeded (" + this.sessionCreationAttackThreshold + ")";
						this.attackHandler.handleAttack(request, ip, message);

						broadcastRemoval = this.clusterPublishTask != null;
						throw new ServerAttackException(message);
					}
				}
			} finally {
				List removals;
				if (broadcastRemoval) {
					removals = new ArrayList(1);
					removals.add(ip);
					JmsUtils.publishSnapshot(new Snapshot("SessionCreationTracker", SYSTEM_IDENTIFIER_OF_THIS_BOX, removals));
				}
			}
		}
	}


	public void trackSessionInvalidation(String ip) {
		if ((this.sessionCreationAttackThreshold > 0) && (this.cleanupTimer != null)) {
			synchronized (this.sessionCreationCounter) {
				IncrementingCounter counter = (IncrementingCounter) this.sessionCreationCounter.get(ip);
				if (counter != null) {
					counter.decrementQuietly();
				}
			}
		}
	}
}


