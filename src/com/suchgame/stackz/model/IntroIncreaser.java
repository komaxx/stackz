//package com.suchgame.stackz.model;
//
//import com.suchgame.stackz.gl.SceneGraphContext;
//import com.suchgame.stackz.model.Game.GameState;
//import com.suchgame.stackz.model.Game.IGameListener;
//
//public class IntroIncreaser implements IGameListener{
//	private static final long INTER_STEP_DELAY_NS = 200 * 1000L * 1000L;
//	
//	private Game game;
//	private boolean active = false;
//	
//	private int currentStep = -1;
//	private long nextIncreaseTime = 0;
//	
//	private static final int[][] steps = new int[][]{
//		new int[]{
//				0,	0,	1,	1,	0,	0,
//				0,	0,	0,	0,	1,	0,
//				0,	0,	1,	1,	0,	0,
//				0,	1,	0,	0,	0,	0,
//				0,	0,	1,	1,	0,	0,
//				0,	0,	0,	0,	0,	0
//		},
//		new int[]{
//				0,	0,	1,	0,	0,	0,
//				0,	0,	1,	0,	0,	0,
//				0,	0,	1,	0,	0,	0,
//				0,	1,	1,	1,	0,	0,
//				0,	0,	1,	0,	0,	0,
//				0,	0,	0,	0,	0,	0,
//		},
//		new int[]{
//				0,	0,	0,	0,	0,	0,
//				0,	0,	1,	1,	0,  0,
//				0,	1,	0,	1,	0,	0,
//				0,	1,	0,	1,	0,	0,
//				0,	0,	1,	0,	0,	0,
//				0,	0,	0,	0,	0,	0
//		},
//		new int[]{
//				0,	0,	0,	0,	0,	0,
//				0,	0,	1,	1,	0,	0,
//				0,	1,	0,	0,	0,	0,
//				0,	1,	0,	1,	0,	0,
//				0,	0,	1,	0,	0,	0,
//				0,	0,	0,	0,	0,	0
//		},
//		new int[]{
//				0,	1,	0,	1,	0,	0,
//				0,	1,	0,	1,	0,	0,
//				0,	1,	1,	0,	0,	0,
//				0,	1,	0,	1,	0,	0,
//				0,	1,	0,	0,	0,	0,
//				0,	0,	0,	0,	0,	0
//		},
//		new int[]{
//				0,	0,	0,	0,	0,	0,
//				0,	1,	1,	1,	0,	0,
//				0,	1,	0,	0,	0,	0,
//				0,	0,	0,	1,	0,	0,
//				0,	1,	1,	1,	0,	0,
//				0,	0,	0,	0,	0,	0
//		},
//	};
//	
//	public IntroIncreaser(Game game){
//		this.game = game;
//		this.game.addListener(this);
//		
//		gameStateChanged();
//	}
//	
//	public void tick(SceneGraphContext sc){
//		if (!active) return;
//		if (nextIncreaseTime == 0){
//			nextIncreaseTime = sc.frameNanoTime + INTER_STEP_DELAY_NS;
//		} else {
//			if (sc.frameNanoTime > nextIncreaseTime){
//				toNextStep();
//				nextIncreaseTime = sc.frameNanoTime + INTER_STEP_DELAY_NS;
//			}
//		}
//	}
//	
//	private void toNextStep() {
//		currentStep++;
//		
//		if (currentStep/2 >= steps.length){
//			game.introFinished();
//			return;
//		}
//		
//		if (currentStep %2 == 0){
//			GameUtil.increaseToPattern(game, steps[currentStep/2]);
//		} else {
//			GameUtil.increaseToPattern(game, invert(steps[currentStep/2]));
//		}
//	}
//
//	private static int[] invert(int[] pattern) {
//		int[] revPattern = new int[pattern.length];
//		for (int i = 0; i < pattern.length; i++){
//			revPattern[i] = Math.abs( pattern[i]-1 );
//		}
//		return revPattern;
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
//		if (!active && game.getGameState() == GameState.INTRO){
//			nextIncreaseTime = 0;
//			currentStep = -1;
//			active = true;
//		} else if (active && game.getGameState() != GameState.INTRO){
//			active = false;
//		}
//	}
//}
