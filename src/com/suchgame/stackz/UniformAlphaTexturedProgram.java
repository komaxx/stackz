package com.suchgame.stackz;

import com.suchgame.stackz.gl.RenderProgram;

public class UniformAlphaTexturedProgram extends RenderProgram {
	public static int alphaHandle;
	
	@Override
	protected void findHandles() {
		vertexXyzHandle = getAttributeHandle("aPosition");
		vertexUvHandle = getAttributeHandle("aTextureCoord");
		
        matrixMVPHandle = getUniformHandle("uMVPMatrix");
        
        alphaHandle = getUniformHandle("uAlpha");
	}

	@Override
	protected String getVertexShader() {
		return vertexShader;
	}

	@Override
	protected String getFragmentShader() {
		return fragmentShader;
	}

	private final String vertexShader = 
			  "uniform mat4 uMVPMatrix;\n"
			
			+ "attribute vec4 aPosition;\n"
			+ "attribute vec2 aTextureCoord;\n"
			
			+ "varying vec2 vTextureCoord;\n"
			
			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "  vTextureCoord = aTextureCoord;\n"
			+ "}\n";

	private final String fragmentShader = 
			  "precision highp float;\n"
			
			+ "uniform float uAlpha;\n"
			+ "uniform sampler2D sTexture;\n"
			
			+ "varying vec2 vTextureCoord;\n"
			
			+ "void main() {\n"
			+ "  gl_FragColor = texture2D(sTexture, vTextureCoord) * uAlpha;\n"
			+ "}\n";

}


