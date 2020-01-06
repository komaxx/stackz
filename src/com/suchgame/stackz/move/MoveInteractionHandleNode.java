//package com.suchgame.stackz.move;
//
//import com.suchgame.stackz.gl.ZLevels;
//import com.suchgame.stackz.gl.scenegraph.Node;
//import com.suchgame.stackz.gl.scenegraph.interaction.DragInteractionInterpreter;
//import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
//import com.suchgame.stackz.model.Game.GameState;
//
//public class MoveInteractionHandleNode extends Node {
//	private DragInteractionInterpreter dragInterpreter;
//	
//	public MoveInteractionHandleNode(MoveNode moveNode) {
//		draws = false;
//		transforms = false;
//		handlesInteraction = false;
//		
//		this.zLevel = ZLevels.MENU;
//		
//		dragInterpreter = new DragInteractionInterpreter(moveNode);
//	}
//	
//	@Override
//	protected boolean onInteraction(InteractionContext interactionContext) {
//		boolean consume = dragInterpreter.onInteraction(interactionContext);
//		return consume; 
//	}
//
//	public void gameStateChanged(GameState nuGameState) {
//		this.interactive = nuGameState==GameState.RUNNING;
//	}
//}
