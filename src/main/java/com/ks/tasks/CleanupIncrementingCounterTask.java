 package com.ks.tasks;

 import com.ks.pojo.IncrementingCounter;

 import java.util.Iterator;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.TimerTask;

 public final class CleanupIncrementingCounterTask extends TimerTask
 {
   private final String name;
   private final Map map;

   public CleanupIncrementingCounterTask(String name, Map map)
   {
     if (name == null) throw new IllegalArgumentException("name must not be null");
     if (map == null) throw new IllegalArgumentException("map must not be null");
     this.name = name;
     this.map = map;
   }

   public void run() {
     if (this.map.isEmpty()) return;
     int loosers = 0;
     Iterator entries; synchronized (this.map) {
       for (entries = this.map.entrySet().iterator(); entries.hasNext();) {
         Entry entry = (Entry)entries.next();
         IncrementingCounter counter = (IncrementingCounter)entry.getValue();
         if ((counter != null) && (counter.isOveraged())) {
           entries.remove();
           loosers++;
         }
       }
     }
   }
 }

