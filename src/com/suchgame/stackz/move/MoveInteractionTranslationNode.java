package com.suchgame.stackz.move;

import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.TranslationNode;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;
import com.suchgame.stackz.model.BoxCoord;
import com.suchgame.stackz.model.GameOld.IGameListener;
import com.suchgame.stackz.model.Square;

public class MoveInteractionTranslationNode extends TranslationNode implements IGameListener{
	private Game2 game;

	private InterpolatedValue zPos = new InterpolatedValue(AnimationType.OVERBUMP, 500);
	
	public MoveInteractionTranslationNode(Game2 game) {
		this.game = game;
		
//		this.game.addListener(this);
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
		levelsChanged();
	}
	
	@Override
	public void levelsChanged() {
		sceneGraph.getRoot().queueOnceInGlThread(heightUpdater); 
	}
	
	private IGlRunnable heightUpdater = new IGlRunnable() {
		@Override
		public void run(RenderContext rc) {
//			float nuZ = (game.getLowestUnfilledLevel() - 3f) * ((float)rc.surfaceWidth / (float)Game.BOARD_SIZE); 
//			zPos.set(nuZ);
		}
	};

	@Override
	public void currentMoveChanged() {
		// don't care
	}

	@Override
	public void gameStateChanged() { /* nothing */ }
	
	@Override
	public void scored(Square[] nuSquares, BoxCoord[] scoredCoords) { /* don't care */}
}
