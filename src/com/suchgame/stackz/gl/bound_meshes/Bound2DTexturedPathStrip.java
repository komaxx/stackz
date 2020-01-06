package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.graphics.RectF;
import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.TexturedVertex;

public class Bound2DTexturedPathStrip extends ABoundMesh {
	protected final int vertexCount;
	protected final int indexCount;

	protected final TexturedVertex[] vertices;

	protected boolean positionDirty = true;
	protected boolean texCoordsDirty = true;
	protected boolean alphaDirty = true;

	protected final FloatBuffer vertexBuffer;

	/**
	 * Builds a strip that goes along the given path. The strip will be <code>distance</code>
	 * wide.
	 */
	public Bound2DTexturedPathStrip(float[][] verticesPath, float[] stripSideVector, float distance){
		this.vertexCount = verticesPath.length * 2;
		this.indexCount = (verticesPath.length-1) * 2 * 3;	// two triangles per segment

		vertices = new TexturedVertex[vertexCount];
		for (int i = 0; i < vertexCount; i++) vertices[i] = new TexturedVertex();

		vertexBuffer = RenderConfig.VBO_RENDERING ? TexturedVertex.allocate(vertexCount) : null;
		indexBuffer = new short[indexCount];
		
		buildStrip(verticesPath, stripSideVector, distance);
	}

	private void buildStrip(float[][] verticesPath, float[] stripSideVector, float distance) {
		reposition(verticesPath, stripSideVector, distance);

		setIndices(verticesPath);
	}

	private void setIndices(float[][] verticesPath) {
		int indexIndex = 0;
		int pathLength = verticesPath.length;
		int l = pathLength -1;
		for (int i = 0; i < l; i++){
			indexBuffer[indexIndex + 0] = (short) i;
			indexBuffer[indexIndex + 1] = (short)(i + 1);
			indexBuffer[indexIndex + 2] = (short)(pathLength + i);

			indexBuffer[indexIndex + 3] = (short)(i + 1);
			indexBuffer[indexIndex + 4] = (short)(pathLength + i + 1);
			indexBuffer[indexIndex + 5] = (short)(pathLength + i);
			
			indexIndex += 6;
		}
	}

	public void reposition(float[][] verticesPath, float[] stripSideVector, float distance){
		setPathVertices(verticesPath);
		setNonPathVertices(verticesPath, stripSideVector, distance);
		positionDirty = true;
	}
	
	private void setPathVertices(float[][] verticesPath) {
		int pathLength = verticesPath.length;
		for (int i = 0; i < pathLength; i++){
			vertices[i].setPositionXY(verticesPath[i][0], verticesPath[i][1]);
		}
	}

	private static float[] tmpAtoB = new float[2];
	private static float[] tmpResult = new float[2];
	private void setNonPathVertices(float[][] verticesPath, float[] stripSideVector, float distance) {
		int pathLength = verticesPath.length;

		// compute segment normals
		float[][] segmentNormals = new float[pathLength-1][];
		for (int i = 0; i < pathLength-1; i++){
			segmentNormals[i] = new float[2];
			Vector.aToB2(tmpAtoB, verticesPath[i], verticesPath[i+1]);
			Vector.normal2(segmentNormals[i], tmpAtoB);
		}
		
		// is this the right direction?
		Vector.aToB2(tmpAtoB, verticesPath[0], stripSideVector);
		if (Vector.dotProduct2(tmpAtoB, segmentNormals[0]) < 0){
			for (int i = 0; i < pathLength-1; i++) Vector.invert2(segmentNormals[i]);
		}
		
		// place first
		Vector.normalize2(segmentNormals[0]);
		Vector.scalarMultiply2(segmentNormals[0], distance);
		vertices[pathLength].setPositionXY(Vector.aPlusB2(tmpResult, verticesPath[0], segmentNormals[0]));
		
		// place vertices in between
		for (int i = 1; i < pathLength-1; i++){
			Vector.aPlusB2(tmpResult, segmentNormals[i-1], segmentNormals[i]);
			Vector.normalize2(tmpResult);
			Vector.scalarMultiply2(tmpResult, distance);
			vertices[pathLength + i].setPositionXY(Vector.aPlusB2(tmpResult, tmpResult, verticesPath[i]));
		}
		
		// place last
		Vector.normalize2(segmentNormals[pathLength-2]);
		Vector.scalarMultiply2(segmentNormals[pathLength-2], distance);
		vertices[pathLength*2 - 1].setPositionXY(
				Vector.aPlusB2(tmpResult, segmentNormals[pathLength-2], verticesPath[verticesPath.length-1]));
		
		
		// make the path fade out
		for (int i = 0; i < pathLength; i++) vertices[pathLength+i].setAlpha(0f);
	}

	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;

		boolean vboDirty = false;

		if (positionDirty){
			int offset = 0;
			for (int i = 0; i < vertexCount; i++){
				// upper left vertex
				vertices[i].writePosition(vertexBuffer, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			positionDirty = false;
			vboDirty = true;
		}

		if (alphaDirty){
			int offset = 0;
			for (int i = 0; i < vertexCount; i++){
				// upper left vertex
				vertices[i].writeAlpha(vertexBuffer, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			alphaDirty = false;
			vboDirty = true;
		}

		if (texCoordsDirty){
			int offset = 0;
			for (int i = 0; i < vertexCount; i++){
				// upper left vertex
				vertices[i].writeUvCoords(vertexBuffer, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			texCoordsDirty = false;
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
		return indexCount;
	}
	
	@Override
	public int render(RenderContext rc, FloatBuffer vb, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;

		int vbIndex = firstVertexIndex * TexturedVertex.STRIDE_FLOATS;
		
		if (positionDirty){
			int offset = vbIndex;
			for (int i = 0; i < vertexCount; i++){
				// upper left vertex
				vertices[i].writePosition(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			positionDirty = false;
		}

		if (alphaDirty){
			int offset = vbIndex;
			for (int i = 0; i < vertexCount; i++){
				// upper left vertex
				vertices[i].writeAlpha(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			alphaDirty = false;
		}

		if (texCoordsDirty){
			int offset = vbIndex;
			for (int i = 0; i < vertexCount; i++){
				// upper left vertex
				vertices[i].writeUvCoords(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			texCoordsDirty = false;
		}

		frameIndexBuffer.put(indexBuffer);
		return indexCount;
	}

	@Override
	public int getMaxVertexCount() {
		return vertexCount;
	}

	@Override
	public int getMaxIndexCount() {
		return indexCount;
	}

	public void setAlpha(int index, float nuAlpha) {
		vertices[index].setAlpha(nuAlpha);
		alphaDirty = true;
	}

	public void scaleCoords(float xScale, float yScale) {
		for (TexturedVertex vertex : vertices) {
			vertex.scaleCoords(xScale, yScale);
		}
		positionDirty = true;
	}

	public void setTexCoordsUv(RectF uvCoords) {
		// HACK FOR NOW: Postualte equal distances between vertices
		
		float uDelta = uvCoords.width() / (float)(vertices.length/2 - 1);
		float u = uvCoords.left;
		float vTop = uvCoords.top;
		float vBottom = uvCoords.bottom;
		
		int l = vertices.length/2;
		for (int i = 0; i < l; i++){
			vertices[i].setUvCoords(u, vTop);
			vertices[i+l].setUvCoords(u, vBottom);
			
			u += uDelta;
		}
		
		texCoordsDirty = true;
	}
}

