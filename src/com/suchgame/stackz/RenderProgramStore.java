package com.suchgame.stackz;

import com.suchgame.stackz.gl.RenderProgram;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;

/**
 * @author
 * Created by Matthias Schicker on 12/17/13.
 */
public class RenderProgramStore extends ARenderProgramStore {
	public static final int BOXES_PROGRAM = FIRST_CUSTOM_RENDER_PROGRAM;
	public static final int UNIFORM_ALPHA_TEXTURED = FIRST_CUSTOM_RENDER_PROGRAM+1;
	
    @Override
    protected RenderProgram buildRenderProgram(int i) {
        if (i == BOXES_PROGRAM){
        	return new BoxesRenderProgram();
        } else if (i == UNIFORM_ALPHA_TEXTURED){
        	return new UniformAlphaTexturedProgram();
        }
        return null;
    }

    @Override
    protected int getAdditionalProgramsCount() {
        return 2;
    }
}
