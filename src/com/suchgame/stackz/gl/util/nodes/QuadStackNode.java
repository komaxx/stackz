package com.suchgame.stackz.gl.util.nodes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.util.RenderUtil;

/**
 * Simple node for dev/debug purposes. 
 * Simply a stack of quads that can be given a start point and a direction
 * to visualize orientations. 
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public class QuadStackNode extends Node {
	private final int QUAD_COUNT = 10;

	private FloatBuffer quadsData;
	private ShortBuffer quadsIndices;

	private float size = 40;
	private float distance = 50;
	private float[] startPos = new float[3];
	private float[] direction = new float[]{ 0, 0, -1 }; 

	private boolean quadsPosDirty = true;
	
	
	public QuadStackNode() {
		this.draws = true;

		this.renderProgramIndex = ARenderProgramStore.SIMPLE_COLORED;
		this.blending = DEACTIVATE;
		this.depthTest = ACTIVATE;

		this.transforms = false;
		this.useVboPainting = false;

		this.clusterIndex = 5;
		this.zLevel = 1;
	}

	@Override
	public void onSurfaceCreated(RenderContext renderContext) {
		quadsData = ColorQuad.allocateColorQuads(QUAD_COUNT);
		quadsIndices = ColorQuad.allocateQuadIndices(QUAD_COUNT);
	}

	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		quadsPosDirty = true;
	}
	
	public void setDirection(float[] direction) {
		Vector.set3(this.direction, direction);
		Vector.normalize3(this.direction);
		
		quadsPosDirty = true;
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		if (quadsPosDirty){
			repositionQuads();
			quadsPosDirty = false;
		}
		
		quadsIndices.position(0);
		renderContext.renderColoredTriangles(0, QUAD_COUNT*ColorQuad.INDICES_COUNT, quadsData, quadsIndices);
		return true;
	}

	private void repositionQuads(){
		float[] color = new float[4];
		color[3] = 1;

		float[] nowPos = new float[3];
		Vector.set3(nowPos, startPos);
		
		float[] step = new float[3];
		Vector.scalarMultiply3(Vector.set3(step, direction), distance);

		for (int i = 0; i < QUAD_COUNT; i++){
			ColorQuad.position(quadsData, i * ColorQuad.QUAD_FLOATS, 
					nowPos[0]-size, nowPos[1]+size, nowPos[2], 
					nowPos[0]+size, nowPos[1]-size, nowPos[2]);
			
			RenderUtil.hsv2rgb(color, i * 20, 1, 1);
			ColorQuad.color(quadsData, i * ColorQuad.QUAD_FLOATS, color);
			
			Vector.addBtoA3(nowPos, step);
		}
	}
}
