package com.suchgame.stackz.gl.traversing;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.scenegraph.Node;

/**
 * Used to visit the SceneGraph when the surface was created (newly).
 * 
 * @author Matthias Schicker
 */
public class SurfaceCreatedVisitor implements ISceneGraphVisitor {
	private RenderContext renderContext;

	@Override
	public boolean visitNode(Node node) {
		node.surfaceCreated(renderContext);
		return true;
	}

	public void setRenderContext(RenderContext frameRenderContext) {
		this.renderContext = frameRenderContext;
	}

}
