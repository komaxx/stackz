package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.MatrixUtil;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.primitives.ColoredVertex;
import com.suchgame.stackz.gl.primitives.TexturedQuad;
import com.suchgame.stackz.gl.util.ObjectsStore;

public class BoundTransformableColoredQuad extends BoundColorQuad {
	protected float[] transformation = MatrixUtil.buildMatrix();
	protected boolean transformationDirty = false;
	
	/**
	 * Sets the transformation for this quad. The matrix will be copied,
	 * so changes to the parameter matrix after this is called will have
	 * no effect.
	 */
	public void setTransformation(float[] nuTransformation){
		MatrixUtil.setMatrix(transformation, nuTransformation);
		transformationDirty = true;
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
		
		if (positionDirty || transformationDirty){
			float[] tmpVectorUl = ObjectsStore.getVector();
			float[] tmpVectorLl = ObjectsStore.getVector();
			float[] tmpVectorLr = ObjectsStore.getVector();
			float[] tmpVectorUr = ObjectsStore.getVector();
			float[] tmpInput = ObjectsStore.getVector();
			
			// direct
			Matrix.multiplyMV(tmpVectorUl, 0, transformation, 0, position.ulf, 0);
			Matrix.multiplyMV(tmpVectorLr, 0, transformation, 0, position.lrb, 0);
			// derived
			Vector.set4(tmpInput, position.ulf[0], position.lrb[1], position.ulf[2], 1);
			Matrix.multiplyMV(tmpVectorLl, 0, transformation, 0, tmpInput, 0);
			Vector.set4(tmpInput, position.lrb[0], position.ulf[1], position.lrb[2], 1);
			Matrix.multiplyMV(tmpVectorUr, 0, transformation, 0, tmpInput, 0);
			
			TexturedQuad.position(vertexBuffer, 0, tmpVectorUl, tmpVectorLl, tmpVectorLr, tmpVectorUr);
			
			ObjectsStore.recycleVector(tmpVectorUl);
			ObjectsStore.recycleVector(tmpVectorLl);
			ObjectsStore.recycleVector(tmpVectorLr);
			ObjectsStore.recycleVector(tmpVectorUr);
			ObjectsStore.recycleVector(tmpInput);
			
			positionDirty = false;
			transformationDirty = false;
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
		
		if (positionDirty || transformationDirty){
			float[] tmpVectorUl = ObjectsStore.getVector();
			float[] tmpVectorLl = ObjectsStore.getVector();
			float[] tmpVectorLr = ObjectsStore.getVector();
			float[] tmpVectorUr = ObjectsStore.getVector();
			float[] tmpInput = ObjectsStore.getVector();
			
			// direct
			Matrix.multiplyMV(tmpVectorUl, 0, transformation, 0, position.ulf, 0);
			Matrix.multiplyMV(tmpVectorLr, 0, transformation, 0, position.lrb, 0);
			// derived
			Vector.set4(tmpInput, position.ulf[0], position.lrb[1], position.ulf[2], 1);
			Matrix.multiplyMV(tmpVectorLl, 0, transformation, 0, tmpInput, 0);
			Vector.set4(tmpInput, position.lrb[0], position.ulf[1], position.lrb[2], 1);
			Matrix.multiplyMV(tmpVectorUr, 0, transformation, 0, tmpInput, 0);
			
			TexturedQuad.position(vb, vbIndex, tmpVectorUl, tmpVectorLl, tmpVectorLr, tmpVectorUr);
			
			ObjectsStore.recycleVector(tmpVectorUl);
			ObjectsStore.recycleVector(tmpVectorLl);
			ObjectsStore.recycleVector(tmpVectorLr);
			ObjectsStore.recycleVector(tmpVectorUr);
			ObjectsStore.recycleVector(tmpInput);
			
			positionDirty = false;
			transformationDirty = false;
		}
		
		frameIndexBuffer.put(indexBuffer);
		return TexturedQuad.INDICES_COUNT;
	}
}
