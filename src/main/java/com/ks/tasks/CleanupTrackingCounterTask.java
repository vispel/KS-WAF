package com.ks.tasks;

import com.ks.trackers.TrackingCounter;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class CleanupTrackingCounterTask extends java.util.TimerTask {
	private final String name;
	private final Map map;

	public CleanupTrackingCounterTask(String name, Map map) {
		if (name == null) throw new IllegalArgumentException("name must not be null");
		if (map == null) throw new IllegalArgumentException("map must not be null");
		this.name = name;
		this.map = map;
	}

	public void run() {
		int loosers = 0;
		Iterator definitions;
		synchronized (this.map) {
			for (definitions = this.map.entrySet().iterator(); definitions.hasNext(); ) {
				Entry definition = (Entry) definitions.next();
				Map client2CounterMap = (Map) definition.getValue();

				Iterator entries;
				if (!client2CounterMap.isEmpty()) {
					for (entries = client2CounterMap.entrySet().iterator(); entries.hasNext(); ) {
						Entry entry = (Entry) entries.next();
						TrackingCounter counter = (TrackingCounter) entry.getValue();
						if ((counter != null) && (counter.isOveraged())) {
							entries.remove();
							loosers++;
						}
					}
				}

				if (client2CounterMap.isEmpty()) definitions.remove();
			}
		}
	}
}


