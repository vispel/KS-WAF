package com.ks.trackers;

import com.ks.listener.SnapshotBroadcastListener;
import com.ks.pojo.IncrementingCounter;
import com.ks.pojo.Snapshot;

import java.util.Iterator;
import java.util.Map;

public final class ClusterSubscribeIncrementingCounterClient implements SnapshotBroadcastListener {
	private final String type;
	private final String systemIdentifier;
	private final Map map;

	public ClusterSubscribeIncrementingCounterClient(String type, String systemIdentifier, Map map) {
		if (type == null) throw new IllegalArgumentException("type must not be null");
		if (systemIdentifier == null) throw new IllegalArgumentException("systemIdentifier must not be null");
		if (map == null) throw new IllegalArgumentException("map must not be null");
		this.type = type;
		this.systemIdentifier = systemIdentifier;
		this.map = map;
	}

	public void handleSnapshotBroadcast(Snapshot snapshot) {
		if ((snapshot == null) || (snapshot.isEmpty()) || (!this.type.equals(snapshot.getType())) || (this.systemIdentifier.equals(snapshot.getSystemIdentifier())))
			return;
		Iterator iter;
		synchronized (this.map) {
			if (snapshot.hasPayload()) {
				for (iter = snapshot.getPayload().entrySet().iterator(); iter.hasNext(); ) {
					Map.Entry entry = (Map.Entry) iter.next();
					String ip = (String) entry.getKey();
					IncrementingCounter foreignCounter = (IncrementingCounter) entry.getValue();
					IncrementingCounter localCounter = (IncrementingCounter) this.map.get(ip);

					if (localCounter == null) {
						IncrementingCounter copy = new IncrementingCounter(foreignCounter);
						copy.resetDelta();
						this.map.put(ip, copy);
					} else {
						localCounter.mergeWith(foreignCounter);
					}
				}
			}
			if (snapshot.hasRemovals()) for (iter = snapshot.getRemovals().iterator(); iter.hasNext(); ) {
				String ip = (String) iter.next();

				IncrementingCounter counter = (IncrementingCounter) this.map.get(ip);
				if (counter != null) {
					counter.resetAllOnForeignRemoval(snapshot.getRemovalTimestamp());
				}
			}
		}
	}
}


