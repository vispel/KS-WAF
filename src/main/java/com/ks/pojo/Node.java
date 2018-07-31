package com.ks.pojo;

import java.io.Serializable;

public final class Node implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int EMPTY_CHAR = -1;
	private final Integer ID;
	private Node parent;
	private Node brother;
	private Node firstChild;

	public static Node createEmptyNode(Node parent, int id4Node, int id4FirstChild, int id4Brother) {
		Node node = new Node(id4Node);
		node.firstChild = new Node(new Integer(id4FirstChild));
		node.brother = new Node(new Integer(id4Brother));
		node.parent = parent;
		node.firstChild.parent = node;
		node.brother.parent = node.parent;
		return node;
	}

	public static Node createEmptyNode(Node parent, IdentifierSequence countingID) {
		return createEmptyNode(parent, countingID.nextValue(), countingID.nextValue(), countingID.nextValue());
	}


	private boolean marked = false;
	private int ch = -1;


	private Node(Integer ID) {
		this.ID = ID;
	}

	public Integer getID() {
		return this.ID;
	}


	public boolean isEmpty() {
		return this.ch == -1;
	}

	public char getChar() {
		return (char) this.ch;
	}

	public void setChar(char c) {
		this.ch = c;
	}


	public Node getParent() {
		return this.parent;
	}

	public void setParent(Node node) {
		this.parent = node;
	}


	public Node getBrother() {
		return this.brother;
	}

	public void setBrother(Node node) {
		this.brother = node;
	}


	public Node getFirstChild() {
		return this.firstChild;
	}

	public void setFirstChild(Node node) {
		this.firstChild = node;
	}


	public boolean isMarked() {
		return this.marked;
	}

	public void mark() {
		this.marked = true;
	}
}


