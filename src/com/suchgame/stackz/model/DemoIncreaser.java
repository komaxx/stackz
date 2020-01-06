//package com.suchgame.stackz.model;
//
//import com.suchgame.stackz.gl.SceneGraphContext;
//import com.suchgame.stackz.model.Game.GameState;
//import com.suchgame.stackz.model.Game.IGameListener;
//
//public class DemoIncreaser implements IGameListener{
//	private static final long INTER_BLOCK_DELAY_NS = 100 * 1000L * 1000L;
//	
//	private Game game;
//	private boolean active = false;
//	
//	private long nextIncreaseTime = 0;
//	
//	
//	public DemoIncreaser(Game game){
//		this.game = game;
//		this.game.addListener(this);
//		
//		gameStateChanged();
//	}
//	
//	public void tick(SceneGraphContext sc){
//		if (!active) return;
//		if (nextIncreaseTime == 0){
//			nextIncreaseTime = sc.frameNanoTime + INTER_BLOCK_DELAY_NS;
//		} else {
//			if (sc.frameNanoTime > nextIncreaseTime){
//				increaseNow();
//				nextIncreaseTime = sc.frameNanoTime + INTER_BLOCK_DELAY_NS;
//			}
//		}
//	}
//	
//	private void increaseNow() {
//		int lowestUnfilledLevel = game.getLowestUnfilledLevel();
//    	if (game.getHighestLevel() - game.getLowestUnfilledLevel() > 4){
//    		// find a coord with the lowest level
//    		boolean done = false;
//    		for (int y = 0; y < Game.BOARD_SIZE && !done; y++){
//    			for (int x = 0; x < Game.BOARD_SIZE && !done; x++){
//    				if (game.getLevel(x, y) <= lowestUnfilledLevel){
//    					game.increaseLevel(x, y);
//    					done = true;
//    				}
//    			}
//    		}
//    	} else {
//        	game.increaseLevel((int) (Math.random()*Game.BOARD_SIZE), (int)(Math.random()*Game.BOARD_SIZE));
//    	}
//	}
//
//	@Override
//	public void levelsChanged() { /* don't care */}
//
//	@Override
//	public void currentMoveChanged() { /* don't care */}
//	
//	@Override
//	public void scored(Square[] nuSquares, BoxCoord[] scoredCoords) { /* don't care */}
//
//	@Override
//	public void gameStateChanged() {
//		if (!active && game.getGameState() == GameState.DEMO){
//			nextIncreaseTime = 0;
//			active = true;
//		} else if (active && game.getGameState() != GameState.DEMO){
//			active = false;
//		}
//	}
//}
