package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.bound_meshes.BoundTexturedQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.texturing.Texture;
import com.suchgame.stackz.gl.texturing.TextureConfig;

/**
 * Just paints a screen filling quad.
 * 
 * @author Matthias Schicker
 */
public class StretchBackgroundNode extends Node {
	private final int drawableId;

	private FloatBuffer vertexBuffer;
	private ShortBuffer quadsIndices;

	private Texture texture;
	private BoundTexturedQuad quad;


	/**
	 * @param size	Determines the tile-size. Smaller == faster.
	 */
	public StretchBackgroundNode(int drawableId) {
		this.drawableId = drawableId;
		this.draws = true;
		this.renderProgramIndex = ARenderProgramStore.SIMPLE_TEXTURED;
		this.blending = DEACTIVATE;
		this.depthTest = DEACTIVATE;
		this.transforms = false;

		this.vbo = new Vbo(TexturedQuad.VERTEX_COUNT,
				Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		vertexBuffer = this.vbo.buildVertexBuffer();
		this.quadsIndices = TexturedQuad.allocateQuadIndices(1);
		this.quad = new BoundTexturedQuad();
		quad.bindToVbo(vbo);
	}

	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		recreateTexture(renderContext);
	}

	private void recreateTexture(RenderContext renderContext) {
		Bitmap updateBmp = ((BitmapDrawable)renderContext.resources.getDrawable(drawableId)).getBitmap();

		int minSize = Math.max(updateBmp.getWidth(), updateBmp.getHeight());
		
		// create the texture
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.edgeBehavior = TextureConfig.EDGE_REPEAT;
		tc.minHeight = minSize;
		tc.minWidth = minSize;
		tc.mipMapped = false;
		
		tc.nearestMapping = true;
		
		texture = new Texture(tc);
		texture.create(renderContext);
		textureHandle = texture.getHandle();

		texture.update(updateBmp, 0, 0);

		quad.setTexCoordsUv(0, 0, 
				(float)updateBmp.getWidth()/(float)texture.getWidth(), 
				(float)updateBmp.getHeight()/(float)texture.getHeight(), false);
		
		onSurfaceChanged(renderContext);
	}

	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		int w = renderContext.surfaceWidth;
		int h = renderContext.surfaceHeight;
		
		renderContext.bindVBO(vbo.getHandle());
		quad.positionXY(0, 0, w, -h);
		quadsIndices.position(0);
		if (RenderConfig.VBO_RENDERING){
			quad.render(renderContext, quadsIndices);
		} else {
			quad.render(renderContext, vertexBuffer, quadsIndices);
		}
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		if (RenderConfig.VBO_RENDERING){
			renderContext.renderTexturedTriangles(0, 6, quadsIndices);
		} else {
			renderContext.renderTexturedTriangles(0, 6, vertexBuffer, quadsIndices);
		}

		return true;
	}
}
