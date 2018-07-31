package com.ks.trackers;

import com.ks.attack.AttackHandler;
import com.ks.pojo.DenialOfServiceLimitDefinition;
import com.ks.pojo.interfaces.Counter;
import com.ks.tasks.CleanupTrackingCounterTask;
import com.ks.utils.CryptoUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


public final class DenialOfServiceLimitTracker {
	private final Map denialOfServiceCounter = Collections.synchronizedMap(new HashMap());


	private final AttackHandler attackHandler;

	private Timer cleanupTimer;

	private TimerTask task;


	public DenialOfServiceLimitTracker(AttackHandler attackHandler, long cleanupIntervalMillis) {
		if (attackHandler == null) throw new IllegalArgumentException("attackHandler must not be null");
		this.attackHandler = attackHandler;
		initTimers(cleanupIntervalMillis);
	}

	private void initTimers(long cleanupIntervalMillis) {
		this.cleanupTimer = new Timer(true);
		this.task = new CleanupTrackingCounterTask("DenialOfServiceLimitTracker", this.denialOfServiceCounter);
		this.cleanupTimer.scheduleAtFixedRate(this.task, CryptoUtils.generateRandomNumber(false, 60000, 300000), cleanupIntervalMillis);
	}

	public void destroy() {
		this.denialOfServiceCounter.clear();
		if (this.task != null) {
			this.task.cancel();
			this.task = null;
		}
		if (this.cleanupTimer != null) {
			this.cleanupTimer.cancel();
			this.cleanupTimer = null;
			this.denialOfServiceCounter.clear();
		}
	}

	public void trackDenialOfServiceRequest(String ip, DenialOfServiceLimitDefinition definition, HttpServletRequest request) {
		if ((this.cleanupTimer != null) && (definition != null)) {
			synchronized (this.denialOfServiceCounter) {

				Map client2CounterMap = (Map) this.denialOfServiceCounter.get(definition);
				if (client2CounterMap == null) {
					client2CounterMap = new HashMap();
					this.denialOfServiceCounter.put(definition, client2CounterMap);
				}
				assert (client2CounterMap != null);


				Counter counter = (Counter) client2CounterMap.get(ip);
				if (counter == null) {
					counter = new TrackingCounter(definition.getWatchPeriodMillis());
					client2CounterMap.put(ip, counter);
				} else {
					counter.setResetPeriodMillis(definition.getWatchPeriodMillis());
					counter.increment();
				}
				assert (counter != null);


				if (counter.getCounter() > definition.getClientDenialOfServiceLimit()) {
					client2CounterMap.remove(ip);
					this.attackHandler.handleAttack(request, ip, "Denial-of-Service limit exceeded: " + definition);
				}
			}
		}
	}
}


