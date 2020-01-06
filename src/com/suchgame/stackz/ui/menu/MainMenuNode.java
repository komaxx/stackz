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
import com.suchgame.stackz.gl.scenegraph.ADelayedGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.scenegraph.interaction.Pointer;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.KoLog;

public class MainMenuNode extends Node implements IMenuNode, IGameStateListener {
	private static int[] drawableIds;
	static {
		drawableIds = new int[]{ R.drawable.menu_start };
	}
	
	private MenuNode menuNode;

	private AnimatedBoundTexturedQuad[] quads;
	private ShortBuffer indexBuffer;

	private MenuButtonInterpreter startBtnInterpreter;
	
	
	public MainMenuNode(MenuNode menuNode){
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
			quads[i].setAlpha(0);
			quads[i].setAlphaAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
			quads[i].bindToVbo(vbo);
		}
		
		startBtnInterpreter = new MenuButtonInterpreter(quads[0], 
				new MenuButtonInterpreter.IMenuButtonClickListener() {
			@Override
			public void clicked(InteractionContext ic) {
				MainMenuNode.this.menuNode.startTapped(ic);
			}
		});
		
		Core.get().registry().addGameStateListener(this);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		handleGameStateChanged(renderContext, GameState.INTRO, Core.get().getGameState());
	}
	
	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		if (interactionContext.getAction() == Pointer.DOWN){
			KoLog.i(this, "DOWN");
		}
		
		return startBtnInterpreter.onInteraction(interactionContext);
	}
	
	@Override
	public int[] getNecessaryResourceIds() {
		return new int[]{ R.drawable.menu_start };
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
	public void onSurfaceChanged(RenderContext renderContext) {
		positionWithCenter(0,0,false);
	}

	private void positionWithCenter(float x, float y, boolean direct) {
		Rect pxCoords = quads[0].getTexCoordsPx();
		
		quads[0].positionXY(
				x-pxCoords.width()/2, y+pxCoords.height()/2, 
				x+pxCoords.width()/2, y-pxCoords.height()/2);
		if (direct) quads[0].shortcutPosition();
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
	
	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		if (nuState == GameState.MAIN_MENU){
			quads[0].setAlpha(1);
			positionWithCenter(0, 0, false);
			if (!visible){
				KoLog.i(this, "Now visible");
				visible = true;
				interactive = true;
			}
		} else {
			quads[0].setAlpha(0);
			interactive = false;
			positionWithCenter(1000, 0, false);
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
}
