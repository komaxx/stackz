package com.suchgame.stackz.gl.renderprograms;

import com.suchgame.stackz.gl.RenderProgram;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;

/**
 * A render-program store that contains all (and only) the inbuilt
 * RenderPrograms of the API.
 *  
 * @author Matthias Schicker
 */
public class DefaultRenderProgramStore extends ARenderProgramStore {
	@Override
	protected RenderProgram buildRenderProgram(int i) {
		// no additional programs!
		return null;
	}

	@Override
	protected int getAdditionalProgramsCount() {
		return 0;
	}
}