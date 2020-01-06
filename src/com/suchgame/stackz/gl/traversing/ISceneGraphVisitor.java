package com.suchgame.stackz.gl.traversing;

import com.suchgame.stackz.gl.scenegraph.Node;

/**
 * Can be passed to a SceneGraphTraverser (broad first <-> depth first). The Traverser
 * will then execute the visitor for each Node in the graph.
 * 
 * @author Matthias Schicker
 */
public interface ISceneGraphVisitor {
	/**
	 * Return true, when the visitor should continue in this branch, false
	 * otherwise. <br/>
	 * <b>NOTE:</b> Must be executed in the same thread as all addChild / removeChild calls
	 * to nodes!
	 */
	boolean visitNode(Node node);
}