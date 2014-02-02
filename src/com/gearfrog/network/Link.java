package com.gearfrog.network;


public class Link {
	public int ownerID;
	public Node node1;
	public Node node2;
	public boolean selected = false;
	public int inUse = 0;
	public int level;
	
	public static float cost(int lvl) {
		switch(lvl) {
		case 1: return 5;
		case 2: return 20;
		case 3: return 60;
		default: return 1;
		}
		
	}

	public Link(int o, Node n1, Node n2) {
		ownerID = o;
		node1 = n1;
		node2 = n2;
		level = 1;
	}
	
	public int getCapacity() {
		switch(level) {
		case 1: return 1;
		case 2: return 2;
		case 3: return 10;
		default: return 1;
		}
	}
	
	public float getSpeed() {
		switch(level) {
		case 1: return 1;
		case 2: return 1.5f;
		case 3: return 3f;
		default: return 1;
		}
	}
	
	public boolean isFull() {
		return inUse >= getCapacity();
	}
	
	public float useFraction() {
		return ((float)inUse) / getCapacity();
	}
	
	public boolean isLinkedLeft(Link l) {
		if(node1 == l.node1)
			return true;
		if(node1 == l.node2)
			return true;

		return false;
	}
	
	public boolean isLinkedRight(Link l) {
		if(node2 == l.node1)
			return true;
		if(node2 == l.node2)
			return true;

		return false;
	}
	
	public boolean isLinked(Link l) {
		return (isLinkedLeft(l) || isLinkedRight(l));
	}
	
	public boolean isBetween(Link l1, Link l2) {
		if(isLinkedLeft(l1) && isLinkedRight(l2))
			return true;
		if(isLinkedRight(l1) && isLinkedLeft(l2))
			return true;
		
		return false;
	}
}
