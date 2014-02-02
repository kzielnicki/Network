package com.gearfrog.network;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;


public class AIThread extends Thread {
	private static final int WAIT_INITIAL = 5000;
	private static final int WAIT_LOOP = 1000;
	
	NetworkView parent;
	float money = NetworkView.STARTING_MONEY;
	boolean die = false;
	
	public AIThread(NetworkView view) {
		parent = view;
	}
	
	private void makeBestMove() {
		int bestLength = Integer.MAX_VALUE;
		ArrayList<Link> bestPath = null;
		for(Node n: parent.activeNodes) {
			ArrayList<Link> curPath = findClosestNode(n, NetworkView.PLAYER_OPEN);
			if(curPath != null) {
				if(curPath.size() < bestLength) {
					bestPath = curPath;
					bestLength = curPath.size();
				}
			}
		}
		
		if(bestPath != null) {
			if(bestPath.size()*5 <= money) {
				money -= 5*bestPath.size();
				Log.v(this.getClass().getName(),"AI money = "+money);
				for(Link l : bestPath) {
					l.ownerID = NetworkView.PLAYER_COMPUTER;
				}
			}
		}
	}
	
	private ArrayList<Link> findClosestNode(Node start, int playerID) {
		Node[][] BFS = new Node[parent.nWidth][parent.nHeight];
		
		for(int i=0; i<parent.nWidth; ++i) {
			for(int j=0; j<parent.nHeight; ++j) {
				BFS[i][j] = null;
			}
		}
		
		Queue<Node> q = new LinkedList<Node>();
		q.offer(start);
		BFS[start.x][start.y] = start;
		
		boolean found = false;
		Node n = start;
		while(!q.isEmpty() && !found) {
			n = q.remove();
			if(n != start && n.ownerID == NetworkView.PLAYER_FIXED) {
				found = true;
				break;
			}
			
			Link l;
			ArrayList<Node> neighbors = parent.getNeighbors(n);
			for(Node next : neighbors) {
				l = parent.getLink(n, next);
				if(BFS[next.x][next.y] == null && l.ownerID == playerID && !l.selected) {
					BFS[next.x][next.y] = n;
					q.offer(next);
				}
			}
		}
		
		if(found) {
			ArrayList<Link> reverseChain = new ArrayList<Link>();
			while(n != start) {
				Node next = BFS[n.x][n.y];
				Link l = parent.getLink(n,next);
				reverseChain.add(l);
				n = next;
			}
			
			ArrayList<Link> chain = new ArrayList<Link>();
			for(int i=reverseChain.size()-1; i>=0; --i) {
				chain.add(reverseChain.get(i));
			}

			return chain;
		}
		
		return null;
	}

	public void setDie() {
		die = true;
		synchronized (this) {
			this.notify();
		}
	}
	
    @Override
    public void run() {
		synchronized (this) {
			while (parent.getState() < NetworkView.STATE_RUNNING && !die) {
				try {
					Log.v(this.getClass().getName(),"AI waiting");
					wait();
				} catch (Exception e) {
				}
			}
		}
		try {
			sleep(WAIT_INITIAL);
		} catch (Exception e) {
		}
		
        while (parent.getState() >= NetworkView.STATE_PAUSE && !die) {
			//Log.v(this.getClass().getName(),"AI loop");
        	makeBestMove();
			try {
				sleep(WAIT_LOOP);
			} catch (Exception e) {
			}
				
			
			synchronized (this) {
				while (parent.getState() < NetworkView.STATE_RUNNING && !die) {
					try {
						Log.v(this.getClass().getName(),"AI waiting");
						wait();
					} catch (Exception e) {
					}
				}
			}
        }
    }
}
