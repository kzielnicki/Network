package com.gearfrog.network;


public class Node {
	public int ownerID;
	public Link down, right;
	public int x,y;
	
	private float fill = 0;
	private float speed = 0.2f;
	private int dataIn = 0;
	
	public Node(int o, int myX, int myY) {
		ownerID = o;
		x = myX;
		y = myY;
	}

	public void elapsed(float time) {
		fill += time*speed;
	}
	
	public boolean isFilled() {
		if(fill >= 1)
			return true;
		return false;
	}
	
	public float getFill() {
		if(fill > 1)
			return 1;
		return fill;
	}
	
	public void empty() {
		fill = 0;
	}
	
	public void gotData() {
		speed *= 1.01;
	}
}