package com.suchgame.stackz.gl.scenegraph.analysis;

import com.suchgame.stackz.gl.RenderContext;


/**
 * Dummy-profiler to be used when no profiling is necessary. Does nothing at all.
 * 
 * @author Matthias Schicker
 */
public class DummyProfiler implements IRenderProfiler {

	@Override
	public void frameStart() {}

	@Override
	public void frameDone(RenderContext rc) {}

	@Override
	public void globalRunnablesStart() {}

	@Override
	public void globalRunnablesDone() {}

	@Override
	public void startPath(Path path) {}

	@Override
	public void pathDone(Path path) {}

}
