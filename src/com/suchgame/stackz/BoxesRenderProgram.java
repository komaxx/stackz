package com.suchgame.stackz;

import com.suchgame.stackz.gl.RenderProgram;

public class BoxesRenderProgram extends RenderProgram {
	public static int gradientStartZHandle;
	public static int gradientEndZHandle;
	public static int gradientPhaseHandle;
	
	public static int depthTextureHandle;
	public static int textureHandle;
	
	
	@Override
	protected void findHandles() {
		vertexXyzHandle = getAttributeHandle("aPosition");
		vertexUvHandle = getAttributeHandle("aTextureCoord");
		vertexAlphaHandle = getAttributeHandle("aVertexColorDepth");
		
        matrixMVPHandle = getUniformHandle("uMVPMatrix");
        
        depthTextureHandle = getUniformHandle("uDepthTexture");
        textureHandle = getUniformHandle("uTexture");
        
        gradientStartZHandle = getUniformHandle("uGradientTexMinZ");
        gradientEndZHandle = getUniformHandle("uGradientTexMaxZ");
        
        gradientPhaseHandle = getUniformHandle("uGradientPhase");
	}

	@Override
	protected String getVertexShader() {
		return boxesVertexShader;
	}

	@Override
	protected String getFragmentShader() {
		return boxesFragmentShader;
	}
	
	
	
	private final String boxesVertexShader = 
			"uniform mat4 uMVPMatrix;\n"
		
			+ "uniform float uGradientTexMinZ;\n"
			+ "uniform float uGradientTexMaxZ;\n"
			+ "uniform float uGradientPhase;\n"

			+ "attribute vec4 aPosition;\n"
			+ "attribute float aVertexColorDepth;\n"
			+ "attribute vec2 aTextureCoord;\n"

			+ "varying vec2 vTextureCoord;\n"
			+ "varying vec2 vDepthTextureCoord;\n"

			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "  vTextureCoord = aTextureCoord;\n"
			+ "  vDepthTextureCoord = vec2(uGradientPhase, aVertexColorDepth / (uGradientTexMaxZ - uGradientTexMinZ));\n"
			+ "}\n";

	private final String boxesFragmentShader = 
			"precision mediump float;\n"

			+ "uniform sampler2D uTexture;\n"
			+ "uniform sampler2D uDepthTexture;\n"

			+ "varying vec2 vTextureCoord;\n"
			+ "varying vec2 vDepthTextureCoord;\n"

			+ "void main() {\n"
			+ "  gl_FragColor = texture2D(uTexture, vTextureCoord) * texture2D(uDepthTexture, vDepthTextureCoord);\n"
			+ "}\n";
	
	
	/*		Relays on texture sampling in vertex shader which not all chipsets support
	private final String boxesVertexShader = 
				"uniform mat4 uMVPMatrix;\n"
			
				+ "uniform float uGradientTexMinZ;\n"
				+ "uniform float uGradientTexMaxZ;\n"
				+ "uniform float uGradientPhase;\n"

				+ "uniform sampler2D uDepthTexture;\n"

				+ "attribute vec4 aPosition;\n"
				+ "attribute float aVertexColorDepth;\n"
				+ "attribute vec2 aTextureCoord;\n"

				+ "varying vec2 vTextureCoord;\n"
				+ "varying vec4 vVertexColor;\n"

				+ "void main() {\n"
				+ "  gl_Position = uMVPMatrix * aPosition;\n"
				+ "  vTextureCoord = aTextureCoord;\n"
				+ "  vVertexColor = texture2D(uDepthTexture, " +
								    "vec2(uGradientPhase, " +
								    	 "aVertexColorDepth / (uGradientTexMaxZ - uGradientTexMinZ)));\n"
//								    	 "smoothstep(uGradientTexMinZ, uGradientTexMaxZ, aVertexColorDepth)));\n"
				+ "}\n";

	private final String boxesFragmentShader = 
				"precision highp float;\n"

				+ "uniform sampler2D uTexture;\n"

				+ "varying vec2 vTextureCoord;\n"
				+ "varying vec4 vVertexColor;\n"

				+ "void main() {\n"
				+ "  gl_FragColor = texture2D(uTexture, vTextureCoord) * vVertexColor;\n"
				+ "}\n";
	//*/
}


