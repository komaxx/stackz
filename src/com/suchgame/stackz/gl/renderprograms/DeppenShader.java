package com.suchgame.stackz.gl.renderprograms;

import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderProgram;
import com.suchgame.stackz.gl.util.RenderUtil;

public class DeppenShader extends RenderProgram {
	@Override
	protected void findHandles() {
        vertexXyzHandle = GLES20.glGetAttribLocation(programHandle, "vertexXYZ");
        RenderUtil.checkGlError("glGetAttribLocation vertexXYZ");
        if (vertexXyzHandle == -1) {
            throw new RuntimeException("Could not get attrib location for position");
        }
		
        matrixMVPHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
		RenderUtil.checkGlError("glGetUniformLocation uMVPMatrix");
		if (matrixMVPHandle == -1) {
			throw new RuntimeException("Could not get uniform location for uMVPMatrix");
		}
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
			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * vertexXYZ;\n"
			+ "}\n";

	private final String trivialFragmentShader = 
			  "precision mediump float;\n"
			+ "void main() {\n"
			+ "  gl_FragColor = vec4(0.0, 0.0, 0.0, 0.2);" 
			+ "}\n";
}
