package com.ks.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AhoCorasickSearching {
	public static final Node EMPTY_SET_NODE = Node.createEmptyNode(null, -3, -2, -1);

	private final Tree tree;

	private final Map failureCache = new HashMap();
	private final Map transitionCache = new HashMap();
	private final List retrievalCacheNegatives = new ArrayList();


	public AhoCorasickSearching(Tree trie) {
		this.tree = trie;
	}


	public Node fail(Node node) {
		if (node == this.tree.getRootNode()) return node;
		if (node.getParent() == this.tree.getRootNode()) {
			return node.getParent();
		}
		Node result = (Node) this.failureCache.get(node.getID());
		if (result != null) {
			return result;
		}
		if (node == this.tree.getRootNode()) {
			this.failureCache.put(node.getID(), node);
			return node;
		}

		if (node.getParent() == this.tree.getRootNode()) {
			this.failureCache.put(node.getID(), node.getParent());
			return node.getParent();
		}

		char character = node.getChar();
		Node test = fail(node.getParent());
		while (transition(test, character) == EMPTY_SET_NODE) {
			test = fail(test);
		}

		result = transition(test, character);
		this.failureCache.put(node.getID(), result);
		return result;
	}


	public Node transition(Node node, char character) {
		Integer combinedKeyCharacterAndID = node.getID().intValue() * 1024 + character;

		Node cachedResult = (Node) this.transitionCache.get(combinedKeyCharacterAndID);
		if (cachedResult != null) {
			return cachedResult;
		}
		Node test;
		for (test = node.getFirstChild(); !test.isEmpty(); test = test.getBrother()) {
			if (test.getChar() == character) {
				this.transitionCache.put(combinedKeyCharacterAndID, test);
				return test;
			}
		}

		if (node == this.tree.getRootNode()) {
			this.transitionCache.put(combinedKeyCharacterAndID, node);
			return node;
		}
		this.transitionCache.put(combinedKeyCharacterAndID, EMPTY_SET_NODE);
		return EMPTY_SET_NODE;
	}


	public boolean isMatching(Node node) {
		if (this.retrievalCacheNegatives.contains(node.getID())) {
			return false;
		}

		if (node == this.tree.getRootNode()) {
			this.retrievalCacheNegatives.add(node.getID());
			return false;
		}

		Node nodeFail = fail(node);
		if ((nodeFail != node) && (isMatching(nodeFail))) {
			return true;
		}
		if (node.isMarked()) {
			return true;
		}
		this.retrievalCacheNegatives.add(node.getID());
		return false;
	}
}

