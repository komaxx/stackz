package com.suchgame.stackz.ui.menu;

import java.nio.ShortBuffer;
import java.util.HashMap;

import android.graphics.Rect;
import android.graphics.RectF;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Core.GameState;
import com.suchgame.stackz.core.listener.IGameStateListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.AnimatedBoundTexturedQuad;
import com.suchgame.stackz.gl.bound_meshes.BoundTexturedQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.Node;


public class ChromeScoreNode extends Node implements IMenuNode, IGameStateListener{
	private static final int MAX_SCORE_DIGITS = 8;
	private static final int QUAD_COUNT = MAX_SCORE_DIGITS + 1;		// 1 label

	private int score = 0;
	
	/**
	 * Note: quad0 is the 'score' label
	 */
	private AnimatedBoundTexturedQuad[] scoreQuads = new AnimatedBoundTexturedQuad[MAX_SCORE_DIGITS+1];		// +label
	private ShortBuffer indexBuffer;
	
	private Rect[] drawablePXs;
	private RectF[] drawableUVs;
	
	private boolean active = false;
	private boolean scoreDirty = true;
	
	
	private int[] drawableIds = new int[]{
		R.drawable.score_0,
		R.drawable.score_1, R.drawable.score_2, R.drawable.score_3,
		R.drawable.score_4, R.drawable.score_5, R.drawable.score_6,
		R.drawable.score_7, R.drawable.score_8, R.drawable.score_9,
		R.drawable.label_score, R.drawable.label_time
	};

	
	public ChromeScoreNode(){
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		zLevel = ZLevels.MENU;
		
		vbo = new Vbo(TexturedQuad.VERTEX_COUNT * QUAD_COUNT, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(QUAD_COUNT);
		
		for (int i = 0; i < scoreQuads.length; i++){
			scoreQuads[i] = new AnimatedBoundTexturedQuad();
			scoreQuads[i].position(2000, 2000, 0, 2000, 2000, 0);
			scoreQuads[i].shortcutPosition();
			scoreQuads[i].bindToVbo(vbo); 
		}
		
		Core.get().registry().addGameStateListener(this);
	}
	
	/**
	 * GlThread
	 */
	public void updateScore(int nuScore){
		if (nuScore == score) return;
		score = nuScore;
		scoreDirty = true;
	}
	
	@Override
	public int[] getNecessaryResourceIds() {
		return drawableIds;
	}
	
	@Override
	public void setTexCoords(int textureHandle, 
			HashMap<Integer, Rect> texCoordsPx, HashMap<Integer, RectF> texCoordsUv) {
		
		this.textureHandle = textureHandle;
		
		drawablePXs = new Rect[drawableIds.length];
		drawableUVs = new RectF[drawableIds.length];
		
		for (int i = 0; i < drawableIds.length; i++){
			drawablePXs[i] = texCoordsPx.get(drawableIds[i]);
			drawableUVs[i] = texCoordsUv.get(drawableIds[i]);
		}
		
		scoreQuads[0].setTexCoordsPx(texCoordsPx.get(R.drawable.label_score));
		scoreQuads[0].setTexCoordsUv(texCoordsUv.get(R.drawable.label_score));
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		if (scoreDirty){
			resetNumberQuads(renderContext);
			scoreDirty = false;
		}
		
		int indexCount = 0;
		indexBuffer.position(0);
		for (AnimatedBoundTexturedQuad q : scoreQuads) indexCount += q.render(renderContext, indexBuffer); 
		
		Vertex.renderTexturedTriangles(
				renderContext.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}
	
	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		active = (nuState == GameState.RUNNING || nuState == GameState.PAUSED || nuState == GameState.COUNTING_DOWN); 
		scoreDirty = true;
	}
	
	private static float[] tmpAnchor = new float[2];
	private void resetNumberQuads(RenderContext renderContext) {
		placeScoreQuads(renderContext);
	}

	private void placeScoreQuads(RenderContext renderContext) {
		Vector.set2(tmpAnchor, renderContext.surfaceWidth/2 - 10 - drawablePXs[10].width(),
				active ? 
						renderContext.surfaceHeight/2 - /*TODO*/ 75 : 
						renderContext.surfaceHeight/2 + 75);

		float labelX = tmpAnchor[0];
		scoreQuads[0].positionXY(labelX, tmpAnchor[1] + drawablePXs[10].height(),
				labelX + drawablePXs[10].width(), tmpAnchor[1]);
		
		int digit;
		int tmpNumber = score;
		int i = 1;
		BoundTexturedQuad quad;
		Rect digitPx;
		do {
			digit = tmpNumber % 10;
			quad = scoreQuads[i];
			
			quad.setVisible(true);
			quad.setTexCoordsUv(drawableUVs[digit]);
			
			digitPx = drawablePXs[digit];
			quad.positionXY(tmpAnchor[0]-digitPx.width(), tmpAnchor[1] + digitPx.height(), tmpAnchor[0], tmpAnchor[1]);

			tmpAnchor[0] -= digitPx.width() - /*TODO*/ 5;
			if (i%3==0) tmpAnchor[0] -= 5;
			tmpNumber = tmpNumber / 10;
			i++;
		} while(i < scoreQuads.length && tmpNumber > 0);

		// hide the unused quads
		for (; i < scoreQuads.length; i++) scoreQuads[i].setVisible(false);
	}
}
