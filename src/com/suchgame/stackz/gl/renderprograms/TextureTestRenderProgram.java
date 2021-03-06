package com.suchgame.stackz.gl.renderprograms;

import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderProgram;
import com.suchgame.stackz.gl.util.RenderUtil;

public class TextureTestRenderProgram extends RenderProgram {
	@Override
	protected void findHandles() {
        vertexXyzHandle = GLES20.glGetAttribLocation(programHandle, "aPosition");
        RenderUtil.checkGlError("glGetAttribLocation aPosition");
        if (vertexXyzHandle == -1) {
            throw new RuntimeException("Could not get attrib location for position");
        }
        vertexUvHandle = GLES20.glGetAttribLocation(programHandle, "aTextureCoord");
        RenderUtil.checkGlError("glGetAttribLocation aTextureCoord");
        if (vertexUvHandle == -1) {
            throw new RuntimeException("Could not get attrib location for textureCoord");
        }
		
        matrixMVPHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
		RenderUtil.checkGlError("glGetUniformLocation uMVPMatrix");
		if (matrixMVPHandle == -1) {
			throw new RuntimeException("Could not get uniform location for uMVPMatrix");
		}
	}

	@Override
	protected String getVertexShader() {
		return textureTestVertexShader;
	}

	@Override
	protected String getFragmentShader() {
		return textureTestFragmentShader;
	}

	private final String textureTestVertexShader = 
		      "uniform mat4 uMVPMatrix;\n"
			
			+ "attribute vec4 aPosition;\n"
			+ "attribute vec2 aTextureCoord;\n"
			
			+ "varying vec2 vTextureCoord;\n"
			
			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "  vTextureCoord = aTextureCoord;\n"
			+ "}\n";

	private final String textureTestFragmentShader = 
			  "precision highp float;\n"
			
			+ "uniform sampler2D sTexture;\n"
			+ "varying vec2 vTextureCoord;\n"
			
			+ "void main() {\n"
			+ "  vec4 baseColor = texture2D(sTexture, vTextureCoord) * 0.8;\n"
			+ "  baseColor.r = vTextureCoord.x * 0.8;\n"
			+ "  baseColor.b = vTextureCoord.y * 0.8;\n"
			+ "  gl_FragColor = baseColor;\n"
			+ "}\n";
}
