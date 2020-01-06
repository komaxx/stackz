package com.suchgame.stackz.move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.Context;

import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Core.GameState;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.core.listener.IGameStateListener;
import com.suchgame.stackz.core.listener.IMovesListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.SceneGraph;
import com.suchgame.stackz.model.Move;
import com.suchgame.stackz.model.PatternCoord;
import com.suchgame.stackz.ui.GameView;
import com.suchgame.stackz.ui.LevelTranslationNode;

/**
 * Shows and executes moves. Handles all the interaction with the game
 * itself.
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public class MoveNode extends Node implements IMovesListener, IGameStateListener {
	private HashMap<String, MoveVizNodes> moveVizNodes = new HashMap<String, MoveVizNodes>();
	
	private MovePaintNode movePaintNode;
	private MovePaintVibrator movePaintVibrator;
	
	// //////////////////////////////////////////
	// temps, caches, plumbing
	
	private HashMap<String, MoveVizNodes> tmpToRemove = new HashMap<String, MoveVizNodes>();
	
	
	public MoveNode(Context c) {
		transforms = false;
		draws = false;
		handlesInteraction = false;
		
		zLevel = ZLevels.MOVE;
		
		movePaintVibrator = new MovePaintVibrator(c);
		
		Core.get().registry().addMovesListener(this);
		Core.get().registry().addGameStateListener(this);
	}

	public void setUp(SceneGraph sceneGraph, LevelTranslationNode levelTranslationNode) {
		movePaintNode = new MovePaintNode(movePaintVibrator);
		sceneGraph.addNode(levelTranslationNode, movePaintNode);
	}

	private void adaptMoveVisualizationHeightToCurrentPosition(float boxSize) {
		Game2 game = Core.get().game();
		Move currentMove = game.getCurrentMove();
		if (currentMove == null) return;
		
		PatternCoord[] coords = currentMove.getCoords();
//		int lowestLevel = -1;
//		for (PatternCoord c : coords){
//			lowestLevel = Math.max(lowestLevel, game.getLevel(c.x, c.y));
//		}
	}

	@Override
	public void handleMovesChanged(SceneGraphContext sc) {
		sceneGraph.getRoot().queueInGlThread(moveChangeHandler);
	}
	private IGlRunnable moveChangeHandler = new IGlRunnable() {
		@Override
		public void run(RenderContext rc) {
			sceneGraph.getRoot().queueInGlThread(movesUpdater);
		}
	};
	
	private void updateMoves(SceneGraphContext c) {
		tmpToRemove.clear();
		tmpToRemove.putAll(moveVizNodes);
		
		// next moves
		ArrayList<Move> nextMoves = Core.get().game().getNextMoves();
		for (int i = 0; i < 3 && i < nextMoves.size(); i++){
			Move nextMove = nextMoves.get(i);
			if (nextMove == null) continue;
			
			MoveVizNodes vizNodes = moveVizNodes.get(nextMove.getId());
			tmpToRemove.remove(nextMove.getId());
			
			if (vizNodes == null) vizNodes = addVizNodesForMove(nextMove);
			vizNodes.setPipelineIndex(c, i);
		}
		
		// remove no longer interesting moves
		Set<String> keySet = tmpToRemove.keySet();
		for (String key : keySet){
			tmpToRemove.get(key).executedAndRemove();
			moveVizNodes.remove(key);
		}
		tmpToRemove.clear();
		
		// and, as a shortcut, the current one
		adaptMoveVisualizationHeightToCurrentPosition(GameView.getBoxSize(c));
	}

	private MoveVizNodes addVizNodesForMove(Move move) {
		MoveVizNodes toAdd = new MoveVizNodes(move);
		toAdd.setup(this, sceneGraph);
		this.moveVizNodes.put(move.getId(), toAdd);
		return toAdd;
	}

	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		if (!gameIsInRightState(nuState)){
			// no longer running -> remove all 
			Set<String> keys = moveVizNodes.keySet();
			for (String key : keys){
				moveVizNodes.get(key).abortAndRemove();
			}
			moveVizNodes.clear();
		} else {
			sceneGraph.getRoot().queueInGlThread(movesUpdater);
		}
	}
	private IGlRunnable movesUpdater = new IGlRunnable() {
		@Override
		public void run(RenderContext rc) {
			if (gameIsInRightState(Core.get().getGameState())) updateMoves(rc);
		}
	};

	
	private boolean gameIsInRightState(GameState nuState) {
		return nuState == GameState.RUNNING 
				|| nuState == GameState.PAUSED 
				|| nuState == GameState.COUNTING_DOWN;
	}
}