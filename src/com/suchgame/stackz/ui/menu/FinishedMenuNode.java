package com.suchgame.stackz.ui.menu;

import java.nio.ShortBuffer;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.StackzApplication;
import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Core.GameState;
import com.suchgame.stackz.core.listener.IGameStateListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.AnimatedBoundTexturedQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.ADelayedGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;

@SuppressLint("UseSparseArrays")
public class FinishedMenuNode extends Node implements IMenuNode, IGameStateListener {
	private static int[] drawableIds;
	static {
		drawableIds = new int[]{ 
				R.drawable.score_0,
				R.drawable.score_1, R.drawable.score_2, R.drawable.score_3,
				R.drawable.score_4, R.drawable.score_5, R.drawable.score_6,
				R.drawable.score_7, R.drawable.score_8, R.drawable.score_9,
				
				R.drawable.txt_finished,
				R.drawable.btn_brag,
				R.drawable.btn_again,
				
				R.drawable.label_score, R.drawable.label_high_score,
				R.drawable.txt_new_high_score
			};
	}
	
	
	private float interRowPadding = -1;			// will be set from resources 
	private float nuScoreSizeFactor = 1.3f;
	private float oldScoreSizeFactor = 0.9f;
	
	
	private HashMap<Integer, Rect> pxCoords = new HashMap<Integer, Rect>();
	private HashMap<Integer, RectF> uvCoords = new HashMap<Integer, RectF>();
	
	private MenuNode menuNode;

	private AnimatedBoundTexturedQuad[] quads = new AnimatedBoundTexturedQuad[50];
	private ShortBuffer indexBuffer;

	private AnimatedBoundTexturedQuad nuHighScoreQuad;
	
	private MenuButtonInterpreter bragBtnInterpreter;
	private MenuButtonInterpreter againBtnInterpreter;
	
	private boolean isHighScore;
	
	
	public FinishedMenuNode(MenuNode menuNode){
		this.menuNode = menuNode;
		
		draws = true;
		transforms = false;
		handlesInteraction = true;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		clusterIndex = ZLevels.CLUSTER_INDEX_MENU;
		zLevel = ZLevels.MENU;
		
		renderProgramIndex = RenderProgramStore.ALPHA_TEXTURED;
		
		useVboPainting = true;
		
		vbo = new Vbo(quads.length*TexturedQuad.VERTEX_COUNT, 
				Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(quads.length);
		
		for (int i = 0; i < quads.length; i++){
			quads[i] = new AnimatedBoundTexturedQuad();
			quads[i].setAlpha(0);
			quads[i].setAlphaAnimationDuration(2*InterpolatedValue.ANIMATION_DURATION_SLOW);
			quads[i].setPositionAnimationDuration(InterpolatedValue.ANIMATION_DURATION_NORMAL);
			quads[i].bindToVbo(vbo);
		}
		
		// take everything off the screen
		for (AnimatedBoundTexturedQuad q : quads) q.positionY(10000);
		
		bragBtnInterpreter = new MenuButtonInterpreter(quads[0], 
				new MenuButtonInterpreter.IMenuButtonClickListener() {
			@Override
			public void clicked(InteractionContext ic) {
				KoLog.i(this, "BRAG");
				// TODO
			}
		});
		againBtnInterpreter = new MenuButtonInterpreter(quads[1], 
				new MenuButtonInterpreter.IMenuButtonClickListener() {
			@Override
			public void clicked(InteractionContext ic) {
				KoLog.i(this, "AGAIN");
				FinishedMenuNode.this.menuNode.pausedQuitTapped(ic);
			}
		});
		
		Core.get().registry().addGameStateListener(this);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		interRowPadding = renderContext.resources.getDimension(R.dimen.finished_row_margin);
	}
	
	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		if (nuState == GameState.FINISHED){
			if (!visible){
				KoLog.i(this, "Now visible");
				isHighScore = Core.get().game().getScore() > StackzApplication.getHighScore();
				if (isHighScore) StackzApplication.setHighScore(Core.get().game().getScore());
				visible = true;
			}
			positionFromGame(sc);
		} else {
			hide(); 
			this.queueInGlThread(new ADelayedGlRunnable(this, 250) {
				@Override
				protected void doRun(RenderContext rc) {
					if (quads[0].getTargetAlpha() > 0) return;
					visible = false;
					KoLog.i(this, "Now invisible");
				}
			});
		}
	}
	
	private void hide() {
		for (AnimatedBoundTexturedQuad q : quads){
			q.setAlpha(0);
			q.positionY(1500);
		}
	}

	private void positionFromGame(SceneGraphContext sc) {
		float centerX = 0;
		float centerY = 0;
		
		float overallHeight = estimateOverallHeight();
		float rowBottomY = centerY - overallHeight/2;
		
		int quadIndex = 0;
		// bottom up, the buttons first
		// brag button
		AnimatedBoundTexturedQuad q = quads[quadIndex];
		Rect px = pxCoords.get(R.drawable.btn_brag);
		q.setTexCoordsUv(uvCoords.get(R.drawable.btn_brag));
		q.positionXY(centerX - px.width(), rowBottomY + px.height(), centerX, rowBottomY);
		quadIndex++;
		// again button
		q = quads[quadIndex]; 
		px = pxCoords.get(R.drawable.btn_again);
		q.setTexCoordsUv(uvCoords.get(R.drawable.btn_again));
		q.positionXY(centerX, rowBottomY + px.height(), centerX + px.width(), rowBottomY);
		quadIndex++;
		
		rowBottomY += px.height();
		rowBottomY += interRowPadding;
		
		// new highscore / old highscore
		if (isHighScore){
			q = nuHighScoreQuad = quads[quadIndex]; 
			px = pxCoords.get(R.drawable.txt_new_high_score);
			q.setTexCoordsPx(px);
			q.setTexCoordsUv(uvCoords.get(R.drawable.txt_new_high_score));
			q.positionXY(centerX - px.width()/2, rowBottomY + px.height(), centerX + px.width()/2, rowBottomY);
			quadIndex++;
			rowBottomY += px.height();
		} else {
			nuHighScoreQuad = null;
			int tmpScore = StackzApplication.getHighScore();
			float oldScoreWidth = measureWidth(tmpScore, oldScoreSizeFactor);
			// score label
			q = quads[quadIndex]; 
			px = pxCoords.get(R.drawable.label_high_score);
			q.setTexCoordsUv(uvCoords.get(R.drawable.label_high_score));
			q.positionXY(centerX - px.width()/2, rowBottomY + px.height(),
					centerX + px.width()/2, rowBottomY);
			quadIndex++;
			rowBottomY += px.height();
			
			// actual score
			float nowX = centerX + ((int)oldScoreWidth/2);
			int digit;
			int i = 1;
			do {
				digit = tmpScore % 10;
				px = pxCoords.get(drawableIds[digit]);
				float pxWidth = px.width()*oldScoreSizeFactor;
				float pxHeight = px.height()*oldScoreSizeFactor;
				
				q = quads[quadIndex];
				q.setTexCoordsUv(uvCoords.get(drawableIds[digit]));
				q.positionXY(nowX-pxWidth, rowBottomY + pxHeight, nowX, rowBottomY);
				
				nowX -= pxWidth;
				nowX -= 0;
				if (i%3==0) nowX -= 5;
				
				tmpScore = tmpScore / 10;
				i++;
				quadIndex++;
			} while (tmpScore > 0);
			
			rowBottomY += px.height() * oldScoreSizeFactor;
		}
		
		rowBottomY += interRowPadding;
		
		// finishing score
		float scoreWidth = measureWidth(Core.get().game().getScore(), nuScoreSizeFactor);
		// score label
		q = quads[quadIndex]; 
		px = pxCoords.get(R.drawable.label_score);
		q.setTexCoordsUv(uvCoords.get(R.drawable.label_score));
		q.positionXY(centerX - px.width()/2, rowBottomY + px.height(),
				centerX + px.width()/2, rowBottomY);
		quadIndex++;
		rowBottomY += px.height();
		// actual score
		int tmpScore = Core.get().game().getScore();
		float nowX = centerX + ((int)scoreWidth/2);
		int digit;
		int i = 1;
		do {
			digit = tmpScore % 10;
			px = pxCoords.get(drawableIds[digit]);
			float pxWidth = px.width()*nuScoreSizeFactor;
			float pxHeight = px.height()*nuScoreSizeFactor;
			
			q = quads[quadIndex];
			q.setTexCoordsUv(uvCoords.get(drawableIds[digit]));
			q.positionXY(nowX-pxWidth, rowBottomY + pxHeight, nowX, rowBottomY);
			
			nowX -= pxWidth;
			nowX -= 0;
			if (i%3==0) nowX -= 5;
			
			tmpScore = tmpScore / 10;
			i++;
			quadIndex++;
		} while (tmpScore > 0);
		
		rowBottomY += px.height() * nuScoreSizeFactor;
		rowBottomY += interRowPadding;
		
		// finished headline text
		q = quads[quadIndex]; 
		px = pxCoords.get(R.drawable.txt_finished);
		q.setTexCoordsUv(uvCoords.get(R.drawable.txt_finished));
		q.positionXY(centerX - px.width()/2, rowBottomY + px.height(), centerX + px.width()/2, rowBottomY);
		quadIndex++;
		
		
		// show all defined quads
		for (i = 0; i < quadIndex; i++){
			quads[i].setAlpha(1);
			quads[i].setVisible(true);
		}
		// hide inactive quads
		for (; i < quads.length; i++){
			quads[i].setAlphaDirect(0);
			quads[i].setVisible(false);
		}
	}

	private float estimateOverallHeight() {
		float ret = pxCoords.get(R.drawable.txt_finished).height();
		ret += interRowPadding; 
		
		ret += pxCoords.get(R.drawable.score_1).height() * nuScoreSizeFactor;
		ret += pxCoords.get(R.drawable.label_score).height();
		ret += interRowPadding;
		
		if (isHighScore){
			ret += pxCoords.get(R.drawable.txt_new_high_score).height();
		} else {
			ret += pxCoords.get(R.drawable.score_1).height() * oldScoreSizeFactor;
			ret += pxCoords.get(R.drawable.label_high_score).height();
		}
		ret += interRowPadding;
				
		ret += pxCoords.get(R.drawable.btn_again).height();
		
		return ret;
	}

	private float measureWidth(int score, float sizeFactor) {
		float ret = 0;
		int digit;
		int i = 1;
		Rect digitPx;
		do {
			digit = score % 10;
			digitPx = pxCoords.get(drawableIds[digit]);
			ret += digitPx.width() * sizeFactor;
			ret += 0;	// TODO make device independent
			if (i%3==0) ret += 5;
			score = score / 10;
			i++;
		} while(score > 0);
		
		return ret;
	}

	@Override
	protected boolean onInteraction(InteractionContext ic) {
		return 
				bragBtnInterpreter.onInteraction(ic) ||
				againBtnInterpreter.onInteraction(ic);
	}
	
	@Override
	public int[] getNecessaryResourceIds() {
		return drawableIds;
	}

	@Override
	public void setTexCoords(int textureHandle,
			HashMap<Integer, Rect> texCoordsPx,
			HashMap<Integer, RectF> texCoordsUv) {
		
		this.textureHandle = textureHandle;
		uvCoords.putAll(texCoordsUv);
		pxCoords.putAll(texCoordsPx);
	}

	@Override
	public boolean onRender(RenderContext renderContext) {
		int indexCount = 0;
		indexBuffer.position(0);
		
		if (nuHighScoreQuad != null){
			nuHighScoreQuad.setAlphaDirect(
					RenderUtil.sinStep(renderContext.frameNanoTime, 100, 0.4f, 1.0f)
					);
		}
		
		for (int i = 0; i < quads.length; i++){
			indexCount += quads[i].render(renderContext, indexBuffer);
		}
		
		renderContext.renderTexturedTriangles(0, indexCount, indexBuffer);
		return true;
	}
}
