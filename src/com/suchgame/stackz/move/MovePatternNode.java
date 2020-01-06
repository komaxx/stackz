package com.suchgame.stackz.move;

import java.nio.ShortBuffer;

import android.opengl.GLES20;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.TexturedBox;
import com.suchgame.stackz.UniformAlphaTexturedProgram;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.SceneGraph;
import com.suchgame.stackz.gl.texturing.TextureStore.ResourceTexture;
import com.suchgame.stackz.gl.util.RenderUtil;
import com.suchgame.stackz.model.Move;
import com.suchgame.stackz.model.PatternCoord;
import com.suchgame.stackz.ui.GameView;

public class MovePatternNode extends Node {
	private static final int MAX_BOXES = 4;
	
	private Move move;
	
	/**
	 * currently not rendered boxes.
	 */
	private TexturedBox[] boxes = new TexturedBox[MAX_BOXES];
	
	private int pipelineIndex = 99;
	
	// /////////////////////////////////////////////////////
	// plumbing, tmps, caches
	private ShortBuffer indexBuffer;
	
	private int textureHandlePossible;
	private int textureHandleImpossible;
	
	
	public MovePatternNode(){
        draws = true;
        transforms = false;
        handlesInteraction = false;

        renderProgramIndex = RenderProgramStore.UNIFORM_ALPHA_TEXTURED;
        depthTest = ACTIVATE;
        scissorTest = DONT_CARE;

        blending = ACTIVATE;
        zLevel = ZLevels.MOVE;
        clusterIndex = ZLevels.CLUSTER_INDEX_MOVES;
        
        useVboPainting = true;
        vbo = new Vbo(MAX_BOXES * TexturedBox.MAX_VERTEX_COUNT, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES);
        indexBuffer = TexturedBox.allocateIndices(MAX_BOXES);
        
        for (int i = 0; i < boxes.length; i++){
        	TexturedBox box = new TexturedBox();
        	box.bindToVbo(vbo);
        	box.setVisible(false);
        	boxes[i] = box;
        }
	}
	
	public void setMove(Move move) {
		if (move == this.move) return;
		
		this.move = move;
		queueBoxRepositioning();
	}
	
	private void queueBoxRepositioning() {
		this.queueInGlThread(new IGlRunnable() {
			@Override
			public void run(RenderContext rc) {
				positionBoxes(rc);
			}
		});
	}

	protected void positionBoxes(RenderContext rc) {
		float boxSize = GameView.getBoxSize(rc); 
		for (TexturedBox b : boxes) b.setSize(boxSize);
		
		if (move == null) return;
		
		float lowerLeftX = (1-move.getBasicSizeX()) * boxSize / 2.0f;
		float lowerLeftY = (1-move.getBasicSizeY()) * boxSize / 2.0f;
		
		for (int i = 0; i < move.getBasicCoords().length; i++){
			PatternCoord c = move.getBasicCoords()[i];
			boxes[i].position(lowerLeftX + c.x * boxSize, lowerLeftY + c.y * boxSize, boxSize);
			boxes[i].setVisible(true);
			
			// remove hidden faces
			boxes[i].setActiveFaces(findActiveFaces(i, move.getBasicCoords()));
		}
	}

	private static int findActiveFaces(int index, PatternCoord[] basicCoords) {
		int ret = TexturedBox.DEFAULT_FACES; 
			
		PatternCoord coord = basicCoords[index];
		
		for (int i = 0; i < basicCoords.length; i++){
			if (i == index) continue;
			PatternCoord nowCoord = basicCoords[i];

			if (nowCoord.y == coord.y){
				if (nowCoord.x == coord.x-1) ret = ret & (~TexturedBox.LEFT_FACE);
				if (nowCoord.x == coord.x+1) ret = ret & (~TexturedBox.RIGHT_FACE);
			} else if (nowCoord.x == coord.x){
				if (nowCoord.y == coord.y-1) ret = ret & (~TexturedBox.BOTTOM_FACE);
				if (nowCoord.y == coord.y+1) ret = ret & (~TexturedBox.TOP_FACE);
			}
		}
		
		return ret;
	}

	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		ResourceTexture resourceTexture = renderContext.textureStore.getResourceTexture(renderContext, R.raw.move_possible, true);
		this.textureHandlePossible = resourceTexture.getHandle();
		
		resourceTexture = renderContext.textureStore.getResourceTexture(renderContext, R.raw.move_impossible, true);
		this.textureHandleImpossible = resourceTexture.getHandle();
		
		this.textureHandle = textureHandlePossible;
	}

	@Override
    public void onSurfaceChanged(RenderContext renderContext) {
		queueBoxRepositioning();
    }

    @Override
    public boolean onRender(RenderContext rc) {
    	float alpha = 1;
    	if (pipelineIndex == 0){
    		alpha = RenderUtil.sinStep(rc.frameNanoTime, 250, 0.8f, 1.2f);
    	};
    	GLES20.glUniform1f(UniformAlphaTexturedProgram.alphaHandle, alpha);
    	
    	indexBuffer.position(0);
    	int indexCount = 0;
    	for (TexturedBox box : boxes){
    		indexCount += box.render(rc, indexBuffer);
    	}
    	
    	rc.renderTexturedTriangles(0, indexCount, indexBuffer);
    	
        return true;
    }

	public SceneGraph getSceneGraph() {
		return sceneGraph;
	}

	public void abort() {
		for (int i = 0; i < MAX_BOXES; i++){
			boxes[i].setSize(0);
		}
	}

	public void done() {
		for (int i = 0; i < MAX_BOXES; i++){
			boxes[i].positionZ(boxes[i].getSize());
			boxes[i].setSize(0);
		}
	}

	public void setPipelineIndex(int index) {
		this.pipelineIndex = index;
		
		this.textureHandle = move.isPossible() ? textureHandlePossible : textureHandleImpossible;
		
		blending = pipelineIndex==0 ? DEACTIVATE : ACTIVATE;
	}
}
