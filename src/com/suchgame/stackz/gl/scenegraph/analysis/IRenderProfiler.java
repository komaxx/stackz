package com.suchgame.stackz.gl.scenegraph.analysis;

import com.suchgame.stackz.gl.RenderContext;

public interface IRenderProfiler {

	void frameStart();

	void frameDone(RenderContext renderContext);

	void globalRunnablesStart();

	void globalRunnablesDone();

	void startPath(Path path);

	void pathDone(Path path);

}