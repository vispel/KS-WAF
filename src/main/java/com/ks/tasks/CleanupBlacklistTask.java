package com.ks.tasks;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;

public final class CleanupBlacklistTask extends TimerTask {
	private final String name;
	private final Map map;

	public CleanupBlacklistTask(String name, Map map) {
		if (name == null) throw new IllegalArgumentException("name must not be null");
		if (map == null) throw new IllegalArgumentException("map must not be null");
		this.name = name;
		this.map = map;
	}

	public void run() {
		int loosers = 0;
		long now;
		Iterator entries;
		synchronized (this.map) {
			now = System.currentTimeMillis();
			for (entries = this.map.entrySet().iterator(); entries.hasNext(); ) {
				Entry entry = (Entry) entries.next();
				Long value = (Long) entry.getValue();
				if ((value != null) && (value < now)) {
					entries.remove();
					loosers++;
				}
			}
		}
	}
}


