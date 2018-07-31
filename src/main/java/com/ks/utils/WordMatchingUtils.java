package com.ks.utils;

import com.ks.pojo.AhoCorasickSearching;
import com.ks.pojo.Node;
import com.ks.pojo.Tree;
import com.ks.pojo.WordDictionary;

import java.util.*;


public final class WordMatchingUtils
{
  public static final boolean matchesWord(WordDictionary wordDictionary, String text, int trieMatchingThreshold)
  {
    if (wordDictionary == null) return true;
    if ((wordDictionary.size() == 0) || (text == null) || (text.length() == 0)) return false;
    String[] words = wordDictionary.getWords();
    int minLength = wordDictionary.getMinLength();
    if (text.length() < minLength) return false;
    text = text.toLowerCase();
    if ((trieMatchingThreshold >= 0) && (words.length >= trieMatchingThreshold))
    {
      Tree tree = wordDictionary.getTree();
      AhoCorasickSearching automat = new AhoCorasickSearching(tree);
      Node currentState = tree.getRootNode();
      List visitedStates = new ArrayList();
      int textLength = text.length();

      for (int j = 0; j < textLength; j++) {
        while (automat.transition(currentState, text.charAt(j)) == AhoCorasickSearching.EMPTY_SET_NODE) {
          currentState = automat.fail(currentState);
        }
        currentState = automat.transition(currentState, text.charAt(j));
        if (!visitedStates.contains(currentState)) {
          if (automat.isMatching(currentState)) {
            return true;
          }

          visitedStates.add(currentState);
        }
      }
    }
    else {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
    }
    return false;
  }






  public static int determineMinimumLength(String[] words)
  {
    if ((words == null) || (words.length == 0)) return 0;
    int minLength = Integer.MAX_VALUE;
      for (String word : words) {
          minLength = Math.min(minLength, word.length());
      }
    return minLength;
  }

  public static String[] deduplicate(String[] patterns) {
    if ((patterns == null) || (patterns.length == 0)) return patterns;
    Set searchWords = new HashSet(patterns.length);
    Collections.addAll(searchWords, patterns);
    return (String[])searchWords.toArray(new String[0]);
  }

  public static String[] trimLowercaseAndDeduplicate(String[] patterns) {
    if ((patterns == null) || (patterns.length == 0)) return patterns;
    Set searchWords = new HashSet(patterns.length);
      for (String pattern : patterns) {
          searchWords.add(pattern.trim().toLowerCase());
      }
    return (String[])searchWords.toArray(new String[0]);
  }

  public static String[] split(String commaOrWhitespaceSeparatedWords)
  {
    return commaOrWhitespaceSeparatedWords == null ? null : commaOrWhitespaceSeparatedWords.trim().split("\\s+|,");
  }
}


