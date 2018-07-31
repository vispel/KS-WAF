package com.ks.tasks;

import com.ks.pojo.IncrementingCounter;
import com.ks.pojo.Snapshot;
import com.ks.utils.JmsUtils;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class ClusterPublishIncrementingCounterTask extends java.util.TimerTask {
    private final String type;
    private final String systemIdentifier;
    private final String clusterInitialContextFactory;
    private final String clusterJmsProviderUrl;
    private final String clusterJmsConnectionFactory;
    private final String clusterJmsTopic;
    private final Map map;

    public ClusterPublishIncrementingCounterTask(String type, String systemIdentifier, String clusterInitialContextFactory, String clusterJmsProviderUrl, String clusterJmsConnectionFactory, String clusterJmsTopic, Map map) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (systemIdentifier == null) throw new IllegalArgumentException("systemIdentifier must not be null");
        if (map == null) throw new IllegalArgumentException("map must not be null");
        this.type = type;
        this.systemIdentifier = systemIdentifier;
        this.clusterInitialContextFactory = clusterInitialContextFactory;
        this.clusterJmsProviderUrl = clusterJmsProviderUrl;
        this.clusterJmsConnectionFactory = clusterJmsConnectionFactory;
        this.clusterJmsTopic = clusterJmsTopic;
        this.map = map;
    }

    public void run() {
        try {
            JmsUtils.init(this.clusterInitialContextFactory, this.clusterJmsProviderUrl, this.clusterJmsConnectionFactory, this.clusterJmsTopic);
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            JmsUtils.closeQuietly(false);
            System.err.println("Unable to init: " + e);
        } catch (RuntimeException e) {
            JmsUtils.closeQuietly(false);
            System.err.println("Unable to init: " + e);

            if (this.map.isEmpty()) {
                return;
            }
            Map payload = new HashMap(this.map.size());
            synchronized (this.map) {
                try {
                    for (Object o : this.map.entrySet()) {
                        Entry entry = (Entry) o;
                        IncrementingCounter counter = (IncrementingCounter) entry.getValue();
                        if ((counter.getDelta() > 0) && (!counter.isOveraged())) {
                            payload.put(entry.getKey(), counter.clone());

                            counter.resetDelta();
                        }
                    }
                } catch (CloneNotSupportedException ex) {
                    System.err.println("Unable to clone: " + ex);
                }
            }


            Snapshot snapshot = new Snapshot(this.type, this.systemIdentifier, payload);

            JmsUtils.publishSnapshot(snapshot);
        }
    }
}


