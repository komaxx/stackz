package com.suchgame.stackz.gl;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.os.Build;

import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.ARenderProgramStore;
import com.suchgame.stackz.gl.texturing.TextureStore;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;


/**
 * A RenderContext is an object that will be available for every node when
 * traversing through the scene graph. 
 * It contains the current state of the GL and other states that are necessary 
 * for rendering.
 * 
 * @author Matthias Schicker
 */
public class RenderContext extends SceneGraphContext {
	public Resources resources;
	
	public TextureStore textureStore;
	public RenderProgram currentRenderProgram;
	public ARenderProgramStore renderProgramStore;

	public int currentRenderProgramIndex = -1;
	public int boundTexture = -1;
	public int boundVboId = -1;

	
	/**
	 * indicates whether the current MVP-matrix is not loaded into the current shader
	 */
	private boolean mvpMatrixInShader = false;
	
	/**
	 * Specifies whether depth testing is currently active or not. Will be set by
	 * nodes during rendering.
	 */
	public boolean depthTestActivated;
	
	/**
	 * The currently enabled depth testing function (GLES20 constant). No effect when
	 * depth test is deactivated.
	 */
	public int depthFunction;

	/**
	 * Specifies whether blending is currently active or not. Will be set by nodes
	 * during rendering.
	 */
	public boolean blendingActivated;
	
	/**
	 * The currently enabled blending function (GLES20 constant). Only to change the
	 * destination blend function, the source blend function is always SRC_ALPHA.
	 * No effect when blending not activated.
	 */
	public int blendFunction;
	
	/**
	 * Will be incremented whenever a new surface is created.
	 */
	public int surfaceId = -1;
	
	
	public RenderContext(ARenderProgramStore renderProgramStore) {
		this.renderProgramStore = renderProgramStore;
	}

	/**
	 * resets this context to the values of the provided context.
	 */
	public final void reset(RenderContext basicRenderContext) {
		basicReset(basicRenderContext);

		resources = basicRenderContext.resources;
		textureStore = basicRenderContext.textureStore;
		surfaceId = basicRenderContext.surfaceId;
		
		depthTestActivated = false;
		activateDepthTest(true);
		
		blendingActivated = true;
		activateBlending(false);
		
		depthFunction = -1;
		
		boundTexture = -1;
		boundVboId = -1;
		resetRenderProgram();
	}

	/**
	 * Makes sure that no old RenderProgram will be reused. To be called
	 * on onSurfaceCreated. 
	 */
	public final void resetRenderProgram(){
		currentRenderProgramIndex = -1;
		currentRenderProgram = null;
	}
	
	/**
	 * Discards all bound textures. Typically called after a surface changes
	 */
	public final void resetTextureStore() {
		textureStore.reset();
	}
	
	/**
	 * Activates a different RenderProgram.
	 * 
	 * @param renderProgramIndex	The renderProgram (referenced per index)
	 * to activate.
	 */
	public final void switchRenderProgram(int renderProgramIndex) {
		if (renderProgramIndex == currentRenderProgramIndex) return;
		renderProgramStore.activateRenderProgram(renderProgramIndex);
		currentRenderProgramIndex = renderProgramIndex;
		currentRenderProgram = renderProgramStore.getRenderProgram(renderProgramIndex);
		mvpMatrixInShader = false;
	}

	@Override
	public void setMvpMatrixDirty() {
		super.setMvpMatrixDirty();
		mvpMatrixInShader = false;
	}
	
	public boolean isMvpMatrixInShader() {
		return mvpMatrixInShader;
	}
	
	/**
	 * Normally, users of the scene graph do not have to call this. Only when
	 * RenderPrograms are switched manually, this needs to be called - otherwise
	 * all render commands will most likely fail.
	 */
	public void applyMvpMatrixToShader(){
		if (mvpMatrixInShader) return;
    	GLES20.glUniformMatrix4fv(
    			currentRenderProgram.matrixMVPHandle, 1, false, getMvpMatrix(), 0);
    	mvpMatrixInShader = true;
	}
	
	public final void bindVBO(int vboId) {
		if (vboId != boundVboId){
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
			boundVboId = vboId;
		}
	}
	
	public final void bindTexture(int textureHandle){
		if (textureHandle != boundTexture){
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
			boundTexture = textureHandle;
			
			if (RenderConfig.DEGRADE_TEXTURE_FILTERING_ON_NOT_IDLE){
				if (lastLastFrameWasIdle && !lastFrameWasIdle){
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, 
								GLES20.GL_NEAREST);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, 
							GLES20.GL_NEAREST);
				} else if (!lastLastFrameWasIdle && lastFrameWasIdle){
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, 
							GLES20.GL_LINEAR);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, 
							GLES20.GL_LINEAR);
				}
			}
		}
	}

	public void activateDepthTest(boolean b) {
		if (b != depthTestActivated){
			if (b) GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			else GLES20.glDisable(GLES20.GL_DEPTH_TEST);
			depthTestActivated = b;
		}
	}
	
	public void activateBlending(boolean activateBlending){
		if (activateBlending != blendingActivated){
			if (activateBlending){
				GLES20.glEnable(GLES20.GL_BLEND);
				if (RenderConfig.GL_DEBUG) RenderUtil.checkGlError("enable blending");
			} else {
				GLES20.glDisable(GLES20.GL_BLEND);
				if (RenderConfig.GL_DEBUG)RenderUtil.checkGlError("disable blending");
			}
			blendingActivated = activateBlending;
		}
	}
	
	public void setBlendFunction(int nuBlendFunction){
		if (nuBlendFunction != blendFunction){
			GLES20.glBlendFunc(GLES20.GL_ONE, nuBlendFunction);
			blendFunction = nuBlendFunction;
		}
	}
	
	public void setDepthFunction(int nuDepthFunction) {
		if (nuDepthFunction != depthFunction){
			GLES20.glDepthFunc(nuDepthFunction);
			depthFunction = nuDepthFunction;
		}
	}

	
	// //////////////////////////////////////////////////////////////////////////
	// render functions
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public void renderTexturedTriangles(int firstIndex, int indexCount, ShortBuffer indexBuffer) {
		if (!RenderConfig.VBO_RENDERING){
			KoLog.w(this, "Tried VBO rendering although turned off / not available! Ignored.");
			return;
		}
		
		// position
		GLES20.glVertexAttribPointer(currentRenderProgram.vertexXyzHandle, 3, GLES20.GL_FLOAT, false, 
				Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, 0);
		GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexXyzHandle);

		// textured
		if (currentRenderProgram.vertexUvHandle != -1){
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexUvHandle, 2, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, 
					Vertex.TEXTURED_VERTEX_DATA_UV_OFFSET * RenderUtil.FLOAT_SIZE_BYTES);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexUvHandle);
		}

		// set the float buffer to be used as alpha source
		if (currentRenderProgram.vertexAlphaHandle != -1){
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexAlphaHandle, 1, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, 
					Vertex.TEXTURED_VERTEX_DATA_ALPHA_OFFSET * RenderUtil.FLOAT_SIZE_BYTES);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexAlphaHandle);
		}

		// set the quad float buffer to be used as texture index source (if available in RenderProgram)
		if (currentRenderProgram.vertexTextureIndexHandle != -1){
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexTextureIndexHandle, 1, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, 
					Vertex.TEXTURED_VERTEX_DATA_TEXTURE_INDEX_OFFSET * RenderUtil.FLOAT_SIZE_BYTES);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexTextureIndexHandle);
		} 

		indexBuffer.position(firstIndex);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
	}

	/**
	 * Sets the attribute pointers for the current renderProgram (which should
	 * be one of the texturing render programs!!) and paints it. The appropriate
	 * texture should already be bound previously.
	 */
	public boolean renderTexturedTriangles(  
			int firstIndex, int renderedIndexCount, FloatBuffer vertexData, ShortBuffer vertexIndices) {

		// set the float buffer to be used as position source
		vertexData.position(0);
		GLES20.glVertexAttribPointer(
				currentRenderProgram.vertexXyzHandle, 
				3, GLES20.GL_FLOAT, false, Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, vertexData);
		GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexXyzHandle);

		// set the float buffer to be used as colorSource
		if (currentRenderProgram.vertexUvHandle != -1){
			vertexData.position(Vertex.TEXTURED_VERTEX_DATA_UV_OFFSET);
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexUvHandle, 2, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, vertexData);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexUvHandle);
		}

		// set the float buffer to be used as alpha source
		if (currentRenderProgram.vertexAlphaHandle != -1){
			vertexData.position(Vertex.TEXTURED_VERTEX_DATA_ALPHA_OFFSET);
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexAlphaHandle, 1, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, vertexData);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexAlphaHandle);
		}

		// set the quad float buffer to be used as texture index source (if available in RenderProgram)
		if (currentRenderProgram.vertexTextureIndexHandle != -1){
			vertexData.position(Vertex.TEXTURED_VERTEX_DATA_TEXTURE_INDEX_OFFSET);
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexTextureIndexHandle, 1, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, vertexData);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexTextureIndexHandle);
		} else if(currentRenderProgram.vertexPulseIntensityHandle != -1){
			vertexData.position(Vertex.TEXTURED_VERTEX_DATA_PULSE_INTENSITY);
			GLES20.glVertexAttribPointer(
					currentRenderProgram.vertexPulseIntensityHandle, 1, GLES20.GL_FLOAT, false,
					Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES, vertexData);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexPulseIntensityHandle);
		}

		vertexData.position(0);
		vertexIndices.position(firstIndex);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 
				renderedIndexCount, GLES20.GL_UNSIGNED_SHORT, vertexIndices);

		return true;
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public void renderColoredTriangles(int firstIndex, int indexCount, ShortBuffer indexBuffer) {
		if (!RenderConfig.VBO_RENDERING){
			KoLog.w(this, "Tried VBO rendering although turned off / not available! Ignored.");
			return;
		}

		GLES20.glVertexAttribPointer(currentRenderProgram.vertexXyzHandle, 3, GLES20.GL_FLOAT, false, 
				Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, 0);
		GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexXyzHandle);

		if (currentRenderProgram.vertexColorHandle != -1){
			GLES20.glVertexAttribPointer(currentRenderProgram.vertexColorHandle, 4, GLES20.GL_FLOAT, false, 
					Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, RenderUtil.FLOAT_SIZE_BYTES*3);
			GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexColorHandle);
		}

		indexBuffer.position(firstIndex);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
	}

    public void renderColoredLines(int firstIndex, int indexCount, ShortBuffer indexBuffer) {
        GLES20.glVertexAttribPointer(currentRenderProgram.vertexXyzHandle, 3, GLES20.GL_FLOAT, false,
                Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, 0);
        GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexXyzHandle);

        GLES20.glVertexAttribPointer(currentRenderProgram.vertexColorHandle, 4, GLES20.GL_FLOAT, false,
                Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, RenderUtil.FLOAT_SIZE_BYTES*3);
        GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexColorHandle);

        indexBuffer.position(firstIndex);
        GLES20.glDrawElements(GLES20.GL_LINES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }
    
    public void renderColoredLines(int firstIndex, int indexCount, FloatBuffer vertexData, ShortBuffer indexBuffer) {
    	vertexData.position(0);
        GLES20.glVertexAttribPointer(
        		currentRenderProgram.vertexXyzHandle, 
        		3, GLES20.GL_FLOAT, false, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, vertexData);
        GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexXyzHandle);
        
        // set the float buffer to be used as colorSource
        if (currentRenderProgram.vertexColorHandle != -1){
	        vertexData.position(Vertex.COLOR_VERTEX_DATA_COLOR_OFFSET);
	        GLES20.glVertexAttribPointer(
	        		currentRenderProgram.vertexColorHandle, 4, GLES20.GL_FLOAT, false,
	        		Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, vertexData);
	        GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexColorHandle);
        }

        indexBuffer.position(firstIndex);
        GLES20.glDrawElements(GLES20.GL_LINES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }
	
	/**
	 * The non-vbo version to draw colored triangles.
	 */
	public boolean renderColoredTriangles(  
			int firstIndex, int indexCount, FloatBuffer vertexData, ShortBuffer vertexIndices) {
		
		vertexData.position(0);
        GLES20.glVertexAttribPointer(
        		currentRenderProgram.vertexXyzHandle, 
        		3, GLES20.GL_FLOAT, false, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, vertexData);
        GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexXyzHandle);
        
        // set the quad float buffer to be used as colorSource
        if (currentRenderProgram.vertexColorHandle != -1){
	        vertexData.position(Vertex.COLOR_VERTEX_DATA_COLOR_OFFSET);
	        GLES20.glVertexAttribPointer(
	        		currentRenderProgram.vertexColorHandle, 4, GLES20.GL_FLOAT, false,
	        		Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES, vertexData);
	        GLES20.glEnableVertexAttribArray(currentRenderProgram.vertexColorHandle);
        }

        vertexIndices.position(firstIndex);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, vertexIndices);
		
		return true;
	}
}
