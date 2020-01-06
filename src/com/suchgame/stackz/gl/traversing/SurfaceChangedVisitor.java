package com.suchgame.stackz.gl.traversing;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.scenegraph.Node;

/**
 * Used to visit the SceneGraph when the surface has changed.
 * 
 * @author Matthias Schicker
 */
public class SurfaceChangedVisitor implements ISceneGraphVisitor {
	private RenderContext frameRenderContext;

	@Override
	public boolean visitNode(Node node) {
		node.surfaceChanged(frameRenderContext);
		return true;
	}

	public void reset(RenderContext frameRenderContext) {
		this.frameRenderContext = frameRenderContext;
	}
}
