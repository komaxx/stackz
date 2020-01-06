package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.GlCube;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.primitives.ColoredVertex;
import com.suchgame.stackz.gl.primitives.TexturedQuad;

public class BoundColorQuad extends ABoundMesh {
    protected final GlCube position = new GlCube();
	protected boolean positionDirty = true;

	protected float[] colorRgba = new float[]{0,0,1,1};
	protected boolean colorDirty = true;

	protected final FloatBuffer vertexBuffer = RenderConfig.VBO_RENDERING ? TexturedQuad.allocateQuads(1) : null;


	public BoundColorQuad(){
		indexBuffer = createIndexBuffer();
	}
	
	/**
	 * Reverses the triangle direction to make this quad visible from behind.
	 */
	public void reverse(){
		indexBuffer[1] = (short) (indexBuffer[0]+3);
		indexBuffer[2] = (short) (indexBuffer[0]+1);
		indexBuffer[4] = (short) (indexBuffer[0]+2);
		indexBuffer[5] = (short) (indexBuffer[0]+1);
	}
	
	public void positionXY(float left, float top, float right, float bottom) {
		position.setXY(left, top, right, bottom);
		positionDirty = true;
	}

	public void positionY(float top, float bottom) {
		position.setY(top, bottom);
		positionDirty = true;
	}
	
	public void positionX(float left, float right) {
		position.setX(left, right);
		positionDirty = true;
	}


	public void positionXY(GlRect nuPosition) {
		position.setXY(nuPosition);
		positionDirty = true;
	}

	public void position(GlCube nuPos) {
		position.set(nuPos);
        positionDirty = true;
	}
	
	public void position(float left, float top, float front, float right, float bottom, float back) {
		position.set(left, top, front, right, bottom, back);
        positionDirty = true;
    }
    
    public void positionZ(float front, float back) {
        position.setZ(front, back);
        positionDirty = true;
    }
    
    public void setColorRgba(float[] colorRgba) {
		this.colorRgba = colorRgba;
		colorDirty = true;
	}
    
	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;
		
		boolean vboDirty = false;
		
		if (colorDirty){
			ColorQuad.color(vertexBuffer, 0, colorRgba);
			colorDirty = false;
			vboDirty = true;
		}
		
		if (positionDirty){
			ColorQuad.position(vertexBuffer, 0, position);
			positionDirty = false;
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
		return TexturedQuad.INDICES_COUNT;
	}
	
	@Override
	public int render(RenderContext rc, FloatBuffer vb, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;

		int vbIndex = firstVertexIndex * ColoredVertex.STRIDE_FLOATS;
		
		if (colorDirty){
			ColorQuad.color(vb, vbIndex, colorRgba);
			colorDirty = false;
		}
		
		if (positionDirty){
			ColorQuad.position(vb, vbIndex, position);
			positionDirty = false;
		}
		
		frameIndexBuffer.put(indexBuffer);
		return TexturedQuad.INDICES_COUNT;
	}

	public boolean contains(float x, float y) {
		return position.containsXY(x, y);
	}
	
	public boolean containsY(float y) {
		return position.containsY(y);
	}

    public GlCube getPosition() {
        return this.position;
    }

	@Override
	public int getMaxVertexCount() {
		return TexturedQuad.VERTEX_COUNT;
	}

	@Override
	public int getMaxIndexCount() {
		return TexturedQuad.INDICES_COUNT;
	}
    
	protected static short[] createIndexBuffer() {
		return ColorQuad.allocateQuadIndexArray(1);
	}
}
