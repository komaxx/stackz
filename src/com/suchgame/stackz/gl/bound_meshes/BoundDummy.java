package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.suchgame.stackz.gl.RenderContext;

/**
 * Dummy implementation of the IBoundMesh. Will draw nothing.
 * 
 * @author Matthias Schicker
 */
public class BoundDummy extends ABoundMesh {
	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer) {
		return 0;
	}

	@Override
	public int getMaxVertexCount() {
		return 0;
	}

	@Override
	public int getMaxIndexCount() {
		return 0;
	}

	@Override
	public int render(RenderContext rc, FloatBuffer vertexBuffer, ShortBuffer frameIndexBuffer) {
		return 0;
	}
}
