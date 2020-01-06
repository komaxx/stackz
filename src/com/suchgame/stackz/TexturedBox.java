package com.suchgame.stackz;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.bound_meshes.ABoundMesh;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;
import com.suchgame.stackz.model.BoxCoord;

public class TexturedBox extends ABoundMesh {
	public static final int MAX_VERTEX_COUNT = 8;
	
	public static final int FRONT_FACE = 1 << 0;
	public static final int TOP_FACE = 1 << 1;
	public static final int RIGHT_FACE = 1 << 2;
	public static final int BOTTOM_FACE = 1 << 3;
	public static final int LEFT_FACE = 1 << 4;
	public static final int BACK_FACE = 1 << 5;
	
	public static final int DEFAULT_FACES = FRONT_FACE | TOP_FACE | RIGHT_FACE | BOTTOM_FACE | LEFT_FACE;
	
	
	protected BoxCoord coord = new BoxCoord();
	
    protected float[] centerXY = new float[2];
    protected InterpolatedValue centerZ = 
    		new InterpolatedValue(AnimationType.LINEAR, 1000, InterpolatedValue.ANIMATION_DURATION_NORMAL);
    
    protected InterpolatedValue alpha = 
    		new InterpolatedValue(AnimationType.LINEAR, 1000, InterpolatedValue.ANIMATION_DURATION_NORMAL);
    protected boolean alphaDirty = true;
    
    
    protected int activeFaces = DEFAULT_FACES;
    
    protected float size = 1;
	protected boolean positionDirty = true;

	protected GlRect uvTexCoords = new GlRect(0,0,1,1);
	protected boolean texCoordsDirty = true;

	protected final FloatBuffer vertexBuffer = 
			ByteBuffer.allocateDirect(
					MAX_VERTEX_COUNT * Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES
	                ).order(ByteOrder.nativeOrder()).asFloatBuffer();

	
	public TexturedBox(){
		indexBuffer = createIndexBuffer();
		alpha.setDirect(1);
		offsetIndexBuffer();
	}
	
	public BoxCoord getCoord() {
		return coord;
	}
	
	public static ShortBuffer allocateIndices(int count) {
		return ByteBuffer.allocateDirect(
				count * 6 * 6 * RenderUtil.SHORT_SIZE_BYTES
                ).order(ByteOrder.nativeOrder()).asShortBuffer();
	}
	
	public void position(float x, float y, float z) {
		centerXY[0] = x;
		centerXY[1] = y;
		
		centerZ.setDirect(z + 20*size);
		centerZ.set(z);
		
        positionDirty = true;
	}
	
	public float getTargetAlpha() {
		return alpha.getTarget();
	}
	
	public void setAlphaDirect(float nuAlpha) {
		alpha.setDirect(nuAlpha);
		alphaDirty = true;
	}
	
	public void setAlpha(float nuAlpha){
		alpha.set(nuAlpha);
		alphaDirty = true;
	}
	
	public void setSize(float nuSize){
		size = nuSize;
		positionDirty = true;
	}
	
	public void setActiveFaces(int nuActiveFaces){
		this.activeFaces = nuActiveFaces;
		indexBuffer = createIndexBuffer();
		offsetIndexBuffer();
	}
	
	public float getSize() {
		return size;
	}

	public void setUvTexCoords(GlRect uvCoords){
		this.uvTexCoords.set(uvCoords);
		this.texCoordsDirty = true;
	}
	
	public void setUvTexCoords(float left, float top, float right, float bottom){
		this.uvTexCoords.set(left, top, right, bottom);
		this.texCoordsDirty = true;
	}
	
	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;
		
		boolean vboDirty = false;
		
		if (texCoordsDirty){
			Vertex.setUVMapping(vertexBuffer, 0, uvTexCoords.left, uvTexCoords.top);
			Vertex.setUVMapping(vertexBuffer, 1, uvTexCoords.left, uvTexCoords.bottom);
			Vertex.setUVMapping(vertexBuffer, 2, uvTexCoords.right, uvTexCoords.bottom);
			Vertex.setUVMapping(vertexBuffer, 3, uvTexCoords.right, uvTexCoords.top);
			
			Vertex.setUVMapping(vertexBuffer, 4, uvTexCoords.left, uvTexCoords.top);
			Vertex.setUVMapping(vertexBuffer, 5, uvTexCoords.left, uvTexCoords.bottom);
			Vertex.setUVMapping(vertexBuffer, 6, uvTexCoords.right, uvTexCoords.bottom);
			Vertex.setUVMapping(vertexBuffer, 7, uvTexCoords.right, uvTexCoords.top);
			
			texCoordsDirty = false;
			vboDirty = true;
		}
		
		if (positionDirty){
			float halfSize = size / 2f;
			
			float nowCenterZ = centerZ.get(rc.frameNanoTime);
			float z = nowCenterZ + halfSize;
			Vertex.positionTextured(vertexBuffer, 0, centerXY[0]-halfSize, centerXY[1]+halfSize, z);
			Vertex.positionTextured(vertexBuffer, 1, centerXY[0]-halfSize, centerXY[1]-halfSize, z);
			Vertex.positionTextured(vertexBuffer, 2, centerXY[0]+halfSize, centerXY[1]-halfSize, z);
			Vertex.positionTextured(vertexBuffer, 3, centerXY[0]+halfSize, centerXY[1]+halfSize, z);
			
			z = nowCenterZ - size*0.45f;
			Vertex.positionTextured(vertexBuffer, 4, centerXY[0]-halfSize, centerXY[1]+halfSize, z);
			Vertex.positionTextured(vertexBuffer, 5, centerXY[0]-halfSize, centerXY[1]-halfSize, z);
			Vertex.positionTextured(vertexBuffer, 6, centerXY[0]+halfSize, centerXY[1]-halfSize, z);
			Vertex.positionTextured(vertexBuffer, 7, centerXY[0]+halfSize, centerXY[1]+halfSize, z);
			
			positionDirty = !centerZ.isDone(rc.frameNanoTime);
			vboDirty = true;
		}
		
		if (alphaDirty){
			float alphaValue = alpha.get(rc.frameNanoTime); 
			for (int i = 0; i < 8; i++) Vertex.setAlpha(vertexBuffer, i, alphaValue);
			alphaDirty = !alpha.isDone(rc.frameNanoTime);
			vboDirty = true;
		}
		
		if (vboDirty){
			vertexBuffer.position(0);
			GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 
					firstByteIndex, 
					vertexBuffer.capacity()*4, 
					vertexBuffer);
			
			vboDirty = false;
		}
		
		frameIndexBuffer.put(indexBuffer);
		return indexBuffer.length;
	}

	@Override
	public int getMaxVertexCount() {
		return MAX_VERTEX_COUNT;
	}
	
	@Override
	public int getMaxIndexCount() {
		return indexBuffer.length;
	}
    
	protected short[] createIndexBuffer() {
		int faceCount = countActiveFaces();
		
		short[] ret = new short[6 * faceCount];
		
		int index = 0;
		
		// top
		if ((activeFaces&TOP_FACE) != 0){
			ret[index++] = 3;
			ret[index++] = 7;
			ret[index++] = 0;
			ret[index++] = 0;
			ret[index++] = 7;
			ret[index++] = 4;
		}
		
		// front
		if ((activeFaces&FRONT_FACE) != 0){
			ret[index++] = 0;
			ret[index++] = 1;
			ret[index++] = 3;
			ret[index++] = 3;
			ret[index++] = 1;
			ret[index++] = 2;
		}
		
		// right
		if ((activeFaces&RIGHT_FACE) != 0){
			ret[index++] = 2;
			ret[index++] = 6;
			ret[index++] = 3;
			ret[index++] = 3;
			ret[index++] = 6;
			ret[index++] = 7;
		}
		
		// bottom
		if ((activeFaces&BOTTOM_FACE) != 0){
			ret[index++] = 1;
			ret[index++] = 5;
			ret[index++] = 2;
			ret[index++] = 2;
			ret[index++] = 5;
			ret[index++] = 6;
		}
		
		// left
		if ((activeFaces&LEFT_FACE) != 0){
			ret[index++] = 0;
			ret[index++] = 4;
			ret[index++] = 1;
			ret[index++] = 1;
			ret[index++] = 4;
			ret[index++] = 5;
		}
		
		// NOTE: back is not used yet.
		
		return ret;
	}

	private int countActiveFaces() {
		return 
				((activeFaces&FRONT_FACE) == 0 ? 0 : 1) +
				((activeFaces&TOP_FACE) == 0 ? 0 : 1) +
				((activeFaces&RIGHT_FACE) == 0 ? 0 : 1) +
				((activeFaces&BOTTOM_FACE) == 0 ? 0 : 1) +
				((activeFaces&LEFT_FACE) == 0 ? 0 : 1) +
				((activeFaces&BACK_FACE) == 0 ? 0 : 1);
	}

	public void positionZ(float nuZ){
		centerZ.set(nuZ);
	}
	
	@Override
	public int render(RenderContext rc, FloatBuffer vertexBuffer, ShortBuffer frameIndexBuffer) {
		// TODO not yet done
		
		KoLog.w(this, "ONLY vbo painting implemented.");
		
		return 0;
	}
	
	@Override
	public String toString() {
		return coord.toString();
	}

	public void shortcutAnimation() {
		centerZ.shortcut();
	}

	public void getCenter(float[] ret) {
		ret[0] = centerXY[0];
		ret[1] = centerXY[1];
		ret[2] = centerZ.getTarget();
	}
}
