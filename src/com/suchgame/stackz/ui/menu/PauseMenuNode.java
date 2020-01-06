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
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.util.InterpolatedValue;

public class PauseMenuNode extends Node implements IMenuNode, IGameStateListener {
	private static int[] drawableIds;
	static {
		drawableIds = new int[]{ 
				R.drawable.btn_pause, 
				R.drawable.txt_paused,
				R.drawable.btn_quit,
				R.drawable.btn_resume 
			};
	}
	
	private MenuNode menuNode;

	private AnimatedBoundTexturedQuad[] quads;
	private ShortBuffer indexBuffer;

	private MenuButtonInterpreter pauseBtnInterpreter;
	private MenuButtonInterpreter quitBtnInterpreter;
	private MenuButtonInterpreter resumeBtnInterpreter;
	
	
	public PauseMenuNode(MenuNode menuNode){
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
		
		vbo = new Vbo(drawableIds.length*TexturedQuad.VERTEX_COUNT, 
				Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(drawableIds.length);
		
		quads = new AnimatedBoundTexturedQuad[drawableIds.length];
		for (int i = 0; i < drawableIds.length; i++){
			quads[i] = new AnimatedBoundTexturedQuad();
			quads[i].setAlpha(1);
			quads[i].setAlphaAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
			quads[i].bindToVbo(vbo);
		}
		
		// take everything off the screen
		for (AnimatedBoundTexturedQuad q : quads) q.positionY(10000);
		
		pauseBtnInterpreter = new MenuButtonInterpreter(quads[0], 
				new MenuButtonInterpreter.IMenuButtonClickListener() {
			@Override
			public void clicked(InteractionContext ic) {
				PauseMenuNode.this.menuNode.pauseTapped(ic);
			}
		});
		quitBtnInterpreter = new MenuButtonInterpreter(quads[2], 
				new MenuButtonInterpreter.IMenuButtonClickListener() {
			@Override
			public void clicked(InteractionContext ic) {
				PauseMenuNode.this.menuNode.pausedQuitTapped(ic);
			}
		});
		resumeBtnInterpreter = new MenuButtonInterpreter(quads[3], 
				new MenuButtonInterpreter.IMenuButtonClickListener() {
			@Override
			public void clicked(InteractionContext ic) {
				PauseMenuNode.this.menuNode.pausedResumeTapped(ic);
			}
		});
		
		Core.get().registry().addGameStateListener(this);
	}
	
	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		if (nuState == GameState.PAUSED){
			// show pause menu
			positionCenter(quads[1], 0, quads[1].getTexCoordsPx().height());
			positionCenter(quads[2], -quads[2].getTexCoordsPx().width()/2, -quads[2].getTexCoordsPx().height()/2);
			positionCenter(quads[3], quads[3].getTexCoordsPx().width()/2, -quads[3].getTexCoordsPx().height()/2);
		} else {
			// hide pause menu
			positionCenter(quads[1], 0, 1200);
			positionCenter(quads[2], -1200, -quads[2].getTexCoordsPx().height()/2);
			positionCenter(quads[3],  1200, -quads[3].getTexCoordsPx().height()/2);
		}

		if (nuState == GameState.RUNNING){
			// show pause button
			positionCenter(quads[0], 
					-sc.surfaceWidth/2 + quads[0].getTexCoordsPx().width()/2,
					sc.surfaceHeight/2 - quads[0].getTexCoordsPx().height()/2);
		} else {
			// hide pause button
			positionCenter(quads[0], 
					-sc.surfaceWidth/2 + quads[0].getTexCoordsPx().width()/2,
					1200);
		}
	}
	
	private static void positionCenter(AnimatedBoundTexturedQuad q, float cX, float cY) {
		float halfWidth = q.getTexCoordsPx().width()/2f;
		float halfHeight = q.getTexCoordsPx().height()/2f;
		q.positionXY(cX - halfWidth, cY + halfHeight, cX + halfWidth, cY - halfHeight);
	}

	@Override
	protected boolean onInteraction(InteractionContext ic) {
		return 
				pauseBtnInterpreter.onInteraction(ic) ||
				quitBtnInterpreter.onInteraction(ic) ||
				resumeBtnInterpreter.onInteraction(ic);
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
		for (int i = 0; i < drawableIds.length; i++){
			quads[i].setTexCoordsUv(texCoordsUv.get(drawableIds[i]));
			quads[i].setTexCoordsPx(texCoordsPx.get(drawableIds[i]));
		}
	}

	@Override
	public boolean onRender(RenderContext renderContext) {
		int indexCount = 0;
		indexBuffer.position(0);
		
		for (int i = 0; i < quads.length; i++){
			indexCount += quads[i].render(renderContext, indexBuffer);
		}
		
		renderContext.renderTexturedTriangles(0, indexCount, indexBuffer);
		return true;
	}
}
