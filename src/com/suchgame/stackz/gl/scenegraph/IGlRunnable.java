package com.suchgame.stackz.gl.scenegraph;

import com.suchgame.stackz.gl.RenderContext;

/**
 * IGlRunnables can be scheduled in Nodes to be run when processing
 * the Node. Careful: The node must actually be processed, which may
 * not happen when the parent is, e.g., not visible!
 *  
 * @author Matthias Schicker
 */
public interface IGlRunnable {
	public void run(RenderContext rc);
}
