package com.suchgame.stackz.model;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;

/**
 * Contains the abstract data and model that is visualized by the GL.
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public class GameOld {
	public static enum GameState {
		INTRO, DEMO, RUNNING, COUNTING_DOWN, PAUSED, FINISHED
	}

	public static final int[] FINISHED_PATTERN = new int[]{
		0,1,0,1,0,1,
		1,0,1,0,1,0,
		0,1,0,1,0,1,
		1,0,1,0,1,0,
		0,1,0,1,0,1,
		1,0,1,0,1,0  };
	
	/**
	 * The maximum time that a move is allowed to take on the base level.
	 * The time is reduced by a factor with each increasement of the
	 * base level.
	 */
	public static final long BASE_MOVE_TIME_NS = RenderUtil.nsFromMs(5 * 1000);
	
	/**
	 * Multiplied on the scoring for boxes in completed squares.
	 */
	public static final int BOX_SCORE_MULTIPLIER = 20;
	
	/**
	 * How many boxes fit into the game cage horizontally or vertically.
	 */
	public static int BOARD_SIZE = 6;
	
	/**
	 * As soon a level has more at least this amount of boxes, it becomes the new
	 * scoring level.
	 */
	public static final int MIN_SCORING_BOXES = BOARD_SIZE*BOARD_SIZE;

	/**
	 * Only squares at least as big as this get you points.
	 */
	public static final int MIN_BUILD_UPON_SQUARE_SIZE = 2;
	
	/**
	 * The game is finished, when the difference between the lowest unfilled
	 * level and the highest level is more than this.
	 */
	public static final int MAX_LEVEL_DIFFERENCE = 6;
	
	/**
	 * Only squares at least as big as this get you points.
	 */
	public static final int MIN_SCORED_SQUARE_SIZE = 3;

	/**
	 * The delay between 'start game' and when it actually starts.
	 */
	public static final int COUNT_DOWN_MS = 2500;

	/**
	 * The game will wait this long before setting a game "finished"
	 */
	private static final long FINISHED_NOTIFICATION_DELAY_NS = RenderUtil.nsFromMs(1000);
	
	
	private GameState gameState = GameState.INTRO;
	
	/**
	 * The earned points. Points are given for quick move finishes and completed 
	 * levels.
	 */
	private int score = 0;
	
	/**
	 * This delivers the next patterns that are to be played.
	 * NOTE: The "current" pattern is the first in this array
	 */
	private ArrayDeque<Move> nextMoves = new ArrayDeque<Move>();

	/**
	 * The user can store one move for later, when is fits better.
	 */
	private Move storedMove = null;
	
	/**
	 * What is to be delivered to the UI. A simple cash array.
	 */
	private Move[] uiNextMoves;
	
	/**
	 * All current heights, as sequence of rows. 
	 * Note: The y-axis follows the GL convention, so 0 is at the bottom of the screen!
	 */
	private int[] levels = new int[BOARD_SIZE*BOARD_SIZE];
	private boolean[] canBuildUpon = new boolean[BOARD_SIZE*BOARD_SIZE];
	
	private HashSet<Square> squares = new HashSet<Square>();
	private HashSet<BoxCoord> scoredCoords = new HashSet<BoxCoord>();
	
	/**
	 * This is the level that will get points as long as the criteria is met.
	 */
	private int scoringLevel;
	
	private int lowestUnfilledLevel;
	private int highestLevel;
	
	private long countDownFinishTime = 0;
	
	/**
	 * When no move is possible any longer, the game finishes. The visualization of that 
	 * works with delay. This value is the end of that delay.
	 */
	private long gameFinishedTimeNs = 0;
	
	/**
	 * Time left until the move is to be finished.
	 */
	private long moveRestTimeNs = -1;
	/**
	 * How long the current move was allowed to take.
	 */
	private long moveMaxTimeNs = -1;
	private long lastRunningFrameTimeStamp = 0;
	
	/**
	 * The id of the current game.
	 */
	private int gameId = 0;
	
	// /////////////////////////////////////////////////////////////////////
	// tmps caches
	private BoxCoord tmpBoxCoord = new BoxCoord();
	private Square tmpSquare = new Square();
	private ArrayList<Square> tmpSquares = new ArrayList<Square>();
	private HashSet<BoxCoord> tmpScoredBoxes = new HashSet<BoxCoord>();
	
	
	public GameOld(){
		resetGame();
	}
	
	public int getLevel(int x, int y) {
		return levels[indexForCoord(x,y)];
	}

	public boolean canBuildUpon(int x, int y) {
		return canBuildUpon[indexForCoord(x, y)];
	}

	/**
	 * The currently selected move. May be <code>null</code>.
	 */
	public Move getCurrentMove(){
		return getNextMoves()[0];
	}
	
	public Move getStoredMove() {
		return storedMove;
	}
	
	public Move[] getNextMoves() {
		if (uiNextMoves == null){
			uiNextMoves = nextMoves.toArray(new Move[nextMoves.size()]);
		}
		
		return uiNextMoves;
	}
	
	public float getMoveRestTimePercentage() {
		return (float)moveRestTimeNs / (float)moveMaxTimeNs;
	}
	
	/**
	 * to be called once per rendering cycle.
	 * Or at least once every 5 frames ;)
	 */
	public void tick(SceneGraphContext sc){
		if (gameState == GameState.COUNTING_DOWN && sc.frameNanoTime > countDownFinishTime){
			// count-down is done!
			gameState = GameState.RUNNING;
			lastRunningFrameTimeStamp = sc.frameNanoTime;
			notifyListeners_gameStateChanged();
		} else if (gameState == GameState.RUNNING){
			long delta = sc.frameNanoTime - lastRunningFrameTimeStamp;
			moveRestTimeNs -= delta;
			lastRunningFrameTimeStamp = sc.frameNanoTime;
			
			if (gameFinishedTimeNs > 0 && sc.frameNanoTime > gameFinishedTimeNs){
				finishGame();
			}
			
//			if (moveRestTimeNs < 0){
//				autoExecuteMoveByTimeOut();
//			}
		}
	}
	
//	private void autoExecuteMoveByTimeOut() {
//		ArrayList<Move> moves = findPossibleMoves();
//		Move toExecute = moves.get((int) (Math.random() * moves.size()));
//		replaceCurrentMove(toExecute);
//		executeCurrentMove();
//	}

//	private void replaceCurrentMove(Move toExecute) {
//		getCurrentMove().set(toExecute);
//	}

	private void finishGame() {
		gameState = GameState.FINISHED;
		KoLog.i(this, "Game finished");
//		GameUtil.increaseToPattern(GameOld.this, FINISHED_PATTERN);
		notifyListeners_gameStateChanged();
	}

	private ArrayList<Move> findPossibleMoves(Move move) {
		ArrayList<Move> possibleMoves = new ArrayList<Move>();
		
		if (getCurrentMove() == null) return possibleMoves;
		
		Move tmpMove = new Move();
		tmpMove.set(move);

		PatternCoord tmpCoord = null;
		
		for (int orientation = 0; orientation < 9; orientation++){		// 2 * 4 orientations + mirroring
			for (int y = 0; y <= GameOld.BOARD_SIZE-tmpMove.getSizeY(); y++){
				for (int x = 0; x <= GameOld.BOARD_SIZE-tmpMove.getSizeX(); x++){
					if (!canBuildUpon(x, y)) continue;

					tmpMove.setPosition(x, y);
					PatternCoord[] coords = tmpMove.getCoords();
					
					tmpCoord = coords[0];
					int baseZ = getLevel(tmpCoord.x, tmpCoord.y);
					
					boolean valid = true;
					for (int i = 0; i < coords.length; i++){
						tmpCoord = coords[i];
						if (getLevel(tmpCoord.x, tmpCoord.y) != baseZ){
							valid = false;
							break;
						}
						
						if (!canBuildUpon(tmpCoord.x, tmpCoord.y)){
							valid = false;
							break;
						}
					}
					
					if (!valid) continue;
					
					// this is a valid move!
					Move toAdd = new Move();
					toAdd.set(tmpMove);
					possibleMoves.add(toAdd);
				}
			}
			if (orientation == 4) tmpMove.mirror();
			else tmpMove.turnCV();
		}
		
		return possibleMoves;
	}

	public void startGame(SceneGraphContext sc){
		if (gameState == GameState.RUNNING){
			KoLog.i(this, "Can not start running game!");
			return;
		} else if (gameState == GameState.COUNTING_DOWN){
			KoLog.i(this, "Already counting down!");
			return;
		} else if (gameState == GameState.PAUSED){
			KoLog.i(this, "Game resumed");
		} else if (gameState == GameState.FINISHED 
				|| gameState == GameState.DEMO){
			KoLog.i(this, "New game started");
			resetGame();
		}
		
		countDownFinishTime = sc.frameNanoTime + COUNT_DOWN_MS*1000L * 1000L; 
				
		this.gameState = GameState.COUNTING_DOWN;
		notifyListeners_gameStateChanged();
	}
	
	public GameState getGameState() {
		return gameState;
	}
	
	public void pauseGame(boolean notify){
		if (gameState != GameState.RUNNING){
			// can only pause running games.
			return;
		}
		
		gameState = GameState.PAUSED;
		if (notify) notifyListeners_gameStateChanged();
	}
	
	public void abortCurrentGame() {
		nextMoves.clear();
		uiNextMoves = null;
		storedMove = null;
		gameState = GameState.DEMO;
		notifyListeners_gameStateChanged();
	}
	
	private void resetGame() {
		for (int i = 0; i < levels.length; i++){
			this.levels[i] = -1;
		}
		for (int i = 0; i < canBuildUpon.length; i++){
			this.canBuildUpon[i] = true;
		}
		scoringLevel = 0;
		lowestUnfilledLevel = 0;
		highestLevel = 0;
		moveRestTimeNs = computeMoveTimeNs();
		gameFinishedTimeNs = 0;
		gameId++;
		
		this.score = 0;
		
		initNextPatterns();
		recomputeLevels();
		squares.clear();
		scoredCoords.clear();
		
		notifyListeners_levelsChange();
		notifyListeners_currentMoveChange();
	}

	private void initNextPatterns() {
		nextMoves.clear();
		
		for (int i = 0; i < 4; i++) nextMoves.add(new Move());
		uiNextMoves = null;
		storedMove = null;
	}
	
	private long computeMoveTimeNs() {
		// TODO reduce with rising lowest level.
		return RenderUtil.nsFromMs(5*1000);
	}


	public int getGameId() {
		return gameId;
	}
	
	public int getScore() {
		return score;
	}

	public void switchStoredWithCurrentMove(SceneGraphContext sc){
		Move currentMove = nextMoves.removeFirst();
		if (storedMove != null){
			nextMoves.addFirst(storedMove);
		}
		storedMove = currentMove;
		uiNextMoves = null;
		
		computeMovesPossible();
		checkForGameEnd(sc);
		
		notifyListeners_currentMoveChange();
	}
	
	public void executeCurrentMove(SceneGraphContext sc) {
		if (gameState != GameState.RUNNING){
			KoLog.e(this, "Not executing move: not running");
			return;
		}
		
		PatternCoord[] coords = getCurrentMove().getCoords();
		for (PatternCoord coord : coords){
			int indexForCoord = indexForCoord(coord.x,coord.y);
			levels[indexForCoord]++;
			canBuildUpon[indexForCoord] = false;
			
		}
		recomputeLevels();
		scoreMove();
		findBuildUponBlocks();
		
		nextMoves.pop();
		nextMoves.addLast(new Move());
		uiNextMoves = null;
		
		computeMovesPossible();
		moveRestTimeNs = computeMoveTimeNs();
		
		notifyListeners_currentMoveChange();
		notifyListeners_levelsChange();
		
		checkForGameEnd(sc);
	}
	
	private void computeMovesPossible() {
		Move[] moves = getNextMoves();
		for (int i = 0; i < moves.length; i++){
			boolean possible = findPossibleMoves(moves[i]).size() > 0;
			moves[i].setPossible(possible);
			
			KoLog.i(this, i + ": ", possible ? "possible" : "IMPOSSIBLE");
		}
		
		if (storedMove != null){
			boolean possible = findPossibleMoves(storedMove).size() > 0;
			storedMove.setPossible(possible);
			KoLog.i(this, "Stored move: ", possible ? "possible" : "IMPOSSIBLE");
		}
	}

	private void checkForGameEnd(SceneGraphContext sc) {
		int levelsDifference = highestLevel - lowestUnfilledLevel;
		if (levelsDifference > MAX_LEVEL_DIFFERENCE){
			// finish immediately!
			gameFinishedTimeNs = sc.frameNanoTime;
			return;
		}
		
		int currentMovePossibilities = findPossibleMoves(getCurrentMove()).size();
		if (storedMove != null 
						&& currentMovePossibilities < 1
						&& findPossibleMoves(storedMove).size() < 1){
			gameFinishedTimeNs = sc.frameNanoTime + FINISHED_NOTIFICATION_DELAY_NS;
		} else if (storedMove == null 
				&& currentMovePossibilities < 1 
				&& findPossibleMoves(getNextMoves()[1]).size() < 1){
			gameFinishedTimeNs = sc.frameNanoTime + FINISHED_NOTIFICATION_DELAY_NS;
		}
	}

	public void introFinished() {
		if (gameState != GameState.INTRO){
			// can only process from here from intro state
			return;
		}
		
		gameState = GameState.DEMO;
		notifyListeners_gameStateChanged();
	}
	
	/**
	 * This is for debugging and demo stuff only.
	 */
	public void increaseLevel(int x, int y){
		int indexForCoord = indexForCoord(x,y);
		levels[indexForCoord]++;
		canBuildUpon[indexForCoord] = false;
		recomputeLevels();
		findBuildUponBlocks();
		notifyListeners_levelsChange();
	}
	
	/**
	 * This is for tutorial and intro stuff
	 */
	public void setLevelTo(int x, int y, int nuLevel, boolean done){
		int indexForCoord = indexForCoord(x,y);
		levels[indexForCoord] = nuLevel;
		canBuildUpon[indexForCoord] = false;
		if (done) {
			recomputeLevels();
			findBuildUponBlocks();
			notifyListeners_levelsChange();
		}
	}
	
	private void recomputeLevels() {
		lowestUnfilledLevel = Integer.MAX_VALUE;
		highestLevel = 0;
		
		for (int i = 0; i < levels.length; i++){
			int level = this.levels[i];
			if (level < lowestUnfilledLevel) lowestUnfilledLevel = level;
			if (level > highestLevel) highestLevel = level;
		}
		
		// now: find the scoring level
		// approach: start with the highest level, step down until a level has >= MIN_SCORING_BOXES
		int nuScoringLevel = Math.max(lowestUnfilledLevel, 0);
		boolean found = false;
		int base = nuScoringLevel;
		for (int l = highestLevel; l >= base && !found; --l){
			int boxesOnLevel = 0;
			for (int i = 0; i < levels.length; i++){
				if (this.levels[i] >= l){
					boxesOnLevel++;
					if (boxesOnLevel >= MIN_SCORING_BOXES){
						nuScoringLevel = l;
						found = true;
						break;
					}
				}
			}
		}
		
		// if scoring level complete -> move one up!
		if (nuScoringLevel >= 0 && nuScoringLevel <= lowestUnfilledLevel){
			nuScoringLevel++;
		}

		// cap on 0
		if (lowestUnfilledLevel < 0) lowestUnfilledLevel = 0;
		
		this.scoringLevel = nuScoringLevel;
	}

	private void findBuildUponBlocks() {
		int maxXY = BOARD_SIZE - MIN_BUILD_UPON_SQUARE_SIZE;
		for (int y = 0; y <= maxXY; y++){
			for (int x = 0; x <= maxXY; x++){
				int lowestZ = Integer.MAX_VALUE;
				// now, let's see if all of the necessary fields are filled
				for (int nY = y; nY < y+MIN_BUILD_UPON_SQUARE_SIZE; nY++){
					for (int nX = x; nX < x+MIN_BUILD_UPON_SQUARE_SIZE; nX++){
						lowestZ = Math.min(lowestZ, levels[indexForCoord(nX, nY)]);
					}
				}
				
				if (lowestZ < 0) continue;
				
				for (int nY = y; nY < y+MIN_BUILD_UPON_SQUARE_SIZE; nY++){
					for (int nX = x; nX < x+MIN_BUILD_UPON_SQUARE_SIZE; nX++){
						if (getLevel(nX, nY) == lowestZ){
							canBuildUpon[indexForCoord(nX, nY)] = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Relies on correct min/max levels. Call recompute levels before invoking
	 * this method.
	 */
	private void scoreMove() {
		tmpSquares.clear();
		
		// find new squares
		int maxXY = BOARD_SIZE - MIN_SCORED_SQUARE_SIZE;
		for (int z = Math.max(0, lowestUnfilledLevel); z <= highestLevel; z++){
			tmpSquare.z = z;
			for (int y = 0; y <= maxXY; y++){
				tmpSquare.llY = y;
				for (int x = 0; x <= maxXY; x++){
					tmpSquare.llX = x;
					
//					int maxSize = BOARD_SIZE - Math.max(x,y);
					int maxSize = MIN_SCORED_SQUARE_SIZE;
					for (int size = MIN_SCORED_SQUARE_SIZE; size <= maxSize; size++){
						tmpSquare.size = size; 
						
						// now, let's see if this square was not filled before
						if (this.squares.contains(tmpSquare)){
							// previously filled. Ignore.
							// KoLog.i(this, "Previously filled square: " + tmpSquare);
							continue;
						}
						
						// now, let's see if all of the necessary fields are filled
						boolean filled = true;
						for (int nY = y; nY < y+size && filled; nY++){
							for (int nX = x; nX < x+size; nX++){
								if (levels[indexForCoord(nX, nY)] < z){
									filled = false;
									break;
								}
							}
						}
						
						if (filled){
							tmpSquares.add(new Square(tmpSquare));
						}
					}
				}
			}
		}
		
		tmpScoredBoxes.clear();
		if (tmpSquares.size() > 0){
			int extraScore = 0;
			for (Square s : tmpSquares){
				extraScore += s.size - 2;
			}
			
			for (Square s : tmpSquares){
				tmpBoxCoord.z = s.z;
				for (int y = s.llY; y < s.llY+s.size; y++){
					tmpBoxCoord.y = y;
					for (int x = s.llX; x < s.llX+s.size; x++){
						tmpBoxCoord.x = x;
						if (scoredCoords.contains(tmpBoxCoord)) continue;
						
						scoredCoords.add(new BoxCoord(tmpBoxCoord));
						tmpScoredBoxes.add(new BoxCoord(tmpBoxCoord));
					}
				}
			}
			
			KoLog.i(this, "Found " + tmpSquares.size() + " new squares or score: " + extraScore);
		}
		
		for (int i = 0; i < tmpScoredBoxes.size(); i++){
			this.score += (i+1)*BOX_SCORE_MULTIPLIER;
		}
		
		this.squares.addAll(tmpSquares);
		notifyListeners_scored(
				tmpSquares.toArray(new Square[tmpSquares.size()]),
				tmpScoredBoxes.toArray(new BoxCoord[tmpScoredBoxes.size()]));
		
		cleanUpSquares();
	}

	/**
	 * Removes all cached squares that will no longer matter
	 */
	private void cleanUpSquares() {
		tmpSquares.clear();
		for (Square s : squares){
			if (s.z < lowestUnfilledLevel || s.z > highestLevel) tmpSquares.add(s); 
		}
		squares.removeAll(tmpSquares);
		
		tmpScoredBoxes.clear();
		for (BoxCoord c : scoredCoords){
			if (c.z < lowestUnfilledLevel || c.z > highestLevel) tmpScoredBoxes.add(c);
		}
		scoredCoords.removeAll(tmpScoredBoxes);
	}

	private static int indexForCoord(int x, int y){
		return y*BOARD_SIZE + x;
	}

	public int getLowestUnfilledLevel() {
		return lowestUnfilledLevel;
	}
	
	public int getHighestLevel() {
		return highestLevel;
	}

	public int getScoringLevel() {
		return scoringLevel;
	}
	
	public static interface IGameListener {
		void levelsChanged();
		void currentMoveChanged();
		void gameStateChanged();
		void scored(Square[] nuSquares, BoxCoord[] boxCoords);
	}
	
	// ////////////////////////////////////////////////////
	// listener stuff
	private ArrayList<WeakReference<IGameListener>> listeners = new ArrayList<WeakReference<IGameListener>>();

	public void addListener(IGameListener nuListener){
		for (WeakReference<IGameListener> l : listeners){
			IGameListener nowL = l.get();
			if (nowL != null && nowL == nuListener) return;		// already added
		}
		
		listeners.add(new WeakReference<GameOld.IGameListener>(nuListener));
	}
	
	public void removeListener(IGameListener toRemove){
		for (WeakReference<IGameListener> l : listeners){
			IGameListener nowL = l.get();
			if (nowL != null && nowL == toRemove) {
				listeners.remove(l);
			}
		}
	}
	
	private void notifyListeners_levelsChange(){
		for (WeakReference<IGameListener> l : listeners){
			IGameListener nowL = l.get();
			if (nowL != null) nowL.levelsChanged();
		}
	}
	
	private void notifyListeners_currentMoveChange(){
		for (WeakReference<IGameListener> l : listeners){
			IGameListener nowL = l.get();
			if (nowL != null) nowL.currentMoveChanged();
		}
	}
	
	private void notifyListeners_gameStateChanged(){
		for (WeakReference<IGameListener> l : listeners){
			IGameListener nowL = l.get();
			if (nowL != null) nowL.gameStateChanged();
		}
	}
	
	private void notifyListeners_scored(Square[] squares, BoxCoord[] boxCoords){
		for (WeakReference<IGameListener> l : listeners){
			IGameListener nowL = l.get();
			if (nowL != null) nowL.scored(squares, boxCoords);
		}
	}

	// ////////////////////////////////////////////////////
	// debug stuff
	
	@SuppressWarnings("unused")
	private void print() {
		KoLog.i(this, "================================");
		for (int y = BOARD_SIZE-1; y >=0 ; --y){
			StringBuffer b = new StringBuffer();
			for (int x = 0; x < BOARD_SIZE; x++){
				b.append(canBuildUpon(x, y) ? "O" : "X");
				b.append(' ');
			}
			KoLog.i(this, b.toString());
		}
		KoLog.i(this, "================================");
	}
	
	@SuppressWarnings("unused")
	private void printLevels() {
		KoLog.i(this, "Lowest unfilled level: " + lowestUnfilledLevel);
		KoLog.i(this, "HigestLevel: " + highestLevel);
		KoLog.i(this, "Scoring level: " + scoringLevel);
	}

	public void fakeFinish(int fakeEndScore) {
		KoLog.i(this, "FAKE ending the game");

		gameState = GameState.FINISHED;
		score = fakeEndScore;
//		GameUtil.increaseToPattern(this, FINISHED_PATTERN);
		notifyListeners_gameStateChanged();
	}
}
