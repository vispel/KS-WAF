package com.ks.pojo;

import com.ks.pojo.interfaces.CustomRequestMatcher;

import java.util.regex.Pattern;

public final class BadRequestDefinition extends RequestDefinition {
  private static final long serialVersionUID = 1L;

  public BadRequestDefinition(boolean enabled, String identification, String description, WordDictionary servletPathPrefilter, Pattern servletPathPattern, boolean servletPathPatternNegated) {
    super(enabled, identification, description, servletPathPrefilter, servletPathPattern, servletPathPatternNegated);
  }

  public BadRequestDefinition(boolean enabled, String identification, String description, CustomRequestMatcher customRequestMatcher) { super(enabled, identification, description, customRequestMatcher); }
}

