package com.suchgame.stackz.ui.menu;

import java.nio.ShortBuffer;
import java.util.HashMap;

import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Core.GameState;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.core.listener.IGameStateListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.AnimatedBoundTexturedQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.ADelayedGlRunnable;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.AnimatedRotationNode;

public class CountDownNode extends AnimatedRotationNode implements IMenuNode, IGameStateListener {
	private int[] drawableIds = new int[] {
			R.drawable.countdown_3, R.drawable.countdown_2, R.drawable.countdown_1, R.drawable.countdown_go
	};

	private MenuNode menuNode;

	private AnimatedBoundTexturedQuad[] quads;
	private ShortBuffer indexBuffer;
	
	private int currentStep = -1;

	
	public CountDownNode(MenuNode menuNode){
		this.menuNode = menuNode;
		
		draws = true;
		transforms = true;
		handlesInteraction = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		clusterIndex = ZLevels.CLUSTER_INDEX_MENU;
		zLevel = ZLevels.MENU;
		
		renderProgramIndex = RenderProgramStore.ALPHA_TEXTURED;
		
		useVboPainting = true;
		
		vbo = new Vbo(4*TexturedQuad.VERTEX_COUNT, Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(4);
		
		quads = new AnimatedBoundTexturedQuad[4];
		for (int i = 0; i < 4; i++){
			quads[i] = new AnimatedBoundTexturedQuad();
			quads[i].bindToVbo(vbo);
			quads[i].setVisible(false);
		}
		
		this.setRotationDirect(-90f, 0, 1, 0);
		
		Core.get().registry().addGameStateListener(this);
	}
	
	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		if (nuState == GameState.COUNTING_DOWN) start(sc);
	}
	
	public void start(SceneGraphContext sc){
		this.currentStep = -1;
		this.visible = true;
		for (int i = 0; i < quads.length; i++) quads[i].setVisible(false);
		repositionQuads();
		toNextStep(sc);
	}
	
	protected void toNextStep(SceneGraphContext sc) {
		this.currentStep++;

		if (currentStep > drawableIds.length){
			this.visible = false;
			return;
		} else if (currentStep == drawableIds.length){
			menuNode.countDownDone(sc);
			quads[3].position(0, 0, 0, 0, 0, 0);
		} else {
			if (currentStep == 0){
				this.setRotationDirect(-90);
			}
			this.setRotation(this.currentStep * 180f);
			
			// make the right quads visible
			for (int i = 0; i < currentStep-1; i++) quads[i].setVisible(false);
			quads[currentStep].setVisible(true);
		}
		
		scheduleNextStep();
	}
	private void scheduleNextStep() {
		this.queueInGlThread(new ADelayedGlRunnable(this, Game2.COUNT_DOWN_MS/4) {
			@Override
			protected void doRun(RenderContext rc) {
				toNextStep(rc);
			}
		});
	}

	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		repositionQuads();
	}
	
	private void repositionQuads() {
		for (int i = 0; i < drawableIds.length; i++){
			Rect texCoordsPx = quads[i].getTexCoordsPx();
			quads[i].position(
					(i%2==0?-1:1)* texCoordsPx.width()/2, texCoordsPx.height() / 2, 0, 
					(i%2==0?1:-1)* texCoordsPx.width()/2, -texCoordsPx.height() / 2, 0);
			if (i>0) quads[i].shortcutPosition();
		}
		quads[3].positionZ(-100, -100);
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
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		
		int indexCount = 0;
		indexBuffer.position(0);
		
		for (int i = 0; i < 4; i++){
			indexCount += quads[i].render(renderContext, indexBuffer);
		}
		
		renderContext.renderTexturedTriangles(0, indexCount, indexBuffer);
		
		return true;
	}
}
