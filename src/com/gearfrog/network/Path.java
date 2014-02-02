package com.gearfrog.network;

import java.util.ArrayList;

public class Path {
	ArrayList<Link> linkChain;
	Node start;
	Node end;
	float time, speed;
	int length, value;
	int ownerID;
	
	public Path(ArrayList<Link> links, Node s, Node e, int val, int owner) {
		linkChain = links;
		start = s;
		end = e;
		value = val;
		ownerID = owner;
		time = 0;
		speed = 2;
		length = linkChain.size();
		
		float totTime = 0;
		for(Link l : linkChain) {
			totTime += 1f/l.getSpeed();
		}
		speed = 2*length/totTime;
		hold();
	}
	
	public boolean isFinished() {
		return time*speed >= length; 
	}
	
	public void hold() {
		for(Link l: linkChain) {
			l.inUse++;
		}
	}
	
	public void release() {
		end.gotData();
		for(Link l: linkChain) {
			l.inUse--;
		}
	}
	
	public float[] getLoc() {
		if(!isFinished()) {
			float[] loc = new float[2];
			float completion = time*speed;
			int n = (int) completion;
			completion -= n;
			Link l = linkChain.get(n);
			boolean forwards = true;
			if(n == 0) {
				if(l.node2 == start)
					forwards = false;
			} else {
				Link prev = linkChain.get(n-1);
				if(l.isLinkedRight(prev))
					forwards = false;
			}
				
			Node begin, end;
			if(forwards) {
				begin = l.node1;
				end = l.node2;
			} else {
				begin = l.node2;
				end = l.node1;
			}
			
			loc[0] = begin.x + (end.x - begin.x)*completion;
			loc[1] = begin.y + (end.y - begin.y)*completion;
			
			return loc;
		}
		return null;
	}
	
}
