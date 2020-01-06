package com.suchgame.stackz.move;

import com.suchgame.stackz.game.TiledBackgroundNode;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.ADelayedGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.SceneGraph;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.AnimatedRotationNode;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.AnimatedScaleNode;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.AnimatedTranslationNode;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.model.Move;
import com.suchgame.stackz.model.PatternOrientation;
import com.suchgame.stackz.ui.GameView;

/**
 * Combines the several nodes used for visualization of a move under
 * a single facade for easier handling.
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public class MoveVizNodes {
	private static final float PIPELINE_1_SIZE_FACTOR = 0.25f;
	private static final float PIPELINE_2_SIZE_FACTOR = 0.2f;
	
	private AnimatedTranslationNode translationNode;
	private AnimatedRotationNode rotationNode;
	private AnimatedScaleNode scaleNode;
	private MovePatternNode patternRenderNode;

	private Move move;
	private int pipelineIndex = -100;		// 0: current, 1: next, .. -1: stored move
	
	
	public MoveVizNodes(Move move){
		this.move = move;
		
		translationNode = new AnimatedTranslationNode();
		translationNode.setAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
		
		rotationNode = new AnimatedRotationNode();
		rotationNode.setAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
		
		scaleNode = new AnimatedScaleNode();
		scaleNode.setScaleDirect(0.1f);
		
		patternRenderNode = new MovePatternNode();
		patternRenderNode.setMove(move);
	}
	
	public String getMoveId(){
		return move.getId();
	}
	
	public void setup(MoveNode moveNode, SceneGraph sceneGraph){
		sceneGraph.addNode(moveNode, translationNode);
		sceneGraph.addNode(translationNode, rotationNode);
		sceneGraph.addNode(rotationNode, scaleNode);
		sceneGraph.addNode(scaleNode, patternRenderNode);
	}

	public void updateOrientation(PatternOrientation orientation) {
		rotationNode.setRotation(orientation.ordinal() * 90f, 0, 0 , 1);
	}

	public void abortAndRemove() {
		patternRenderNode.abort();
		scheduleRemove();
	}
	
	public void executedAndRemove() {
		patternRenderNode.done();
		scheduleRemove();
	}

	private void scheduleRemove() {
		Node root = patternRenderNode.getSceneGraph().getRoot();
		root.queueInGlThread(
				new ADelayedGlRunnable(root, 250) {
					@Override
					protected void doRun(RenderContext rc) {
						patternRenderNode.getSceneGraph().removeNode(translationNode);
					}
				}
			);
	}

	/**
	 * 0: the current/active move. >0 one of the future pattern.
	 */
	public void setPipelineIndex(SceneGraphContext sc, int index) {
		this.pipelineIndex = index;
		this.patternRenderNode.setPipelineIndex(index);
		float boxSize = GameView.getBoxSize(sc);
		
		float pipelineZ = TiledBackgroundNode.NEXT_MOVE_SHAFT_START_Z * boxSize;
		
		if (pipelineIndex == -1){
			this.scaleNode.setScale(0.35f);
			this.translationNode.setTranslation(0, -sc.surfaceWidth/2f - boxSize, pipelineZ);
		} else if (pipelineIndex == 0){
			this.scaleNode.setScale(0.5f);
			this.translationNode.setTranslation(0, sc.surfaceWidth/2f + boxSize, pipelineZ);
		} else if (pipelineIndex == 1){
			this.scaleNode.setScale(PIPELINE_1_SIZE_FACTOR);
			this.translationNode.setTranslation(-2f*boxSize, sc.surfaceWidth/2f + boxSize/2f, pipelineZ+boxSize);
		} else {
			this.scaleNode.setScale(PIPELINE_2_SIZE_FACTOR);
			this.translationNode.setTranslation(-2f*boxSize, sc.surfaceWidth/2f + boxSize*1.5f, pipelineZ + boxSize);
		}
	}
}
