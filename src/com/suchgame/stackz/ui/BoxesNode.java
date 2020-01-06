package com.suchgame.stackz.ui;

import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.opengl.GLES20;

import com.suchgame.stackz.BoxesRenderProgram;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.TexturedBox;
import com.suchgame.stackz.core.Box;
import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.core.listener.IBoxesListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.texturing.Texture;
import com.suchgame.stackz.gl.texturing.TextureConfig;
import com.suchgame.stackz.gl.util.KoLog;

public class BoxesNode extends Node implements IBoxesListener {
	private static final int MAX_BOXES = Game2.BOARD_SIZE * Game2.BOARD_SIZE * (Game2.MAX_LEGAL_HEIGHT+1) + 4;
	
	/**
	 * currently not rendered boxes.
	 */
	private ArrayDeque<UiBox> boxPool = new ArrayDeque<UiBox>();
	
	private HashMap<String, UiBox> boxesById = new HashMap<String, UiBox>();
	
	private UiBoxTextures textures;
	
	// /////////////////////////////////////////////////////
	// plumbing, tmps, caches
	private float defaultBoxSize;
	private ShortBuffer indexBuffer;

	private boolean renderBoxesDirty = true;
	private ArrayList<UiBox> renderBoxes = new ArrayList<UiBox>();
	
	private HashMap<String, UiBox> toRemove = new HashMap<String, UiBox>(); 
	
	
	public BoxesNode(Context c){
		textures = new UiBoxTextures(c);
		
        draws = true;
        transforms = false;
        handlesInteraction = false;

        renderProgramIndex = RenderProgramStore.BOXES_PROGRAM;
        depthTest = ACTIVATE;
        scissorTest = DONT_CARE;

        blending = DEACTIVATE;
        zLevel = ZLevels.GAME_BOXES;
        
        useVboPainting = true;
        vbo = new Vbo(MAX_BOXES * TexturedBox.MAX_VERTEX_COUNT, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES);
        indexBuffer = TexturedBox.allocateIndices(MAX_BOXES);
        
        for (int i = 0; i < MAX_BOXES; i++){
        	UiBox box = new UiBox(textures);
        	box.bindToVbo(vbo);
        	boxPool.push(box);
        }
        
        Core.get().registry().addBoxesListener(this);
	}
	
	private IGlRunnable updateJob = new IGlRunnable() {
		@Override
		public void run(RenderContext rc) { update(rc); }
	};
	public void update(SceneGraphContext sc){
		toRemove.clear();
		toRemove.putAll(boxesById);
		ArrayList<Box> coreBoxes = Core.get().game().getBoxes();
		for (Box b : coreBoxes){
			toRemove.remove(b.id);
			
			UiBox uiBox = boxesById.get(b.id);
			if (uiBox == null) uiBox = addUiBox(b, sc);
			uiBox.update(sc, b, defaultBoxSize);
		}
		
		Set<String> keySet = toRemove.keySet();
		for (String id : keySet){
			removeUiBox(id);
		}
	}

	private void removeUiBox(String id) {
		boxPool.add(boxesById.remove(id));
		renderBoxesDirty = true;
	}

	private UiBox addUiBox(Box b, SceneGraphContext sc) {
		if (boxPool.isEmpty()){
			KoLog.e(this, "F*CK!! Out of boxes!");
			return null;
		}
		
		UiBox nuBox = boxPool.pop();
		nuBox.reset();
		nuBox.setSize(defaultBoxSize);
		nuBox.positionZDirect(15*defaultBoxSize);
		boxesById.put(b.id, nuBox);
		renderBoxesDirty = true;
		
		return nuBox;
	}

	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		this.textureHandle = textures.create(renderContext);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1);
		
		Bitmap depthPattern = createDepthPattern(renderContext);
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = false;
		tc.minHeight = depthPattern.getWidth();
		tc.minWidth = depthPattern.getHeight();
		tc.nearestMapping = true;
		tc.edgeBehavior = TextureConfig.EDGE_REPEAT;
		Texture t = new Texture(tc);
		t.create(renderContext);
		t.update(depthPattern, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	}

	private static Bitmap createDepthPattern(RenderContext rc) {
		int[] depthColors = new int[8];
		for (int i = 0; i < depthColors.length; i++){
			depthColors[i] = Color.HSVToColor(new float[]{ 
					(180 + (430/depthColors.length*i)) % 360, 
					1, 1});
		}
		
		depthColors = new int[]{
			0xFF1599ad, 0xFFAA66B0, 0xFFAA99AA, 0xFF666666,
			0xFFfb17ff, 0xFFff2008, 0xFFffe600, 0xFF16bf00
		};
		
		Bitmap basicBmp = Bitmap.createBitmap(1, 2*depthColors.length, Config.ARGB_8888);
		
		// paint the unscored colors to texture bitmap 
		for (int i = 1; i < 2*depthColors.length; i += 2) basicBmp.setPixel(0, i, depthColors[i/2]);
		// paint the scored colors to texture bitmap
		for (int i = 0; i < 2*depthColors.length; i += 2) basicBmp.setPixel(0, i, makeInactiveColor( depthColors[i/2] ));
		
		Bitmap ret = Bitmap.createScaledBitmap(basicBmp, 256, 256, false);
		return ret;
	}

	private static float[] hsv = new float[4];
	private static int makeInactiveColor(int color) {
		Color.colorToHSV(color, hsv);
		hsv[1] *= 0.68f;
		
		return Color.HSVToColor(hsv);
	}

	@Override
    public void onSurfaceChanged(RenderContext renderContext) {
        defaultBoxSize = GameView.getBoxSize(renderContext); 
        queueInGlThread(updateJob);
    }

	@Override
	public void handleBoxesChanged(SceneGraphContext sc) {
		queueInGlThread(updateJob);
	}
	
    @Override
    public boolean onRender(RenderContext renderContext) {
    	if (renderBoxesDirty){
    		renderBoxes.clear();
    		renderBoxes.addAll(boxesById.values());
    		renderBoxesDirty = false;
    	}
    	
    	// register secondary texture
    	GLES20.glUniform1i(BoxesRenderProgram.depthTextureHandle, 1);
    	
    	// set the depth texture scaling
//    	depthBaseZ.set(Math.max(0, (game.getLowestUnfilledLevel()-1)) * defaultBoxSize - 0.25f*defaultBoxSize);
//    	float baseZ = depthBaseZ.get(renderContext.frameNanoTime);
    	
    	float startZ = 8f;
    	float endZ = 0f;
    	
    	GLES20.glUniform1f(BoxesRenderProgram.gradientStartZHandle, startZ);
    	GLES20.glUniform1f(BoxesRenderProgram.gradientEndZHandle, endZ);
    	GLES20.glUniform1f(BoxesRenderProgram.gradientPhaseHandle, 
    			(renderContext.frameNanoTime%3000000000L) / 3000000000f);
    	
    	indexBuffer.position(0);
    	int indexCount = 0;
    	for (UiBox box : renderBoxes){
    		indexCount += box.render(renderContext, indexBuffer);
    	}
    	
    	renderContext.renderTexturedTriangles(0, indexCount, indexBuffer);
    	
        return true;
    }
}
