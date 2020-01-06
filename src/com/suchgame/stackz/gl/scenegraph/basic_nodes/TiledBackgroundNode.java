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
public class TiledBackgroundNode extends Node {
	private final int drawableId;
	private final int size;

	private ShortBuffer quadsIndices;
	private FloatBuffer vertexBuffer;

	private Texture texture;
	private BoundTexturedQuad quad;


	/**
	 * @param size	Determines the tile-size. Smaller == faster.
	 */
	public TiledBackgroundNode(int drawableId, int size, int zLevel) {
		this.drawableId = drawableId;
		this.size = size;
		this.draws = true;
		this.renderProgramIndex = ARenderProgramStore.SIMPLE_TEXTURED;
		this.blending = DEACTIVATE;
		this.depthTest = DEACTIVATE;
		this.transforms = false;
		this.zLevel = zLevel;

		this.vbo = new Vbo(TexturedQuad.VERTEX_COUNT,
				Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		if (!RenderConfig.VBO_RENDERING){
			vertexBuffer = vbo.buildVertexBuffer();
		}
		
		this.quadsIndices = TexturedQuad.allocateQuadIndices(1);
		this.quad = new BoundTexturedQuad();
		quad.bindToVbo(vbo);
	}

	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		recreateTexture(renderContext);
	}

	private void recreateTexture(RenderContext renderContext) {
		// create the texture
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.edgeBehavior = TextureConfig.EDGE_REPEAT;
		tc.minHeight = size;
		tc.minWidth = size;
		tc.mipMapped = false;
		
		tc.nearestMapping = true;
		
		texture = new Texture(tc);
		texture.create(renderContext);
		textureHandle = texture.getHandle();
		
		onSurfaceChanged(renderContext);
	}

	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		int w = renderContext.surfaceWidth;
		int h = renderContext.surfaceHeight;
		
		Bitmap updateBmp = ((BitmapDrawable)renderContext.resources.getDrawable(drawableId)).getBitmap();
		updateBmp = Bitmap.createScaledBitmap(updateBmp, size, size, true);

		renderContext.bindTexture(textureHandle);
		texture.update(updateBmp, 0, 0);

		renderContext.bindVBO(vbo.getHandle());
		quad.positionXY(0, 0, w, -h);
		quad.setTexCoordsUv(0, 0, 
				(float)w/(float)texture.getWidth(), 
				(float)h/(float)texture.getHeight(), false);
		quadsIndices.position(0);
		quad.render(renderContext, quadsIndices);

		quadsIndices.position(0);
		quad.render(renderContext, quadsIndices);
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
