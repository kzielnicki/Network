package com.gearfrog.network;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NetworkView extends View {

	/*
	 * State-tracking constants
	 */
	public static final int STATE_LOSE = 1;
	public static final int STATE_WIN = 2;
	public static final int STATE_PAUSE = 3;
	public static final int STATE_READY = 4;
	// states above this level are actively running
	public static final int STATE_RUNNING = 5;
	public static final int STATE_BUY_LINKS = 6;
	public static final int STATE_BUY_NODES = 7;

	/*
	 * Keys for saved values 
	 */
	public static final String KEY_POINTS = "pts";
	public static final String KEY_PLAYERS = "players";
	public static final String KEY_NODES = "nodes";
	public static final String KEY_ACTIVE = "active";

	/*
	 * Player ID
	 */
	public static final int PLAYER_CLOSED = 0;
	public static final int PLAYER_OPEN = 1;
	public static final int PLAYER_FIXED = 2;
	public static final int PLAYER_HUMAN = 3;
	public static final int PLAYER_COMPUTER = 4;

	public static final int EDGE_LENGTH_DEFAULT = 100;
	public static final int PADDING = 30;
	public static final int STARTING_MONEY = 50;

	public static final Random rand = new Random();

	/*
	 * Member (state) fields
	 */
	/** Pointer to the text view to display "Paused.." etc. */
	private TextView mStatusText;
	private TextView mMoneyText;
	private TextView mScoreTextHuman;
	private TextView mScoreTextComp;
	private MediaPlayer mMusic;

	/** The drawable to use as the background of the animation canvas */
	private Bitmap mBackgroundImage;
	private ToggleImageButton linkButton;
	private LevelSelectButton levelButton;

	/**
	 * Current height of the surface/canvas.
	 * 
	 * @see #setSurfaceSize
	 */
	private int mCanvasHeight = 1;

	/**
	 * Current width of the surface/canvas.
	 * 
	 * @see #setSurfaceSize
	 */
	private int mCanvasWidth = 1;

	// gesture detection
	private GestureDetector mGestureDetector;

	/** Message handler used by thread to interact with TextView */
	//private Handler mHandler;

	/** Used to figure out elapsed time between frames */
	private long mLastTime;

	/** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
	private int mMode;

	/** Handle to the surface manager object we interact with */
	//private SurfaceHolder mSurfaceHolder;

	AIThread AI;

	// offset for scrolling
	private int xOffset = 30;
	private int yOffset = 30;

	// playing field dimensions
	int nWidth = 15;
	int nHeight = 20;
	private int edgeLength = EDGE_LENGTH_DEFAULT;
	private boolean zoomed = false;

	// collection of nodes
	// note that array indices in this program are in [x,y] order not [r,c] order
	Node[][] nodes = new Node[nWidth][nHeight];
	int nodeColor = Color.MAGENTA;
	ArrayList<Node> activeNodes = new ArrayList<Node>();
	private ArrayList<Path> activePaths = new ArrayList<Path>();

	private ArrayList<Player> players = new ArrayList<Player>();

	private float money;
	private NumberFormat moneyFormat = NumberFormat.getInstance();

	private int[] score = {0, 0};
	private int winScore = 500;

	// last touch screen location
	private PointF lastPoint;

	// current chain of links being selected
	private ArrayList<Link> linkChain;

	private Queue<Node> fullNodes;

	/**
	 * Create a simple handler that we can use to cause animation to happen.  We
	 * set ourselves as a target and we can use the sleep()
	 * function to cause an update/invalidate to occur at a later date.
	 */
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			//Log.v(this.getClass().getName(),"msg?...");
			NetworkView.this.update();
			NetworkView.this.invalidate();
		}

		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};


	/**
	 * Constructs a NetworkView based on inflation from XML
	 * 
	 * @param context
	 * @param attrs
	 */
	public NetworkView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initNetworkView();
	}

	public NetworkView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initNetworkView();
	}

	private void initNetworkView() {
		//mSurfaceHolder = getHolder();

		mGestureDetector = new GestureDetector(new NetworkGestures());

		setFocusable(true);

		Resources res = getContext().getResources();

		// load background image as a Bitmap instead of a Drawable b/c
		// we don't need to transform it and it's faster to draw this way
		//		mBackgroundImage = Bitmap.createScaledBitmap(
		//				BitmapFactory.decodeResource(res, R.drawable.earthrise), mCanvasWidth, mCanvasHeight, true);

		// initialize playing field
		players = new ArrayList<Player>();
		players.add(new Player("closed", Color.TRANSPARENT));
		players.add(new Player("open", ColorHelper.setAlpha(Color.WHITE,100)));
		players.add(new Player("fixed", Color.RED));
		players.add(new Player("human", Color.CYAN));
		players.add(new Player("computer", Color.YELLOW));
		for(int i=0; i<nWidth; i++) {
			for(int j=0; j<nHeight; j++) {
				Node n = new Node(PLAYER_CLOSED, i, j);
				nodes[i][j] = n;
			}
		}
		for(int i=0; i<nWidth; i++) {
			for(int j=0; j<nHeight; j++) {
				Node n = nodes[i][j];
				if(j<nHeight-1) {
					n.down = new Link(PLAYER_OPEN, n, nodes[i][j+1]);
				}
				if(i<nWidth-1) {
					n.right = new Link(PLAYER_OPEN, n, nodes[i+1][j]);
				}
			}
		}

		activeNodes = new ArrayList<Node>();
		int toPlace = 15;
		while(toPlace > 0) {
			int x = rand.nextInt(nWidth);
			int y = rand.nextInt(nHeight);
			Node n = nodes[x][y];
			if(n.ownerID != PLAYER_FIXED) {
				n.ownerID = PLAYER_FIXED;
				activeNodes.add(n);
				toPlace--;
			}
		}

		//		nWidth = 8;
		//		nHeight = 10;
		//		ArrayList<Node> temp = new ArrayList<Node>();
		//		temp.add(nodes[0][0]);
		//		temp.add(nodes[7][0]);
		//		temp.add(nodes[1][5]);
		//		temp.add(nodes[6][5]);
		//		temp.add(nodes[2][3]);
		//		temp.add(nodes[5][3]);
		//		for(Node n: temp) {
		//			n.ownerID = PLAYER_FIXED;
		//			activeNodes.add(n);
		//			Node mirror = nodes[n.x][nHeight-n.y-1];
		//			mirror.ownerID = PLAYER_FIXED;
		//			activeNodes.add(mirror);
		//		}

		zoomed = false;
		edgeLength = EDGE_LENGTH_DEFAULT;
		money = STARTING_MONEY;
		score = new int[2];
		score[0] = 0;
		score[1] = 0;

		activePaths = new ArrayList<Path>();
		fullNodes = new LinkedList<Node>();

		mMode = STATE_READY;
		mLastTime = System.currentTimeMillis() + 100;
		AI = new AIThread(this);
		AI.start();
	}


	/**
	 * Starts the game, setting parameters for the current difficulty.
	 */
	public void doStart() {
		//synchronized (mSurfaceHolder) {
		AI.setDie();
		initNetworkView();
		setState(STATE_RUNNING);
		mMusic.start();
		mMusic.seekTo(0);
		levelButton.setLevel(1);
		synchronized(AI) {
			AI.notify();
		}
		//}
	}

	/**
	 * Pauses the physics update & animation.
	 */
	public void pause() {
		//synchronized (mSurfaceHolder) {
		if (mMode >= STATE_RUNNING) {
			setState(STATE_PAUSE);
			linkButton.setChecked(false);
		}
		mMusic.pause();
		//}
	}

	/**
	 * Resumes from a pause.
	 */
	public void unpause() {
		// make sure we're actually paused
		if(mMode == STATE_PAUSE || mMode == STATE_READY) {
			// Move the real time clock up to now
			//synchronized (mSurfaceHolder) {
			mLastTime = System.currentTimeMillis() + 100;
			//}
			setState(STATE_RUNNING);
			mMusic.start();
			synchronized(AI) {
				AI.notify();
			}
		}
	}

	/**
	 * Sets the current difficulty.
	 * 
	 * @param difficulty
	 */
	public void setDifficulty(int difficulty) {
		//synchronized (mSurfaceHolder) {
		//mDifficulty = difficulty;
		//}
	}

	/**
	 * Sets the game mode. That is, whether we are running, paused, in the
	 * failure state, in the victory state, etc.
	 * 
	 * @see #setState(int, CharSequence)
	 * @param mode one of the STATE_* constants
	 */
	public void setState(int mode) {
		//synchronized (mSurfaceHolder) {
		setState(mode, null);
		//}
	}

	public int getState() {
		return mMode;
	}

	/**
	 * Sets the game mode. That is, whether we are running, paused, in the
	 * failure state, in the victory state, etc.
	 * 
	 * @param mode one of the STATE_* constants
	 * @param message string to add to screen or null
	 */
	public void setState(int mode, CharSequence message) {
		/*
		 * This method optionally can cause a text message to be displayed
		 * to the user when the mode changes. Since the View that actually
		 * renders that text is part of the main View hierarchy and not
		 * owned by this thread, we can't touch the state of that View.
		 * Instead we use a Message + Handler to relay commands to the main
		 * thread, which updates the user-text View.
		 */
		//synchronized (mSurfaceHolder) {
		if(mMode == STATE_BUY_LINKS && mode != STATE_BUY_LINKS)
			linkButton.setChecked(false);
		mMode = mode;

		if (mMode >= STATE_RUNNING) {
			mStatusText.setVisibility(View.INVISIBLE);
		} else {
			Resources res = getContext().getResources();
			CharSequence str = "";
			if (mMode == STATE_READY)
				str = res.getText(R.string.mode_ready);
			else if (mMode == STATE_PAUSE)
				str = res.getText(R.string.mode_pause);
			else if (mMode == STATE_LOSE)
				str = res.getText(R.string.mode_lose);
			else if (mMode == STATE_WIN)
				str = res.getString(R.string.mode_win_prefix) + " "
				+ res.getString(R.string.mode_win_suffix);

			if (message != null) {
				str = message + "\n" + str;
			}

			mStatusText.setVisibility(View.VISIBLE);
			mStatusText.setText(str);
		}

		update();
		//}
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		//synchronized (mSurfaceHolder) {
		mCanvasWidth = width;
		mCanvasHeight = height;

		// don't forget to resize the background image
		mBackgroundImage = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
				getContext().getResources(), R.drawable.fiber_small), mCanvasWidth, mCanvasHeight, true);
		//}
	}

	//	/**
	//	 * Standard override to get key-press events.
	//	 */
	//	@Override
	//	public boolean onKeyDown(int keyCode, KeyEvent msg) {
	//		//synchronized (mSurfaceHolder) {
	//		boolean okStart = false;
	//		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) okStart = true;
	//		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true;
	//		if (keyCode == KeyEvent.KEYCODE_S) okStart = true;
	//
	//		boolean center = (keyCode == KeyEvent.KEYCODE_DPAD_UP);
	//
	//		if (okStart
	//				&& (mMode == STATE_READY || mMode == STATE_LOSE || mMode == STATE_WIN)) {
	//			// ready-to-start -> start
	//			doStart();
	//			return true;
	//		} else if (mMode == STATE_PAUSE && okStart) {
	//			// paused -> running
	//			unpause();
	//			return true;
	//		} else if (mMode == STATE_RUNNING) {
	//			// center/space -> fire
	//			if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
	//					|| keyCode == KeyEvent.KEYCODE_SPACE) {
	//				return true;
	//				// left/q -> left
	//			} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
	//					|| keyCode == KeyEvent.KEYCODE_Q) {
	//				return true;
	//				// right/w -> right
	//			} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
	//					|| keyCode == KeyEvent.KEYCODE_W) {
	//				return true;
	//				// up -> pause
	//			} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
	//				return true;
	//			}
	//		}
	//
	//		return false;
	//		//}
	//	}
	//
	//	/**
	//	 * Standard override for key-up. We actually care about these, so we can
	//	 * turn off the engine or stop rotating.
	//	 */
	//	@Override
	//	public boolean onKeyUp(int keyCode, KeyEvent msg) {
	//		boolean handled = false;
	//
	//		//synchronized (mSurfaceHolder) {
	//		if (mMode == STATE_RUNNING) {
	//			if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
	//					|| keyCode == KeyEvent.KEYCODE_SPACE) {
	//				handled = true;
	//			} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
	//					|| keyCode == KeyEvent.KEYCODE_Q
	//					|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
	//					|| keyCode == KeyEvent.KEYCODE_W) {
	//				handled = true;
	//			}
	//		}
	//		//}
	//
	//		return handled;
	//	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		//synchronized (mSurfaceHolder) {
		if (mMode == STATE_BUY_NODES) {
			if(me.getAction() == MotionEvent.ACTION_DOWN) {
				Node n = touchedNode(me.getX(), me.getY());
				if(n != null) {
					if(n.ownerID == PLAYER_OPEN) {
						n.ownerID = PLAYER_HUMAN;
						setState(STATE_RUNNING);
						return false;
					}
				}
			}
			return true;
		} else if(mMode == STATE_BUY_LINKS) {
			if(me.getAction() == MotionEvent.ACTION_DOWN) {
				lastPoint = new PointF(me.getX(),me.getY());
				linkChain = new ArrayList<Link>();
			} else if(me.getAction() == MotionEvent.ACTION_MOVE) {
				Link l = touchedLink(me.getX(), me.getY());
				if(l != null) {
					if(isBuyableLink(l)) {
						l.selected = true;
						linkChain.add(l);
						cleanLinkChain();
						update();
					}
				}
				lastPoint = new PointF(me.getX(),me.getY());
			} else if(me.getAction() == MotionEvent.ACTION_UP) {
				for(Link l : linkChain) {
					l.selected = false;
					float cost = Link.cost(levelButton.getLevel());
					if(money >= cost && !(l.ownerID == PLAYER_HUMAN && levelButton.getLevel() <= l.level)) {
						l.ownerID = PLAYER_HUMAN;
						l.level = levelButton.getLevel();
						money -= cost;
					}
				}
				setState(STATE_RUNNING);
			}
			return true;
		} else if(mMode == STATE_RUNNING) {
			boolean temp = mGestureDetector.onTouchEvent(me);
			//Log.v(this.getClass().getName(),"gestured:"+temp);
			return true;
		} else if(mMode < STATE_PAUSE) {
			//			AI.stop();
			//			initNetworkView();
			//			unpause();
			return false;
		} else if(mMode < STATE_RUNNING) {
			unpause();
			return false;
		}

		return false;
	}


	private void drawDotted(Canvas canvas, int x0, int y0, int x1, int y1, Paint paint, int color1, int color2, float fill) {
		paint.setColor(color1);
		float xm1 = (x1-x0)*fill*0.5f + x0;
		float ym1 = (y1-y0)*fill*0.5f + y0;
		float xm2 = (x1-x0)*(1-fill) + xm1;
		float ym2 = (y1-y0)*(1-fill) + ym1;
		canvas.drawLine(x0, y0, xm1, ym1, paint);
		canvas.drawLine(xm2, ym2, x1, y1, paint);

		paint.setColor(color2);
		canvas.drawLine(xm1,ym1,xm2, ym2, paint);
	}

	//	private void drawThick(Canvas canvas, int x0, int y0, int x1, int y1, Paint paint, int thickness) {
	//		canvas.drawLine(x0, y0, x1, y1, paint);
	//	}

	/**
	 * Draws to the provided Canvas.
	 */
	@Override
	public void onDraw(Canvas canvas) {
		//Log.v(this.getClass().getName(),"drawing...");
		// Draw the background image. Operations on the Canvas accumulate
		// so this is like clearing the screen.
		//Log.v(this.getClass().getName(),"drawing...");
		canvas.drawBitmap(mBackgroundImage, 0, 0, null);


		RectF tmpRect = new RectF();
		Paint color = new Paint();
		color.setAntiAlias(true);
		color.setARGB(200, 255, 0, 0);


		for(int i=0; i<nWidth; ++i) {
			int x0 = xOffset + (i)*edgeLength;
			int x1 = xOffset + (i+1)*edgeLength;
			for(int j=0; j<nHeight; ++j) {
				// draw links
				int y0 = yOffset + (j)*edgeLength;
				int y1 = yOffset + (j+1)*edgeLength;
				Node n = nodes[i][j];
				if(n.right != null) {
					color.setStrokeWidth((float) Math.pow(n.right.level,2));
					if(n.right.selected) {
						color.setColor(Color.GREEN);
						canvas.drawLine(x0, y0, x1, y0, color);
					} else if(n.right.inUse > 0) {
						color.setColor(ColorHelper.interpolate(players.get(n.right.ownerID).color, nodeColor, 0.4f*n.right.useFraction()));
						//color.setColor(ColorHelper.setAlpha(players.get(n.right.ownerID).color, 150));
						//drawDotted(canvas, x0, y0, x1, y0, color, nodeColor, players.get(n.right.ownerID).color, 0.8f);
						canvas.drawLine(x0, y0, x1, y0, color);
						//color.setColor(nodeColor);
						//canvas.drawLine(x0, y0+2, x1, y0+2, color);
						//canvas.drawLine(x0, y0-2, x1, y0-2, color);
					} else {
						color.setColor(players.get(n.right.ownerID).color);
						canvas.drawLine(x0, y0, x1, y0, color);
					}
					color.setStrokeWidth(1);
				}
				if(n.down != null) {
					color.setStrokeWidth((float) Math.pow(n.down.level,2));
					if(n.down.selected) {
						color.setColor(Color.GREEN);
						canvas.drawLine(x0, y0, x0, y1, color);
					} else if(n.down.inUse > 0) {
						color.setColor(ColorHelper.interpolate(players.get(n.down.ownerID).color, nodeColor, 0.4f*n.down.useFraction()));
						//color.setColor(ColorHelper.setAlpha(players.get(n.down.ownerID).color, 150));
						//drawDotted(canvas, x0, y0, x0, y1, color, nodeColor, players.get(n.down.ownerID).color, 0.8f);
						canvas.drawLine(x0, y0, x0, y1, color);
						//color.setColor(nodeColor);
						//canvas.drawLine(x0+2, y0, x0+2, y1, color);
						//canvas.drawLine(x0-2, y0, x0-2, y1, color);
					} else {
						color.setColor(players.get(n.down.ownerID).color);
						canvas.drawLine(x0, y0, x0, y1, color);
					}
					color.setStrokeWidth(1);
				}

				// draw node
				if(!zoomed) {
					tmpRect.top = y0-15;
					tmpRect.bottom = y0+15;
					tmpRect.left = x0-15;
					tmpRect.right = x0+15;
					color.setColor(nodeColor);
					canvas.drawArc(tmpRect, 0, 360*n.getFill(), true, color);
					color.setColor(players.get(n.ownerID).color);
					canvas.drawCircle(x0, y0, 10, color);
				} else {
					color.setColor(players.get(n.ownerID).color);
					canvas.drawCircle(x0, y0, 5, color);
				}
			}
		}

		if(!zoomed) {
			for(Path p : activePaths) {
				if(!p.isFinished()) {
					//Log.v(this.getClass().getName(),"trying to animate...");
					float[] loc = p.getLoc();
					loc[0] = xOffset + loc[0]*edgeLength;
					loc[1] = yOffset + loc[1]*edgeLength;
					color.setColor(nodeColor);
					canvas.drawCircle(loc[0], loc[1], 5, color);
				}
			}
		}

	}

	/**
	 * Handles the basic update loop, checking to see if we are in the running
	 * state, determining if a move should be made, updating the snake's location.
	 */
	public void update() {
		//TODO: might need to make synchronized?

		if (mMode >= STATE_RUNNING) {
			long now = System.currentTimeMillis();
			double elapsed = (now - mLastTime) / 1000.0;

			for(Node n : activeNodes) {
				boolean wasFilled = n.isFilled();
				n.elapsed((float)elapsed);
				if(n.isFilled() && !wasFilled)
					fullNodes.offer(n);
			}

			//TODO: maybe get rid of some of the temp. objects create to help GC out
			if(fullNodes.size() > 0) {	
				Node start = fullNodes.remove();
				start.empty();
				Node end;
				do {
					end = activeNodes.get(rand.nextInt(activeNodes.size()));
				} while(start == end);

				ArrayList<Node> humanNodes = findConnectedNodes(start, PLAYER_HUMAN);
				ArrayList<Node> compNodes = findConnectedNodes(start, PLAYER_COMPUTER);
				if(humanNodes.size() + compNodes.size() > 0) {
					int chosen = rand.nextInt(humanNodes.size() + compNodes.size());
					int player;
					int value;
					if(chosen < humanNodes.size()) {
						end = humanNodes.get(chosen);
						value = humanNodes.size();
						player = PLAYER_HUMAN;
					} else {
						end = compNodes.get(chosen - humanNodes.size());
						value = compNodes.size();
						player = PLAYER_COMPUTER;
					}

					ArrayList<Link> chain = findShortestPath(start, end, player);
					if(chain != null) {
						activePaths.add(new Path(chain, start, end, value, player));
					}
				}
			}

			ArrayList<Path> newPaths = new ArrayList<Path>();
			for(Path p : activePaths) {
				p.time += elapsed;
				if(p.isFinished()) {
					p.release();
					if(p.ownerID == PLAYER_HUMAN) {
						money += 0.3*p.value*Math.sqrt(p.length);
						score[0]++;
						if(score[0] >= winScore) {
							setState(STATE_WIN);
						}
					} else {
						AI.money += 0.3*p.value*Math.sqrt(p.length);
						score[1]++;
						if(score[1] >= winScore) {
							setState(STATE_LOSE);
						}
					}
				} else {
					newPaths.add(p);
				}
			}
			activePaths = newPaths;

			mLastTime = now;
			mRedrawHandler.sleep(50);
			//Log.v(this.getClass().getName(),"in update...");
		}

		//NumberFormat formatter = new DecimalFormat("#0");

		// Print the number using our defined decimal format pattern as above.
		//System.out.println(formatter.format(money));

		mMoneyText.setText("$"+moneyFormat.format((int)(money*1000)));
		mScoreTextHuman.setText(Integer.toString(score[0]));
		mScoreTextComp.setText(Integer.toString(score[1]));
		invalidate();

	}


	private Node touchedNode(float x, float y) {
		x-=xOffset;
		y-=yOffset;
		float nXf = x/edgeLength;
		float nYf = y/edgeLength;
		int nX = Math.round(nXf);
		int nY = Math.round(nYf);
		nXf -= nX;
		nYf -= nY;

		//Log.v(this.getClass().getName(),"x:"+nX+"+"+nXf+" y:"+nY+"+"+nYf);

		if(nX >= 0 && nY >= 0 && nX < nWidth && nY < nHeight) {
			Node n = nodes[nX][nY];
			if(nXf < 0.2 && nXf > -0.2 && nYf < 0.2 && nYf > -0.2) {
				return n;
			}		
		}

		return null;
	}

	private Link touchedLink(float x, float y) {
		float dX = Math.abs(x-lastPoint.x);
		float dY = Math.abs(y-lastPoint.y);

		x-=xOffset;
		y-=yOffset;
		float nXf = x/edgeLength;
		float nYf = y/edgeLength;
		int nX = (int)nXf;
		int nY = (int)nYf;
		nXf -= nX;
		nYf -= nY;

		//Log.v(this.getClass().getName(),"x:"+nX+"+"+nXf+" y:"+nY+"+"+nYf);	

		if(nX >= 0 && nY >= 0 && nX < nWidth && nY < nHeight) {
			//Log.v(this.getClass().getName(),"claiming...");
			Node n = nodes[nX][nY];
			if(nXf > 0.4 && nXf < 0.8 && dX > dY) {
				if(nYf < 0.4)
					return n.right;
				else if(nYf > 0.8 && nY < nHeight-1)
					return nodes[nX][nY+1].right;
			} else if(nYf > 0.4 && nYf < 0.8 && dY > dX) {
				if (nXf < 0.4)
					return n.down;
				else if(nXf > 0.8 && nX < nWidth-1)
					return nodes[nX+1][nY].down;
			}		
		}

		return null;
	}

	//	private Node getLeftNode(Link l) {
	//		return nodes[rc(l.node1[0], l.node1[1])];
	//	}
	//	
	//	private Node getRightNode(Link l) {
	//		return nodes[rc(l.node2[0], l.node2[1])];
	//	}

	public Link getLink(Node n1, Node n2) {
		if(n1.x < n2.x)
			return n1.right;
		if(n2.x < n1.x)
			return n2.right;
		if(n1.y < n2.y)
			return n1.down;
		if(n2.y < n1.y)
			return n2.down;

		return null;
	}

	public ArrayList<Node> getNeighbors(Node n) {
		ArrayList<Node> neighbors = new ArrayList<Node>();

		if(n.x > 0) {
			neighbors.add(nodes[n.x-1][n.y]);
		}
		if(n.y > 0) {
			neighbors.add(nodes[n.x][n.y-1]);
		}
		if(n.x < nWidth-1) {
			neighbors.add(nodes[n.x+1][n.y]);
		}
		if(n.y < nHeight-1) {
			neighbors.add(nodes[n.x][n.y+1]);
		}

		return neighbors;
	}

	private ArrayList<Node> findConnectedNodes(Node start, int playerID) {
		ArrayList<Node> found = new ArrayList<Node>();
		boolean[][] BFS = new boolean[nWidth][nHeight];

		for(int i=0; i<nWidth; ++i) {
			for(int j=0; j<nHeight; ++j) {
				BFS[i][j] = false;
			}
		}

		Queue<Node> q = new LinkedList<Node>();
		q.offer(start);
		BFS[start.x][start.y] = true;

		Node n;
		while(!q.isEmpty()) {
			n = q.remove();
			if(n != start && n.ownerID == PLAYER_FIXED)
				found.add(n);

			Link l;
			ArrayList<Node> neighbors = getNeighbors(n);
			for(Node next : neighbors) {
				l = getLink(n, next);
				if(BFS[next.x][next.y] == false && l.ownerID == playerID && !l.isFull()) {
					BFS[next.x][next.y] = true;
					q.offer(next);
				}
			}
		}

		return found;
	}

	private ArrayList<Link> findShortestPath(Node start, Node end, int playerID) {
		Node[][] BFS = new Node[nWidth][nHeight];

		for(int i=0; i<nWidth; ++i) {
			for(int j=0; j<nHeight; ++j) {
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
			if(n == end) {
				found = true;
				break;
			}

			Link l;
			ArrayList<Node> neighbors = getNeighbors(n);
			for(Node next : neighbors) {
				l = getLink(n, next);
				if(BFS[next.x][next.y] == null && l.ownerID == playerID && !l.isFull()) {
					BFS[next.x][next.y] = n;
					q.offer(next);
				}
			}
		}

		if(found) {
			ArrayList<Link> reverseChain = new ArrayList<Link>();
			while(n != start) {
				Node next = BFS[n.x][n.y];
				Link l = getLink(n,next);
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

	private boolean isBuyableLink(Link l) {
		if(l.selected)
			return false;
		if(l.ownerID == PLAYER_HUMAN || l.ownerID == PLAYER_OPEN)
			return true;
		if(l.ownerID == PLAYER_COMPUTER && l.level < levelButton.getLevel())
			return true;
		return false;
	}

	private boolean makeShortestConnection(ArrayList<Link> newChain, Link last) {
		Node[][] BFS = new Node[nWidth][nHeight];

		for(int i=0; i<nWidth; ++i) {
			for(int j=0; j<nHeight; ++j) {
				BFS[i][j] = null;
			}
		}

		Queue<Node> q = new LinkedList<Node>();
		Node n1 = nodes[last.node1.x][last.node1.y];
		Node n2 = nodes[last.node2.x][last.node2.y];
		q.offer(n1);
		BFS[last.node1.x][last.node1.y] = n1;
		q.offer(n2);
		BFS[last.node2.x][last.node2.y] = n2;

		boolean found = false;
		Node n = n1;
		while(!q.isEmpty() && !found) {
			n = q.remove();

			Link l;
			ArrayList<Node> neighbors = getNeighbors(n);
			for(Node next : neighbors) {
				l = getLink(n, next);
				if(l.selected && n != n1 && n != n2) {
					found = true;
				} else if(BFS[next.x][next.y] == null && isBuyableLink(l)) {
					BFS[next.x][next.y] = n;
					q.offer(next);
				}
			}
		}

		if(found) {
			//ArrayList<Link> reverseChain = new ArrayList<Link>();
			while(n != n1 && n != n2) {
				Node next = BFS[n.x][n.y];
				Link l = getLink(n,next);
				l.selected = true;
				newChain.add(l);
				n = next;
			}

			//			for(int i=reverseChain.size()-1; i>=0; --i) {
			//				newChain.add(reverseChain.get(i));
			//			}
			//			Log.v(this.getClass().getName(),"added "+reverseChain.size()+" links");
		} else {
			Log.v(this.getClass().getName(),"couldn't find path");
		}

		return found;
	}

	private void cleanLinkChain() {
		//Log.v(this.getClass().getName(),"length = "+linkChain.size());
		boolean changed;
		int passes = 0;
		do {
			passes++;
			changed = false;
			ArrayList<Link> newChain = new ArrayList<Link>();
			newChain.add(linkChain.get(0));

			for(int i=1; i<linkChain.size()-1; ++i) {
				if(linkChain.get(i).isBetween(linkChain.get(i-1), linkChain.get(i+1)) ) {
					newChain.add(linkChain.get(i));
				} else if(!linkChain.get(i).isLinked(linkChain.get(i-1))){
					if(makeShortestConnection(newChain, linkChain.get(i))) {
						newChain.add(linkChain.get(i));
						changed = true;
					} else {
						linkChain.get(i).selected = false;
					}
				} else {
					linkChain.get(i).selected = false;
					changed = true;
				}
			}

			if(linkChain.size() > 1) {
				int last = linkChain.size()-1;
				if(!linkChain.get(last).isLinked(linkChain.get(last-1))) {
					if(makeShortestConnection(newChain, linkChain.get(last))) {
						newChain.add(linkChain.get(last));
						changed = true;
					} else {
						linkChain.get(last).selected = false;
					}
				} else {
					newChain.add(linkChain.get(last));
				}
			}

			linkChain = newChain;
		} while(changed && passes < 3);

		if(changed) {
			for(int i=1; i<linkChain.size()-1; ++i) {
				linkChain.get(i).selected = false;
			}
			ArrayList<Link> newChain = new ArrayList<Link>();
			newChain.add(linkChain.get(0));
			Link lastLink = linkChain.get(linkChain.size()-1);
			if(makeShortestConnection(newChain, lastLink)) {
				newChain.add(lastLink);
			}
			linkChain = newChain;
		}
	}






	/**
	 * Standard window-focus override. Notice focus lost so we can pause on
	 * focus lost. e.g. user switches to take a call.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus) pause();
	}

	/**
	 * Installs a pointer to the text view used for messages.
	 */
	public void setTextView(TextView textView) {
		mStatusText = textView;
	}

	public void setMoneyText(TextView text) {
		mMoneyText = text;
	}

	public void setScore1Text(TextView text) {
		mScoreTextHuman = text;
		mScoreTextHuman.setTextColor(players.get(PLAYER_HUMAN).color);
	}

	public void setScore2Text(TextView text) {
		mScoreTextComp = text;
		mScoreTextComp.setTextColor(players.get(PLAYER_COMPUTER).color);
	}

	public void setMusic(MediaPlayer mp) {
		mMusic = mp;
	}

	public void setLinkButton(ToggleImageButton b) {
		linkButton = b;
	}

	public void setLevelButton(LevelSelectButton b) {
		levelButton = b;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		setSurfaceSize(w, h);
	}


	// class for detecting gestures, such as scrolling
	private class NetworkGestures extends SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			//Log.v(this.getClass().getName(),"scrolling: "+distanceX+", "+distanceY);
			xOffset -= distanceX;
			yOffset -= distanceY;
			update();
			return true;
		}

		@Override
		public boolean onFling(MotionEvent  e1, MotionEvent  e2, float velocityX, float velocityY) {
			Log.v(this.getClass().getName(),"flung");
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if(zoomed) {
				zoomed = false;
				int oldEdge = edgeLength;
				edgeLength = EDGE_LENGTH_DEFAULT;
				xOffset = -(int)((e.getX()-xOffset)*edgeLength/oldEdge-mCanvasWidth/2.);
				yOffset = -(int)((e.getY()-yOffset)*edgeLength/oldEdge-mCanvasHeight/2.);
			} else {
				zoomed = true;
				int overhead = linkButton.getHeight();
				int width = (mCanvasWidth-2*PADDING)/nWidth;
				int height = (mCanvasHeight-overhead-2*PADDING)/nHeight;
				edgeLength = Math.min(width, height);
				xOffset = (mCanvasWidth - edgeLength*(nWidth-1))/2;
				yOffset = (mCanvasHeight + overhead - edgeLength*(nHeight-1))/2;
			}
			return true;
		}
	}


	/**
	 * A call-back for when the user presses the link button.
	 */
	public OnClickListener mLinkListener = new OnClickListener() {
		public void onClick(View v) {
			if(mMode >= STATE_RUNNING) {
				if(linkButton.isChecked())
					setState(STATE_BUY_LINKS);
				else
					setState(STATE_RUNNING);
			} else {
				linkButton.setChecked(false);
			}
		}
	};
}
