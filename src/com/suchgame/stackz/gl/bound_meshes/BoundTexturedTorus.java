package com.suchgame.stackz.gl.bound_meshes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.graphics.PointF;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.primitives.TexturedVertex;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;

/**
 * Draws a (simply) textured torus or torus slice. That's a flat thing, not a donut.
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public class BoundTexturedTorus extends ABoundMesh {
	private final int segments;
	
    private TexturedVertex[] innerVertices;
    private TexturedVertex[] outerVertices;
    
    protected boolean positionDirty = true;

    protected InterpolatedValue alpha = new InterpolatedValue(AnimationType.INVERSE_SQUARED, 0, InterpolatedValue.ANIMATION_DURATION_SLOW);
	protected boolean alphaDirty = true;
    
	protected final PointF innerTexCoordsUv = new PointF();
	protected final PointF outerTexCoordsUv = new PointF();
	
	protected boolean texCoordsDirty = true;

	protected final FloatBuffer vertexBuffer;


	public BoundTexturedTorus(int segments){
		this.segments = segments;
		
		int l = segments+1;
		innerVertices = new TexturedVertex[l];
		outerVertices = new TexturedVertex[l];
		for (int i = 0; i < l; i++){
			innerVertices[i] = new TexturedVertex();
			outerVertices[i] = new TexturedVertex();
		}
		
		vertexBuffer = RenderConfig.VBO_RENDERING ? TexturedVertex.allocate((segments+1)*2) : null;
		indexBuffer = createIndexBuffer();
	}
	
	public void position(float[] center, 
			float innerRadius, float outerRadius, 
			float innerStartAngle, float innerEndAngle,
			float outerStartAngle, float outerEndAngle){
		
		positionVertices(innerVertices, center, innerRadius, innerStartAngle, innerEndAngle);
		positionVertices(outerVertices, center, outerRadius, outerStartAngle, outerEndAngle);
		
		positionDirty = true;
	}
	
    private void positionVertices(TexturedVertex[] v, float[] center, float radius, float startAngle, float endAngle) {
    	if (endAngle > startAngle){
    		float tmp = startAngle;
    		startAngle = endAngle;
    		endAngle = tmp;
    	}
    	
		float deltaAngle = (endAngle - startAngle) / segments;
		
		float angle = startAngle;
		int l = segments+1;
		for (int i = 0; i < l; i++){
			double radians = Math.toRadians(angle);
			
			float x = (float) (center[0] + Math.cos(radians) * radius);
			float y = (float) (center[1] + Math.sin(radians) * radius);
			
			v[i].setPositionXY(x, y);
			
			angle += deltaAngle;
		}
	}

	public void positionZ(float z) {
    	int l = segments+1;
    	for (int i = 0; i < l; i++){
    		innerVertices[i].setPositionZ(z);
    		outerVertices[i].setPositionZ(z);
    	}
        positionDirty = true;
    }

    public void setTexCoordsUv(PointF inner, PointF outer) {
    	innerTexCoordsUv.set(inner);
    	outerTexCoordsUv.set(outer);
    	texCoordsDirty = true;
    }
	
	@Override
	public int render(RenderContext rc, ShortBuffer frameIndexBuffer){

		// not supported yet!
		
		return 0;

	}
	
	@Override
	public int render(RenderContext rc, FloatBuffer vb, ShortBuffer frameIndexBuffer){
		if (!visible) return 0;
		
		int vbIndex = firstVertexIndex * TexturedVertex.STRIDE_FLOATS;
		
		if (positionDirty){
			int offset = vbIndex;
			int l = segments+1;
			for (int i = 0; i < l; i++){
				innerVertices[i].writePosition(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			for (int i = 0; i < l; i++){
				outerVertices[i].writePosition(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			positionDirty = false;
		}
		
		if (alphaDirty){
			float nowAlpha = alpha.get(rc.frameNanoTime);
			
			int offset = vbIndex;
			int l = segments+1;
			for (int i = 0; i < l; i++){
				innerVertices[i].setAlpha(nowAlpha);
				innerVertices[i].writeAlpha(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			for (int i = 0; i < l; i++){
				outerVertices[i].setAlpha(nowAlpha);
				outerVertices[i].writeAlpha(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			
			alphaDirty = !alpha.isDone(rc.frameNanoTime);
		}
		
		if (alpha.getLast() < 0.1f) return 0;
		
		if (texCoordsDirty){
			int offset = vbIndex;
			int l = segments+1;
			for (int i = 0; i < l; i++){
				innerVertices[i].setUvCoords(innerTexCoordsUv.x, innerTexCoordsUv.y);
				innerVertices[i].writeUvCoords(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			for (int i = 0; i < l; i++){
				outerVertices[i].setUvCoords(outerTexCoordsUv.x, outerTexCoordsUv.y);
				outerVertices[i].writeUvCoords(vb, offset);
				offset += TexturedVertex.STRIDE_FLOATS;
			}
			
			texCoordsDirty = false;
		}
		
		frameIndexBuffer.put(indexBuffer);
		return indexBuffer.length;
	}

	@Override
	public int getMaxVertexCount() {
		return (segments+1) * 2;
	}

	@Override
	public int getMaxIndexCount() {
		return indexBuffer.length;
	}
    
	public void setAlpha(float alpha){
		this.alpha.set(alpha);
		alphaDirty = true;
	}
	
	protected short[] createIndexBuffer() {
		short[] ret = new short[segments * 6];
		
		int index = 0;
		for (int i = 0; i < segments; i++){
			ret[index++] = (short) i;
			ret[index++] = (short) (i+1);
			ret[index++] = (short) (i + segments + 1);
			
			ret[index++] = (short) (i+1);
			ret[index++] = (short) (i + segments + 2);
			ret[index++] = (short) (i + segments + 1);
		}
		
		return ret;
	}

	public void clampInto(GlRect bounds) {
		int l = segments + 1;
		for (int i = 0; i < l; i++){
			bounds.clampInside(innerVertices[i].getPosition());
			bounds.clampInside(outerVertices[i].getPosition());
		}
	}

	public float getTargetAlpha() {
		return alpha.getTarget();
	}

	public float getLastAlpha() {
		return alpha.getLast();
	}
}
