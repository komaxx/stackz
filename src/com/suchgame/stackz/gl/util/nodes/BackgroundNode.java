package com.suchgame.stackz.gl.util.nodes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

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
public class BackgroundNode extends Node {
	private final int drawableId;
	private final int size;
	private final float sizeFactor;

	private ShortBuffer quadsIndices;
	private FloatBuffer vertexBuffer;

	private Texture texture;
	private BoundTexturedQuad quad;


	/**
	 * @param size	Determines the tile-size in pixels. Smaller == faster.
	 * @param sizeFactor	1: One texture pixel == one screen pixel. 0.5: one texture pixel == 2 screen pixels. 
	 * Valuable to ensure that the background does not repeat to finely on high-dpi screens...
	 */
	public BackgroundNode(int drawableId, int size, float sizeFactor, int zLevel) {
		this.drawableId = drawableId;
		this.size = size;
		this.sizeFactor = sizeFactor;
		
		this.draws = true;
		this.handlesInteraction = false;
		this.transforms = false;
		
		this.renderProgramIndex = ARenderProgramStore.SIMPLE_TEXTURED;
		this.blending = DEACTIVATE;
		this.depthTest = DEACTIVATE;
		
		this.transforms = false;
		this.useVboPainting = false;
		this.zLevel = zLevel;

		this.vbo = new Vbo(TexturedQuad.VERTEX_COUNT,
				Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		this.vertexBuffer = vbo.buildVertexBuffer();
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
		tc.alphaChannel = false;
		tc.edgeBehavior = TextureConfig.EDGE_REPEAT;
		tc.minHeight = size;
		tc.minWidth = size;
		tc.mipMapped = false;
		
		tc.nearestMapping = true;
		
		texture = new Texture(tc);
		texture.create(renderContext);
		textureHandle = texture.getHandle();
	}

	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		int w = renderContext.surfaceWidth;
		int h = renderContext.surfaceHeight;
		
		Bitmap updateBmp = ((BitmapDrawable)renderContext.resources.getDrawable(drawableId)).getBitmap();
		updateBmp = Bitmap.createScaledBitmap(updateBmp, size, size, true);

		renderContext.bindTexture(textureHandle);
		texture.update(updateBmp, 0, 0);

		quad.positionXY(-w/2f, h/2f, w/2f, -h/2f);
		
		quad.setTexCoordsUv(0, 0, 
				(float)w/(float)texture.getWidth()    *sizeFactor, 
				(float)h/(float)texture.getHeight()   *sizeFactor, 
				false);
		
		vertexBuffer.position(0);
		quadsIndices.position(0);
		quad.render(renderContext, vertexBuffer, quadsIndices);
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		renderContext.renderTexturedTriangles(0, 6, vertexBuffer, quadsIndices);

		return true;
	}
}
