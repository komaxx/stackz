package com.suchgame.stackz.model;

import java.util.ArrayDeque;
import java.util.HashSet;

import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;

public class Move {
	public static enum DefaultPattern {
		I,		i_,		o,		s,		T
//		,     single,	pair,
//		_i, z,
	}

	private String id;
	private PatternCoord[] basicCoords;
	private int sizeX = 0;
	private int sizeY = 0;
	
	private PatternCoord position = new PatternCoord(0, 0);
	private PatternOrientation orientation = PatternOrientation.cw_0;
	
	private boolean possible = true;
	
	// /////////////////////////////////////////////////////////////////////////
	// tmp, caches
	/**
	 * The current coords of the move as seen on the board (including position
	 * and orientation). Note: the orientation of the y-axis follows the one of
	 * OpenGl: y=0 is on the BOTTOM.
	 */
	private PatternCoord[] nowCoords;
	private boolean nowCoordsDirty = true;
	
	private HashSet<PatternCoord> tmpCoords = new HashSet<PatternCoord>();
	private static Move tmpMove = new Move(); 
	
	
	public Move() {
		this(chooseRandomDefaultPattern());
	}
	

	public Move(DefaultPattern defPattern){
//		basicCoords = new PatternCoord[]{
//				new PatternCoord(0,0),
//				new PatternCoord(0,1),
//				new PatternCoord(0,2),
//				new PatternCoord(1,0),
//				new PatternCoord(1,1),
//				new PatternCoord(1,2),
//				new PatternCoord(2,0),
//				new PatternCoord(2,1),
//				new PatternCoord(2,2),
//		};
		
		
		//*
		switch (defPattern){
		case I:
			basicCoords = new PatternCoord[]{
					new PatternCoord(0,0),
					new PatternCoord(0,1),
					new PatternCoord(0,2),
					new PatternCoord(0,3)
			};
			break;
		case i_:
			basicCoords = new PatternCoord[]{
					new PatternCoord(0,0),
					new PatternCoord(0,1),
					new PatternCoord(0,2),
					new PatternCoord(1,0)
			};

			break;
		case o:
			basicCoords = new PatternCoord[]{
					new PatternCoord(0,0),
					new PatternCoord(0,1),
					new PatternCoord(1,0),
					new PatternCoord(1,1)
			};
			break;
		case s:
			basicCoords = new PatternCoord[]{
					new PatternCoord(0,0),
					new PatternCoord(1,0),
					new PatternCoord(1,1),
					new PatternCoord(2,1)
			};
			break;
		case T:
			basicCoords = new PatternCoord[]{
					new PatternCoord(0,1),
					new PatternCoord(1,1),
					new PatternCoord(2,1),
					new PatternCoord(1,0)
			};
			break;
//		case single:
//			basicCoords = new PatternCoord[]{
//					new PatternCoord(0,0)
//			};
//			break;
//		case pair:
//			basicCoords = new PatternCoord[]{
//					new PatternCoord(0,0), 
//					new PatternCoord(0,1)
//			};
//			break;
		default:
			KoLog.w(this, "Unknown default pattern: " + defPattern +". Aborted.");
			basicCoords = new PatternCoord[]{ new PatternCoord(0, 0) };
			break;
		}
		
		//*/
		basicSetup();
	}
	
	private void basicSetup() {
		makeId();
		this.trim();
		this.makeNowCoords();
		this.setPosition(
				(Game2.BOARD_SIZE - getSizeX())/2, 
				(Game2.BOARD_SIZE - getSizeY())/2);
	}

	public void set(Move m){
		this.basicCoords = new PatternCoord[m.basicCoords.length];
		for (int i = 0; i < m.basicCoords.length; i++){
			this.basicCoords[i] = new PatternCoord(m.basicCoords[i]);
		}
		this.trim();
		this.makeNowCoords();
		
		this.setPosition(m.position);
		this.orientation = m.orientation;
		this.nowCoordsDirty = true;
	}
	
	public PatternOrientation getOrientation() {
		return orientation;
	}

	private static int nextId;
	private void makeId() {
		this.id = "p" + nextId++;
	}

	private void makeNowCoords() {
		nowCoords = new PatternCoord[basicCoords.length];
		for (int i = 0; i < basicCoords.length; i++){
			nowCoords[i] = new PatternCoord(basicCoords[i]);
		}
	}

	private void trim() {
		trimCoords(basicCoords);

		this.sizeX = 0;
		this.sizeY = 0;
		for (PatternCoord c : basicCoords){
	        if (c.x > this.sizeX) this.sizeX = c.x;
	        if (c.y > this.sizeY) this.sizeY = c.y;
		}
		
	    this.sizeX += 1;
	    this.sizeY += 1;
	}
	
	private static void trimCoords(PatternCoord[] coords){
		int minX = 999;
		int minY = 999;
		
		for (PatternCoord c : coords){
			if (c.x < minX) minX = c.x;
			if (c.y < minY) minY = c.y;
		}
		for (PatternCoord c : coords) c.move(-minX, -minY);
	}
	
	public PatternCoord getPosition() {
		return position;
	}
	
	public void turnCV() {
		this.orientation = PatternOrientation.values()[  
		                       (this.orientation.ordinal()+1) % PatternOrientation.values().length ];
		
		int nuTargetPosX = position.x + (getSizeY()-getSizeX())/2;
		int nuTargetPosY = position.y + (getSizeX()-getSizeY())/2;
		
		setPosition(nuTargetPosX, nuTargetPosY);		// sanitize the position
		this.nowCoordsDirty = true;
	}
	
	public void mirror() {
		for (PatternCoord pc : basicCoords) pc.x = getBasicSizeX() - pc.x - 1; 
		
		trimCoords(basicCoords);
		this.nowCoordsDirty = true;
	}
	
	/**
	 * Sets the position after sanitizing it to stay on the board/s
	 */
	public void setPosition(PatternCoord position) {
		this.setPosition(position.x, position.y);
	}
	
	/**
	 * Sets the position after sanitizing it to stay on the board/s
	 */
	public void setPosition(int nuX, int nuY) {
		this.position.set(
				RenderUtil.clamp(nuX, 0, Game2.BOARD_SIZE - getSizeX()), 
				RenderUtil.clamp(nuY, 0, Game2.BOARD_SIZE - getSizeY()));
		this.nowCoordsDirty = true;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isPossible() {
		return possible;
	}
	
	public void setPossible(boolean possible) {
		this.possible = possible;
	}
	
	public PatternCoord[] getBasicCoords() {
		return basicCoords;
	}
	
	public PatternCoord[] getCoords(){
		if (nowCoordsDirty){
			rotateNowCoords();
			positionNowCoords();
			
			nowCoordsDirty = false;
		}
		return nowCoords;
	}
	
	private void rotateNowCoords() {
		switch (orientation){
		case cw_90:
			for (int i = 0; i < basicCoords.length; i++){
				nowCoords[i].set(sizeY - basicCoords[i].y - 1, basicCoords[i].x);
			}
			break;
		case cw_180:
			for (int i = 0; i < basicCoords.length; i++){
				nowCoords[i].set(sizeX - basicCoords[i].x - 1, sizeY - basicCoords[i].y - 1);
			}
			break;
		case cw_270:
			for (int i = 0; i < basicCoords.length; i++){
				nowCoords[i].set(basicCoords[i].y, sizeX - basicCoords[i].x - 1);
			}
			break;
		default:
			KoLog.e(this, "Was asked to rotate for unknown orientation: " + orientation);
			KoLog.e(this, "Fallback to 0 degrees rotation.");
		case cw_0:
			for (int i = 0; i < basicCoords.length; i++){
				nowCoords[i].set(basicCoords[i]);
			}
			break;
		}
	}
	
	private void positionNowCoords() {
		for (PatternCoord c : nowCoords){
			c.move(this.position.x, this.position.y); 
		}
	}
	
	public int getSizeX() {
		return (orientation==PatternOrientation.cw_0 || orientation==PatternOrientation.cw_180) ? sizeX : sizeY;
	}
	
	public int getSizeY() {
		return (orientation==PatternOrientation.cw_0 || orientation==PatternOrientation.cw_180) ? sizeY : sizeX;
	}
	
	public int getBasicSizeX() {
		return sizeX;
	}
	
	public int getBasicSizeY() {
		return sizeY;
	}

	/**
	 * Checks, whether the given coords can be positioned/turned to be
	 * the same as this move.
	 */
	public boolean sameAs(HashSet<BoxCoord> coords) {
		if (coords.size() != this.basicCoords.length) return false;
		
		// position c in lower left corner
		int minX = 999;
		int minY = 999;
		for (BoxCoord c : coords){
			if (c.x < minX) minX = c.x;
			if (c.y < minY) minY = c.y;
		}
		
		tmpCoords.clear();
		for (BoxCoord c : coords){
			tmpCoords.add(new PatternCoord(c.x - minX, c.y - minY));
		}
		
		tmpMove.set(this);
		for (int i = 0; i < 4; i++){
			PatternCoord[] nowMyCoords = tmpMove.getCoords();
			trimCoords(nowMyCoords);
			boolean equal = true;
			for (PatternCoord c : nowMyCoords){
				if (!tmpCoords.contains(c)){
					equal = false;
					break;
				}
			}
			
			if (equal) return true;
			tmpMove.turnCV();
		}
		
		return false;
	}

	/**
	 * Moves / rotates the move to fit the given coords. You should check first that this
	 * is even possible!
	 */
	public void moveToEqual(HashSet<BoxCoord> c) {
		// TODO evil evil hack!!! Just takes the input and makes it it's nowCoords
		
		int index = 0;
		for (BoxCoord coord : c) nowCoords[index++].set(coord.x, coord.y);
		nowCoordsDirty = false;
	}

	
	// ////////////////////////////////////////////////////////////////////////////////
	// Random pattern chooser (similar to the one used in the Tetris guidelines)

	private static ArrayDeque<DefaultPattern> history;
	private static DefaultPattern chooseRandomDefaultPattern() {
		if (history == null) history = new ArrayDeque<Move.DefaultPattern>();
		
		int RANDOMIZER_TRIALS = 12;
		DefaultPattern candidate = DefaultPattern.values()[  (int)(Math.random()*DefaultPattern.values().length) ];
		for (int i = 0; i < RANDOMIZER_TRIALS; i++){
			// check if in the history
			if (!history.contains(candidate)) break;
			// pick the next
			candidate = DefaultPattern.values()[  (int)(Math.random()*DefaultPattern.values().length) ];
		}
		
		if (history.size() > 3) history.removeLast();
		history.addFirst(candidate);
		
		return candidate;
	}
}
