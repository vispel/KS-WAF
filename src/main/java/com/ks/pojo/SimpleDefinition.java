 package com.ks.pojo;

 import com.ks.pojo.abstracts.AbstractDefinition;

import java.util.regex.Pattern;


 public abstract class SimpleDefinition
   extends AbstractDefinition
 {
   private final WordDictionary servletPathOrRequestURIPrefilter;
   private final Pattern servletPathOrRequestURIPattern;

   public SimpleDefinition(boolean enabled, String identification, String description, WordDictionary servletPathOrRequestURIPrefilter, Pattern servletPathOrRequestURIPattern)
   {
     super(enabled, identification, description);
     this.servletPathOrRequestURIPrefilter = servletPathOrRequestURIPrefilter;
     if (servletPathOrRequestURIPattern == null) throw new IllegalArgumentException("servletPathOrRequestURIPattern must not be null");
     this.servletPathOrRequestURIPattern = servletPathOrRequestURIPattern;
   }


   public final WordDictionary getServletPathOrRequestURIPrefilter()
   {
     return this.servletPathOrRequestURIPrefilter;
   }

   public final Pattern getServletPathOrRequestURIPattern()
   {
     return this.servletPathOrRequestURIPattern;
   }
 }


