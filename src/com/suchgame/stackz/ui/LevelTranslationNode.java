package com.suchgame.stackz.ui;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.TranslationNode;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;

/**
 * @author Created by Matthias Schicker on 12/19/13.
 */
public class LevelTranslationNode extends TranslationNode {
	private InterpolatedValue zPos = new InterpolatedValue(
			AnimationType.LINEAR, -500, InterpolatedValue.ANIMATION_DURATION_QUICK);

	
	public LevelTranslationNode() {
		// add level listener Core.get().registry().
	} 
	
	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		if (!zPos.isDone(sgContext.frameNanoTime)){
			this.setTranslation(0, 0, zPos.get(sgContext.frameNanoTime));
		}
		return super.onTransform(sgContext);
	}


	@Override
	public void onAttached() {
//		levelsChanged();
	}
	
//	@Override
//	public void levelsChanged() {
//		sceneGraph.getRoot().queueOnceInGlThread(heightUpdater); 
//	}
//	
//	private IGlRunnable heightUpdater = new IGlRunnable() {
//		@Override
//		public void run(RenderContext rc) {
//			float nuZ = -Math.max(game.getScoringLevel(),0) * ((float)rc.surfaceWidth / (float)Game.BOARD_SIZE); 
//			zPos.set(nuZ);
//		}
//	};
//	
//	public float getTargetZ() {
//		return zPos.getTarget();
//	}
}
