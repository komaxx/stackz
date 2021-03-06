package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.primitives.ColoredVertex;

/**
 * This is a simplification of the BoundFreeMesh. It has always four vertices.
 * 
 * @author Matthias Schicker
 */
public class BoundFreeColoredQuad extends ABoundMesh {
	public static final byte VERTEX_UPPER_LEFT = 0;
	public static final byte VERTEX_LOWER_LEFT = 1;
	public static final byte VERTEX_LOWER_RIGHT = 2;
	public static final byte VERTEX_UPPER_RIGHT = 3;
	
	protected final ColoredVertex[] vertices;
	
	protected boolean positionDirty = true;
	protected boolean colorDirty = true;

	protected final FloatBuffer vertexBuffer;
	
	
	public BoundFreeColoredQuad(){
		vertices = new ColoredVertex[ColorQuad.VERTEX_COUNT];
		for (int i = 0; i < ColorQuad.VERTEX_COUNT; i++) vertices[i] = new ColoredVertex();
		
		vertexBuffer = RenderConfig.VBO_RENDERING ? ColorQuad.allocateColorQuads(1) : null;
		indexBuffer = ColorQuad.allocateQuadIndexArray(1);
	}
	
	public void setAlpha(float alpha) {
		vertices[VERTEX_UPPER_LEFT].setAlpha(alpha);
		vertices[VERTEX_UPPER_RIGHT].setAlpha(alpha);
		vertices[VERTEX_LOWER_LEFT].setAlpha(alpha);
		vertices[VERTEX_LOWER_RIGHT].setAlpha(alpha);
		
		colorDirty = true;
	}
	
    public void setColor(float[] rgba) {
		vertices[VERTEX_UPPER_LEFT].setColorRGBA(rgba);
		vertices[VERTEX_UPPER_RIGHT].setColorRGBA(rgba);
		vertices[VERTEX_LOWER_LEFT].setColorRGBA(rgba);
		vertices[VERTEX_LOWER_RIGHT].setColorRGBA(rgba);
		
		colorDirty = true;
	}
    
	public void positionXY(byte vertex, float x, float y) {
		vertices[vertex].setPositionXY(x, y);
		positionDirty = true;
	}

	public void positionXYZ(byte vertex, float x, float y, float z) {
		vertices[vertex].setPosition(x, y, z);
		positionDirty = true;
	}

	/**
	 * Use this to place the quad along a vector.
	 */
	private static float[] tmpVector1 = new float[4]; 
	private static float[] tmpVector2 = new float[4]; 
	public void positionAlong2(float[] a, float[] b, float thickness){
		float[] aToB = Vector.aToB2(tmpVector1, a, b);
		float[] normal = Vector.normal2(tmpVector2, aToB);
		Vector.normalize2(normal);
		Vector.scalarMultiply2(normal, thickness*0.5f);
		
		Vector.aPlusB2(vertices[VERTEX_UPPER_LEFT].getPosition(), a, normal);
		Vector.aMinusB2(vertices[VERTEX_UPPER_RIGHT].getPosition(), a, normal);

		Vector.aPlusB2(vertices[VERTEX_LOWER_LEFT].getPosition(), b, normal);
		Vector.aMinusB2(vertices[VERTEX_LOWER_RIGHT].getPosition(), b, normal);
		
		positionDirty = true;
	}
	
	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;
		
		boolean vboDirty = false;
		
		if (positionDirty){
			int offset = 0;
			for (int i = 0; i < 4; i++){
				// upper left vertex
				vertices[i].writePosition(vertexBuffer, offset);
				offset += ColoredVertex.STRIDE_FLOATS;
			}
			positionDirty = false;
			vboDirty = true;
		}
		
		if (colorDirty){
			int offset = 0;
			for (int i = 0; i < 4; i++){
				// upper left vertex
				vertices[i].writeColor(vertexBuffer, offset);
				offset += ColoredVertex.STRIDE_FLOATS;
			}
			colorDirty = false;
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
		return 6;
	}
	
	@Override
	public int render(RenderContext rc, FloatBuffer vb, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;
		
		int vbIndex = firstVertexIndex * ColoredVertex.STRIDE_FLOATS;
		
		if (positionDirty){
			int offset = vbIndex;
			for (int i = 0; i < 4; i++){
				// upper left vertex
				vertices[i].writePosition(vertexBuffer, offset);
				offset += ColoredVertex.STRIDE_FLOATS;
			}
			positionDirty = false;
		}
		
		if (colorDirty){
			int offset = vbIndex;
			for (int i = 0; i < 4; i++){
				// upper left vertex
				vertices[i].writeColor(vertexBuffer, offset);
				offset += ColoredVertex.STRIDE_FLOATS;
			}
			colorDirty = false;
		}
		
		frameIndexBuffer.put(indexBuffer);
		return 6;
	}

	@Override
	public int getMaxVertexCount() {
		return ColorQuad.VERTEX_COUNT;
	}

	@Override
	public int getMaxIndexCount() {
		return ColorQuad.INDICES_COUNT;
	}

	public boolean contains(float[] xy) {
		return getBoundingBox().contains(xy) 
				//&& fineContains(xy)
				;
	}

	private static GlRect tmpBoundingBox = new GlRect();
	private GlRect getBoundingBox() {
		float[] position = vertices[0].getPosition();
		tmpBoundingBox.set(position[0], position[1], position[0], position[1]);
		tmpBoundingBox.enlargeToContain(vertices[1].getPosition());
		tmpBoundingBox.enlargeToContain(vertices[2].getPosition());
		tmpBoundingBox.enlargeToContain(vertices[3].getPosition());
		return tmpBoundingBox;
	}
}
