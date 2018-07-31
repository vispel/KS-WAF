package com.ks.pojo;

public final class IdentifierSequence
{
  private int counter = 0;

  public int nextValue() {
    return this.counter++;
  }
}

