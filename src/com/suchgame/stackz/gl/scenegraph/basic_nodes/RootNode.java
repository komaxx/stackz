package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.Node;

public class RootNode extends Node {
	public RootNode(){
		setName("ROOT");
		zLevel = Integer.MAX_VALUE - 1;
	}
	
	@Override
	protected void applyStateChangeRendering(RenderContext renderContext) {
		// do nothing!
	}
	
	@Override
	protected void applyStateChangeTransform(SceneGraphContext scContext) {
		// do nothing!
	}
}
