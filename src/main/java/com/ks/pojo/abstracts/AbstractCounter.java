package com.ks.pojo.abstracts;

import com.ks.pojo.interfaces.Counter;

public abstract class AbstractCounter implements Counter {
	private volatile long resetPeriodMillis;

	public AbstractCounter(long resetPeriodMillis) {
		this.resetPeriodMillis = resetPeriodMillis;
	}


	public AbstractCounter(AbstractCounter objectToCopy) {
		if (objectToCopy == null) throw new IllegalArgumentException("objectToCopy must not be null");
		this.resetPeriodMillis = objectToCopy.resetPeriodMillis;
	}


	public long getResetPeriodMillis() {
		return this.resetPeriodMillis;
	}

	public void setResetPeriodMillis(long resetPeriodMillis) {
		this.resetPeriodMillis = resetPeriodMillis;
	}
}

