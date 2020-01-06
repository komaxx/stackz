package com.suchgame.stackz.gl.util.nodes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.bound_meshes.BoundTexturedQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;
import com.suchgame.stackz.gl.scenegraph.Node;

/**
 * A node that contains just one textured quad. Exposes everything to control the rendering
 * of the quad. 
 * 
 * @author Matthias Schicker
 */
public class TexturedQuadNode extends Node {
	private FloatBuffer quadsData;
	private ShortBuffer quadsIndices;
	
	private boolean twoSided = true;
	
	private BoundTexturedQuad quad = new BoundTexturedQuad();

	public TexturedQuadNode() {
		this.draws = true;
		this.renderProgramIndex = ARenderProgramStore.SIMPLE_TEXTURED;
		this.blending = DEACTIVATE;
		this.transforms = false;
		this.useVboPainting = false;
		
		vbo = new Vbo(4, Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		quadsData = vbo.buildVertexBuffer();
		quadsIndices = TexturedQuad.allocateQuadIndices(2);
		
		quad.bindToVbo(vbo);
	}

	@Override
	public boolean onRender(RenderContext renderContext) {
		quadsIndices.position(0);
		
		if (RenderConfig.VBO_RENDERING){
			quad.render(renderContext, quadsIndices);
			renderContext.renderTexturedTriangles(0, 6, quadsIndices);
			if (twoSided){
				quad.putReversedIndices(quadsIndices);
				renderContext.renderTexturedTriangles(0, 12, quadsIndices);
			} else {
				renderContext.renderTexturedTriangles(0, 6, quadsIndices);
			}
		} else {
			quad.render(renderContext, quadsData, quadsIndices);
			if (twoSided){
				quad.putReversedIndices(quadsIndices);
				renderContext.renderTexturedTriangles(0, 12, quadsData, quadsIndices);
			} else {
				renderContext.renderTexturedTriangles(0, 6, quadsData, quadsIndices);
			}
		}

		return true;
	}

	public BoundTexturedQuad getQuad() {
		return quad;
	}
}
