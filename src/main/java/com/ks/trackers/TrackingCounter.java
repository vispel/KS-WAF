package com.ks.trackers;

import com.ks.pojo.abstracts.AbstractCounter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackingCounter extends AbstractCounter {
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_AGGREGATION_PERIOD_SECONDS = 10;
	private final List counter = new ArrayList();
	private final long aggregationPeriodMillis;
	private AggregatedTrackedValue current;

	public TrackingCounter(long resetPeriodMillis) {
		super(resetPeriodMillis);
		this.aggregationPeriodMillis = Math.min(resetPeriodMillis, 10000L);

		increment();
	}


	public final synchronized void increment() {
		if (this.current == null) {
			createAndAddNewAggregation();
		} else if (this.current.timestamp >= System.currentTimeMillis()) {
			this.current.size += 1;
		} else {
			createAndAddNewAggregation();
		}
	}

	private void createAndAddNewAggregation() {
		AggregatedTrackedValue newAggregation = new AggregatedTrackedValue(this.aggregationPeriodMillis);
		this.counter.add(newAggregation);

		this.current = newAggregation;
	}

	public final synchronized boolean isOveraged() {
		cutoffOldTrackings();
		return this.counter.isEmpty();
	}

	public final synchronized int getCounter() {
		cutoffOldTrackings();
		int result = 0;
        for (Object aCounter : this.counter) {
            AggregatedTrackedValue trackedValue = (AggregatedTrackedValue) aCounter;
            result += trackedValue.size;
        }
		return result;
	}

	private final void cutoffOldTrackings() {
		if (this.counter.isEmpty()) return;
		long cutoffTimestamp = System.currentTimeMillis() - getResetPeriodMillis();
		for (Iterator iter = this.counter.iterator(); iter.hasNext();

			 iter.remove()) {
			AggregatedTrackedValue trackedValue = (AggregatedTrackedValue) iter.next();
			if (trackedValue.timestamp >= cutoffTimestamp) {
				break;
			}
		}
	}

	public final String toString() {
		return "counter with reset period: " + getResetPeriodMillis();
	}


	private static final class AggregatedTrackedValue
			implements Serializable {
		final long timestamp;
		int size = 1;

		public AggregatedTrackedValue(long aggregationPeriodMillis) {
			this.timestamp = (System.currentTimeMillis() + aggregationPeriodMillis);
		}
	}
}
