package com.suchgame.stackz.gl.util.nodes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.RenderProgram;
import com.suchgame.stackz.gl.math.GlTrapezoid;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;
import com.suchgame.stackz.gl.scenegraph.Node;

/**
 * A node that contains random quads in the 0-plane.
 * 
 * @author Matthias Schicker
 */
public class UnitCubeNode extends Node{
	private FloatBuffer quadsData;
	private ShortBuffer quadsIndices;
	
	public UnitCubeNode(){
		draws = true;
		zLevel = 500;
		blending = DEACTIVATE;
		useVboPainting = false;

		setRenderProgramIndex(ARenderProgramStore.SIMPLE_COLORED);
	}
	
	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		recreateQuads();
	}
	
	private void recreateQuads(){
		quadsData = ColorQuad.allocateColorQuads(8);
		quadsIndices = ColorQuad.allocateQuadIndices(8);
		
		float size = 10.1f;
		
		ColorQuad.position(quadsData, 0 * ColorQuad.QUAD_FLOATS, -1, 1, 0, -1 + size, 1-size, 0);
		ColorQuad.color(quadsData, 0 * ColorQuad.QUAD_FLOATS, 1, 0, 0, 1);
		
		ColorQuad.position(quadsData, 1 * ColorQuad.QUAD_FLOATS, -1, -1, 0, -1 + size, -1+size, 0);
		ColorQuad.color(quadsData, 1 * ColorQuad.QUAD_FLOATS, 0, 1, 0, 1);
		
		ColorQuad.position(quadsData, 2 * ColorQuad.QUAD_FLOATS, 1, 1, 0, 1 - size, 1-size, 0);
		ColorQuad.color(quadsData, 2 * ColorQuad.QUAD_FLOATS, 0, 0, 1, 1);
		
		ColorQuad.position(quadsData, 3 * ColorQuad.QUAD_FLOATS, 1, -1, 0, 1 - size, -1+size, 0);
		ColorQuad.color(quadsData, 3 * ColorQuad.QUAD_FLOATS, 0, 1, 1, 1);
		
		
		
		ColorQuad.position(quadsData, 4 * ColorQuad.QUAD_FLOATS, -1, 1, -1, -1 + size, 1-size, -1);
		ColorQuad.color(quadsData, 4 * ColorQuad.QUAD_FLOATS, 0.5f, 0, 0, 1);
		
		ColorQuad.position(quadsData, 5 * ColorQuad.QUAD_FLOATS, -1, -1, -1, -1 + size, -1+size, -1);
		ColorQuad.color(quadsData, 5 * ColorQuad.QUAD_FLOATS, 0, 0.5f, 0, 1);
		
		ColorQuad.position(quadsData, 6 * ColorQuad.QUAD_FLOATS, 1, 1, -1, 1 - size, 1-size, -1);
		ColorQuad.color(quadsData, 6 * ColorQuad.QUAD_FLOATS, 0, 0, 0.5f, 1);
		
		ColorQuad.position(quadsData, 7 * ColorQuad.QUAD_FLOATS, 1, -1, -1, 1 - size, -1+size, -1);
		ColorQuad.color(quadsData, 7 * ColorQuad.QUAD_FLOATS, 0, 0.5f, 0.5f, 1);
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		GlTrapezoid t = renderContext.getZ0VisibleTrapezoid();
		
		float size = 10.0f;
		float padding = 1.0f;
		
		ColorQuad.position(quadsData, 0 * ColorQuad.QUAD_FLOATS, 
				t.ul[0] + padding, t.ul[1] - padding, 0, 
				t.ul[0] + padding + size, t.ul[1] - padding - size, 0);
		
		ColorQuad.position(quadsData, 1 * ColorQuad.QUAD_FLOATS,
				t.ll[0] + padding, t.ll[1] + padding + size, 0, 
				t.ll[0] + padding + size, t.ll[1] + padding, 0);
		
		ColorQuad.position(quadsData, 2 * ColorQuad.QUAD_FLOATS, 
				t.ur[0] - padding - size, t.ur[1] - padding, 0, 
				t.ur[0] - padding, t.ur[1] - padding - size, 0);
		
		ColorQuad.position(quadsData, 3 * ColorQuad.QUAD_FLOATS, 
				t.lr[0] - padding - size, t.lr[1] + padding + size, 0, 
				t.lr[0] - padding, t.lr[1] + padding, 0);
		
		
		RenderProgram currentRenderProgram = renderContext.currentRenderProgram;
        ColorQuad.renderColored(currentRenderProgram, 0, 8, quadsData, quadsIndices);

		return true;
	}
}
