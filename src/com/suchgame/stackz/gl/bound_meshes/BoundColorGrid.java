package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.primitives.TexturedVertex;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;

public class BoundColorGrid extends ABoundMesh {
	private final int vCols;
	private final int vRows;
	private final int verticesCount;
	
	private GlRect position = new GlRect();
	private float z = 0;
	private boolean positionDirty = true;

	private final Bitmap colorBitmap;
	private boolean colorDirty = true;
	
	private FloatBuffer vertexBuffer;
	
	// tiles: (r-1)*(c-1)
	// triangles: 2*tiles = 2* (r-1)*(c-1)
	// indices : 3*triangles = 6 * (r-1)*(c-1)

	// /////////////////////////////////////////////
	// caches, short use stuff
	private final float[] tmpX;
	private final float[] tmpY;
	
	public BoundColorGrid(int vCols, int vRows){
		this.vCols = vCols;
		this.vRows = vRows;
		this.verticesCount = vCols * vRows;
		
		tmpX = new float[vCols];
		tmpY = new float[vRows];
		colorBitmap = Bitmap.createBitmap(vCols, vRows, Config.RGB_565);
		
		vertexBuffer = Vertex.allocateColorVertices(vRows * vCols);
		
		createIndices();
	}
	
	private void createIndices() {
		indexBuffer = new short[6 * (vRows-1) * (vCols-1)];
		setIndices();
	}
	
	public static final int getIndexCount(int vColumns, int vRows){
		return 6 * (vRows-1) * (vColumns-1);
	}
	
	private void setIndices() {
		int index = 0;
		for (short r = 0; r < (vRows-1); r++){
			for (short c = 0; c < (vCols-1); c++){
				// first triangle
				indexBuffer[index++] = (short)( (r*vCols) + c);
				indexBuffer[index++] = (short)( ((r+1)*vCols) + c);
				indexBuffer[index++] = (short)( (r*vCols) + c + 1);
				// second triangle
				indexBuffer[index++] = (short)( (r*vCols) + c + 1);
				indexBuffer[index++] = (short)( ((r+1)*vCols) + c);
				indexBuffer[index++] = (short)( ((r+1)*vCols) + c + 1);
			}
		}
	}
	
	public short[] getIndices() {
		return indexBuffer;
	}

	public int getVboVertexIndex(){
		return firstVertexIndex;
	}
	
	public void positionXY(float left, float top, float right, float bottom) {
		position.set(left, top, right, bottom);
		positionDirty = true;
	}
	
	/**
	 * Colors will be taken from the Drawable. <br/>
	 * NOTE: Setting colors on a grid may be expansive. Therefore you need to call
	 * <code>setColorDirty</code> explicitly yourself.<br/>
	 * WARNING: Will change the drawable's bounds.
	 */
	public void setColors(Drawable d){
		d.setBounds(0, 0, vCols, vRows);
		Canvas c = new Canvas(colorBitmap);
		d.draw(c);
	}
	
	public void setColor(int color) {
		colorBitmap.eraseColor(color);
	}
	
	public void setColors(Bitmap bmp){
		try {
			Canvas c = new Canvas(colorBitmap);
			c.drawBitmap(bmp, null, new Rect(0,0,colorBitmap.getWidth(),colorBitmap.getHeight()), null);
		} catch (Exception e){
			KoLog.w(this, "Exception in setColors: " + e.getMessage());
		}
	}
	
	public void setColorDirty(){
		colorDirty = true;
	}
	

	@Override
	public int render(RenderContext rc, FloatBuffer vertexBuffer, ShortBuffer frameIndexBuffer) {
		if (!visible) return 0;
		
		int vbIndex = firstVertexIndex * TexturedVertex.STRIDE_FLOATS;
		
		if (positionDirty){
			applyPosition(vertexBuffer, vbIndex);
			positionDirty = false;
		}
		
		if (colorDirty){
			applyColor(vertexBuffer, vbIndex);
			colorDirty = false;
		}
		
		frameIndexBuffer.put(indexBuffer);
		return indexBuffer.length;
	}
	
	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;
		
		boolean vboDirty = false;
		
		if (positionDirty){
			applyPosition(vertexBuffer, 0);
			positionDirty = false;
			vboDirty = true;
		}
		
		if (colorDirty){
			applyColor(vertexBuffer, 0);
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
		return indexBuffer.length;
	}

	private void applyColor(FloatBuffer vertexBuffer, int offset) {
		offset += Vertex.COLOR_VERTEX_DATA_COLOR_OFFSET;
		float[] tmpColor = new float[]{ 1,1,1,1};
		for (int y = 0; y < vRows; y++){
			for (int x = 0; x < vCols; x++){
				int pixelColor = colorBitmap.getPixel(x, y);
				RenderUtil.color2floatsRGB(tmpColor, pixelColor);
				
				vertexBuffer.position(offset);
				vertexBuffer.put(tmpColor);
				offset += Vertex.COLOR_VERTEX_DATA_STRIDE_FLOATS;
			}
		}
	}

	private void applyPosition(FloatBuffer vertexBuffer, int offset) {
		// precompute x-Values
		tmpX[0] = position.left;
		int tiles = vCols-1;
		float delta = position.width() / (float)tiles;
		
		float now = position.left;
		for(int i = 1; i < tiles; i++){
			now += delta;
			tmpX[i] = now;
		}
		tmpX[tiles] = position.right;
		
		// precompute yValues
		now = tmpY[0] = position.top;
		tiles = vRows-1;
		delta = position.height() / (float)tiles;
		
		for(int i = 1; i < tiles; i++){
			now -= delta;
			tmpY[i] = now;
		}
		tmpY[tiles] = position.bottom;

		// and assign to the vertexBuffer
		for (int y = 0; y < vRows; y++){
			for (int x = 0; x < vCols; x++){
				vertexBuffer.position(offset);
				vertexBuffer.put(tmpX[x]);
				vertexBuffer.put(tmpY[y]);
				vertexBuffer.put(z);
				offset += Vertex.COLOR_VERTEX_DATA_STRIDE_FLOATS;
			}
		}
	}

	public boolean contains(float x, float y) {
		return position.contains(x, y);
	}

	@Override
	public int getMaxVertexCount() {
		return verticesCount;
	}

	@Override
	public int getMaxIndexCount() {
		return indexBuffer.length;
	}

	public void positionZ(float zPos) {
		z = zPos;
		positionDirty = true;
	}
}
