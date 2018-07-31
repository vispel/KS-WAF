 package com.ks.pojo;

 import com.ks.pojo.interfaces.CustomRequestMatcher;

 import java.util.regex.Pattern;

 public final class DenialOfServiceLimitDefinition extends RequestDefinition {
   private static final long serialVersionUID = 1L;
   private int watchPeriodMillis;
   private int clientDenialOfServiceLimit;

   public DenialOfServiceLimitDefinition(boolean enabled, String identification, String description, WordDictionary servletPathPrefilter, Pattern servletPathPattern, boolean servletPathPatternNegated) {
     super(enabled, identification, description, servletPathPrefilter, servletPathPattern, servletPathPatternNegated);
   }

   public DenialOfServiceLimitDefinition(boolean enabled, String identification, String description, CustomRequestMatcher customRequestMatcher) { super(enabled, identification, description, customRequestMatcher); }



   public int getWatchPeriodMillis()
   {
     return this.watchPeriodMillis;
   }

   public void setWatchPeriodSeconds(int seconds) { this.watchPeriodMillis = (seconds * 1000); }

   public int getClientDenialOfServiceLimit()
   {
     return this.clientDenialOfServiceLimit;
   }

   public void setClientDenialOfServiceLimit(int limit) { this.clientDenialOfServiceLimit = limit; }
 }

