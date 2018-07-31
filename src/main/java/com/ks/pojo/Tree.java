package com.ks.pojo;

import java.io.Serializable;

public final class Tree implements Serializable {
	private static final long serialVersionUID = 1L;
	private final Node rootNode;

	public static final Tree createTrie(String[] words) {
		IdentifierSequence countingID = new IdentifierSequence();
		Tree tree = new Tree(countingID);
        for (String word : words) {
            tree.addString(word, countingID);
        }
		return tree;
	}

	public Tree(IdentifierSequence countingID) {
		this.rootNode = Node.createEmptyNode(null, countingID);
	}

	public Node getRootNode() {
		return this.rootNode;
	}


	public void addString(String str, IdentifierSequence countingID) {
		addString(this.rootNode.getFirstChild(), str, str.length(), 0, countingID);
	}

	private void addString(Node ptrFirstChild, String str, int strLen, int i, IdentifierSequence countingID) {
		Node node;
		for (node = ptrFirstChild; !node.isEmpty(); node = node.getBrother()) {
			if (node.getChar() == str.charAt(i)) {
				if (i == strLen - 1) {
					node.mark();
					return;
				}

				addString(node.getFirstChild(), str, strLen, i + 1, countingID);
				return;
			}
		}

		node.setChar(str.charAt(i));
		node.setBrother(Node.createEmptyNode(node.getParent(), countingID));
		node.setFirstChild(Node.createEmptyNode(node, countingID));

		if (i == strLen - 1) {
			node.mark();
			return;
		}
		addString(node.getFirstChild(), str, strLen, i + 1, countingID);
	}
}


