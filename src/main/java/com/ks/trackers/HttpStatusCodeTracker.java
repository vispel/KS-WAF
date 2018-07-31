package com.ks.trackers;

import com.ks.attack.AttackHandler;
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

public final class HttpStatusCodeTracker
{
  private static final boolean DEBUG = false;
  private static final String SYSTEM_IDENTIFIER_OF_THIS_BOX = IdGeneratorUtils.createId();
  private static final String TYPE = "HttpStatusCodeTracker";
  private final String clusterInitialContextFactory;
  private final String clusterJmsProviderUrl;
  private final String clusterJmsConnectionFactory;
  private final String clusterJmsTopic;
  private final Map httpInvalidRequestOrNotFoundCounter = Collections.synchronizedMap(new HashMap());

  private final AttackHandler attackHandler;
  private final int httpInvalidRequestOrNotFoundAttackThreshold;
  private final long resetPeriodMillis;
  private Timer cleanupTimer;
  private Timer clusterPublishTimer;
  private TimerTask cleanupTask;
  private TimerTask clusterPublishTask;
  private SnapshotBroadcastListener broadcastListener;

  public HttpStatusCodeTracker(AttackHandler attackHandler, int httpInvalidRequestOrNotFoundAttackThreshold, long cleanupIntervalMillis, long resetPeriodMillis, long clusterPublishPeriodMillis, String clusterInitialContextFactory, String clusterJmsProviderUrl, String clusterJmsConnectionFactory, String clusterJmsTopic)
  {
    if (attackHandler == null) throw new IllegalArgumentException("attackHandler must not be null");
    if (httpInvalidRequestOrNotFoundAttackThreshold < 0) throw new IllegalArgumentException("httpInvalidRequestOrNotFoundAttackThreshold must not be negative");
    this.attackHandler = attackHandler;
    this.httpInvalidRequestOrNotFoundAttackThreshold = httpInvalidRequestOrNotFoundAttackThreshold;
    this.resetPeriodMillis = resetPeriodMillis;

    this.clusterInitialContextFactory = clusterInitialContextFactory;
    this.clusterJmsProviderUrl = clusterJmsProviderUrl;
    this.clusterJmsConnectionFactory = clusterJmsConnectionFactory;
    this.clusterJmsTopic = clusterJmsTopic;

    initTimers(cleanupIntervalMillis, clusterPublishPeriodMillis);
  }

  private void initTimers(long cleanupIntervalMillis, long clusterPublishPeriodMillis)
  {
    if (this.httpInvalidRequestOrNotFoundAttackThreshold > 0)
    {
      this.cleanupTimer = new Timer("HttpStatusCodeTracker-cleanup", true);
      this.cleanupTask = new CleanupIncrementingCounterTask("HttpStatusCodeTracker", this.httpInvalidRequestOrNotFoundCounter);
      this.cleanupTimer.scheduleAtFixedRate(this.cleanupTask, CryptoUtils.generateRandomNumber(false, 60000, 300000), cleanupIntervalMillis);

      if (clusterPublishPeriodMillis > 0L)
      {
        this.broadcastListener = new ClusterSubscribeIncrementingCounterClient(TYPE, SYSTEM_IDENTIFIER_OF_THIS_BOX, this.httpInvalidRequestOrNotFoundCounter);
        JmsUtils.addSnapshotBroadcastListener("HttpStatusCodeTracker", this.broadcastListener);

        this.clusterPublishTimer = new Timer("HttpStatusCodeTracker-clusterPublish", true);
        this.clusterPublishTask = new ClusterPublishIncrementingCounterTask(TYPE, SYSTEM_IDENTIFIER_OF_THIS_BOX, this.clusterInitialContextFactory, this.clusterJmsProviderUrl, this.clusterJmsConnectionFactory, this.clusterJmsTopic, this.httpInvalidRequestOrNotFoundCounter);
        this.clusterPublishTimer.scheduleAtFixedRate(this.clusterPublishTask, CryptoUtils.generateRandomNumber(false, 30000, 120000), clusterPublishPeriodMillis);
      }
    }
  }

  public void destroy()
  {
    this.httpInvalidRequestOrNotFoundCounter.clear();
    if (this.cleanupTask != null) {
      this.cleanupTask.cancel();
      this.cleanupTask = null;
    }
    if (this.cleanupTimer != null) {
      this.cleanupTimer.cancel();
      this.cleanupTimer = null;
      this.httpInvalidRequestOrNotFoundCounter.clear();
    }
    if (this.clusterPublishTask != null) {
      this.clusterPublishTask.cancel();
      this.clusterPublishTask = null;
    }
    if (this.clusterPublishTimer != null) {
      this.clusterPublishTimer.cancel();
      this.clusterPublishTimer = null;
      this.httpInvalidRequestOrNotFoundCounter.clear();
    }
    if (this.broadcastListener != null) this.broadcastListener = null;
  }

  public void trackStatusCode(String ip, int statusCode, HttpServletRequest request)
  {
    if ((this.httpInvalidRequestOrNotFoundAttackThreshold > 0) && (this.cleanupTimer != null) && (
      (statusCode == 400) || (statusCode == 404))) {
      boolean broadcastRemoval = false;
      try {
        synchronized (this.httpInvalidRequestOrNotFoundCounter) {
          IncrementingCounter counter = (IncrementingCounter)this.httpInvalidRequestOrNotFoundCounter.get(ip);
          if (counter == null) {
            counter = new IncrementingCounter(this.resetPeriodMillis);
            this.httpInvalidRequestOrNotFoundCounter.put(ip, counter);
          } else { counter.increment();
          }
          if (counter.getCounter() > this.httpInvalidRequestOrNotFoundAttackThreshold) {
            this.httpInvalidRequestOrNotFoundCounter.remove(ip);
            this.attackHandler.handleAttack(request, ip, "HTTP 400/404 per-client threshold exceeded (" + this.httpInvalidRequestOrNotFoundAttackThreshold + ")");

            broadcastRemoval = this.clusterPublishTask != null;
          }
        }
      } finally { List removals;
        if (broadcastRemoval) {
          removals = new ArrayList(1);
          removals.add(ip);
          JmsUtils.publishSnapshot(new Snapshot(TYPE, SYSTEM_IDENTIFIER_OF_THIS_BOX, removals));
        }
      }
    }
  }
}


