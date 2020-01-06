package com.suchgame.stackz.gl.bound_meshes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.opengl.GLES20;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.util.RenderUtil;

public class Vbo {
	private static final int DEFAULT_BYTES_PER_VERTEX = Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES;
	private static final int DEFAULT_VERTICES_SIZE = 16;
		
	private int handle = - 1;
	private int bytesPerVertex = DEFAULT_BYTES_PER_VERTEX;
	private int vertexCount = DEFAULT_VERTICES_SIZE;
	
	private int firstFreeIndex = 0;
	private ArrayList<int[]> vertexHoles = new ArrayList<int[]>();	// format: [0]:index, [1]:length


	public Vbo(int vertexCount, int bytesPerVertex){
		this.bytesPerVertex = bytesPerVertex;
		this.vertexCount = vertexCount;
	}
	
	/**
	 * Builds a Vbo with the default size vertex type (16 simple textured vertices)
	 * NOTE: Before using the vbo, you need to <code>create</code> it!
	 */
	public Vbo(){ }
	
	
	public int getHandle() {
		return handle;
	}
	
	private static int[] tmpBufferHandles = new int[1];
	
	/**
	 * MUST be called before using the VBO!
	 */
	public void create(){
		if (RenderConfig.VBO_RENDERING){
			GLES20.glGenBuffers(1, tmpBufferHandles, 0);
			handle = tmpBufferHandles[0];
			
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 
					vertexCount*bytesPerVertex, null, GLES20.GL_STATIC_DRAW);
			if (RenderConfig.GL_DEBUG) RenderUtil.checkGlError("building VBO");
			
			// unbind VBOs - otherwise, non-vbo-stuff will not be drawn
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
	}

	/**
	 * Creates a user-space buffer with the same size as the VBO. Useful when
	 * VBO painting is not available.
	 */
	public FloatBuffer buildVertexBuffer() {
		return ByteBuffer.allocateDirect(
				vertexCount * bytesPerVertex
                ).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}
	
	/**
	 * Locates sufficient space for the mesh and sets up the mesh accordingly.
	 * @return	true when enough space was available, false otherwise.
	 */
	public boolean bind(IBoundMesh boundMesh) {
		int necessarySpace = boundMesh.getMaxVertexCount();
		if (firstFreeIndex + necessarySpace <= vertexCount){
			boundMesh.setFirstIndex(firstFreeIndex);
			firstFreeIndex += necessarySpace;
			return true;
		}
		
		for (int[] hole : vertexHoles){
			if (hole[1] >= necessarySpace){
				boundMesh.setFirstIndex(hole[0]);
				
				if (necessarySpace == hole[1]){
					vertexHoles.remove(hole);
				} else {
					hole[0] += necessarySpace;
					hole[1] -= necessarySpace;
				}
				
				return true;
			}
		}
		
		return false;
	}

	public void unbind(IBoundMesh mesh) {
		int firstIndex = mesh.getFirstIndex();
		int size = mesh.getMaxVertexCount();
		
		if (firstIndex + size >= firstFreeIndex){
			firstFreeIndex = firstIndex;
		} else {
			vertexHoles.add(new int[]{firstIndex, size});
		}
	}
	
	public int getBytesPerVertex() {
		return bytesPerVertex;
	}
	
	public void setBytesPerVertex(int bytesPerVertex) {
		this.bytesPerVertex = bytesPerVertex;
	}
	
	public void setVertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}
}
