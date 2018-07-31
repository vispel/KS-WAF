package com.ks.pojo;

import com.ks.utils.WordMatchingUtils;

import java.io.Serializable;
import java.util.Collection;

public final class WordDictionary
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final String[] words;
  private final int minLength;
  private final Tree tree;

  public static final WordDictionary createInstance(String whitespaceOrCommaSeparatedWords)
  {
    if (whitespaceOrCommaSeparatedWords == null) return null;
    return new WordDictionary(whitespaceOrCommaSeparatedWords);
  }

  public WordDictionary(String[] wordsOriginalUntrimmed)
  {
    this.words = WordMatchingUtils.trimLowercaseAndDeduplicate(wordsOriginalUntrimmed);
    this.minLength = WordMatchingUtils.determineMinimumLength(this.words);
    this.tree = Tree.createTrie(this.words);
  }

  public WordDictionary(Collection wordsOriginalUntrimmed) { this((String[])wordsOriginalUntrimmed.toArray(new String[0])); }

  public WordDictionary(String whitespaceOrCommaSeparatedWords) {
    this(WordMatchingUtils.split(whitespaceOrCommaSeparatedWords));
  }

  public int getMinLength()
  {
    return this.minLength;
  }

  public String[] getWords()
  {
    return this.words;
  }

  public Tree getTree()
  {
    return this.tree;
  }

  public int size()
  {
    return this.words.length;
  }
}


