package com.ks.pojo;


import com.ks.pojo.abstracts.AbstractCounter;
import com.ks.pojo.interfaces.Counter;

public final class IncrementingCounter extends AbstractCounter implements Cloneable {
	private static final long serialVersionUID = 1L;
	private long lastEventMillis;
	private long minimumTimestampForForeignActions = 0L;
	private int totalCounter;
	private int deltaCounter;

	public IncrementingCounter(long resetPeriodMillis) {
		super(resetPeriodMillis);
		increment();
	}


	public IncrementingCounter(IncrementingCounter objectToCopy) {
		super(objectToCopy);
		this.lastEventMillis = objectToCopy.lastEventMillis;
		this.totalCounter = objectToCopy.totalCounter;
		this.deltaCounter = objectToCopy.deltaCounter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public int getDelta() {
		return this.deltaCounter;
	}

	public void resetAllOnForeignRemoval(long removalTimestamp) {
		this.totalCounter = 0;
		this.deltaCounter = 0;
		this.lastEventMillis = System.currentTimeMillis();
		this.minimumTimestampForForeignActions = removalTimestamp;
	}

	public void resetDelta() {
		this.deltaCounter = 0;
	}

	public void mergeWith(Counter objectToMergeValuesFrom) {
		if ((objectToMergeValuesFrom instanceof IncrementingCounter)) {
			IncrementingCounter counterToMergeValuesFrom = (IncrementingCounter) objectToMergeValuesFrom;
			if (counterToMergeValuesFrom.deltaCounter <= 0) return;
			if (counterToMergeValuesFrom.lastEventMillis <= this.minimumTimestampForForeignActions) return;
			if (isOveraged()) {
				this.totalCounter = counterToMergeValuesFrom.deltaCounter;
			} else {
				this.totalCounter += counterToMergeValuesFrom.deltaCounter;
			}
			this.lastEventMillis = Math.max(this.lastEventMillis, counterToMergeValuesFrom.lastEventMillis);
		}
	}


	public final void decrementQuietly() {
		if (this.totalCounter > 0) this.totalCounter -= 1;
		if (this.deltaCounter > 0) this.deltaCounter -= 1;
	}

	public final void increment() {
		if (isOveraged()) {
			this.totalCounter = 1;
			this.deltaCounter = 1;
		} else {
			this.totalCounter += 1;
			this.deltaCounter += 1;
		}
		this.lastEventMillis = System.currentTimeMillis();
	}

	public final boolean isOveraged() {
		return (this.lastEventMillis != 0L) && (this.lastEventMillis + getResetPeriodMillis() < System.currentTimeMillis());
	}

	public final int getCounter() {
		return this.totalCounter;
	}


	public final String toString() {
		return "counter:" + this.totalCounter + "(" + this.deltaCounter + ")" + getResetPeriodMillis();
	}
}


