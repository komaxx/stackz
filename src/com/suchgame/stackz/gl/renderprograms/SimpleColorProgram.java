package com.suchgame.stackz.gl.renderprograms;

import com.suchgame.stackz.gl.RenderProgram;

public class SimpleColorProgram extends RenderProgram {
	@Override
	protected void findHandles() {
		vertexXyzHandle = getAttributeHandle("vertexXYZ");
        vertexColorHandle = getAttributeHandle("vertexColor");
		
        matrixMVPHandle = getUniformHandle("uMVPMatrix");
	}

	@Override
	protected String getVertexShader() {
		return trivialVertexShader;
	}

	@Override
	protected String getFragmentShader() {
		return trivialFragmentShader;
	}

	private final String trivialVertexShader = 
		      "uniform mat4 uMVPMatrix;\n"
		    		  
			+ "attribute vec4 vertexXYZ;\n" 
			+ "attribute vec4 vertexColor;\n"
			
			+ "varying vec4 vVertexColor;\n"
			
			+ "void main() {\n"
			+ "  vVertexColor = vertexColor;\n"
			+ "  gl_Position = uMVPMatrix * vertexXYZ;\n"
			+ "}\n";

	private final String trivialFragmentShader = 
			"precision highp float;\n"
			
			+ "varying vec4 vVertexColor;\n"
			
			+ "void main() {\n"
			+ "  gl_FragColor = vVertexColor;\n"
			+ "}\n";
}
