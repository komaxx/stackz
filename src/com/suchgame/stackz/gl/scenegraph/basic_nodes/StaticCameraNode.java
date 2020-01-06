package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import android.opengl.Matrix;

import com.suchgame.stackz.gl.ICameraInfoProvider;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.math.MatrixUtil;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.scenegraph.interaction.Pointer;
import com.suchgame.stackz.gl.util.ObjectsStore;

/**
 * A simple unmoving camera node that focuses on the viewport in such a way
 * that 3d-coords correspond to pixels in the z0-plane. 
 * 
 * @author Matthias Schicker
 */
public class StaticCameraNode extends Node implements ICameraInfoProvider {
	private float fovAngleX = 40f;		// degrees
	
	protected float[] projectionMatrix = new float[MatrixUtil.MATRIX_SIZE];
	protected float[] viewMatrix = new float[MatrixUtil.MATRIX_SIZE];
	protected float[] invertedViewMatrix = new float[MatrixUtil.MATRIX_SIZE];
	private float[] modelMatrix = new float[MatrixUtil.MATRIX_SIZE];
	
	protected float[] eyePoint = new float[]{ 0f, 0f, 1234f };
	protected float[] lookingDirection = new float[]{ 0f, 0f, -1f };
	protected float[] upVector = new float[]{ 0f, 1f, 0f };
	
	protected boolean pvMatrixDirty = true;

	/**
	 * Caches the current viewProjection matrix
	 */
	protected float[] tmpPvMatrix = new float[MatrixUtil.MATRIX_SIZE];
	
	protected float[] tmpInteractionCoordsNow;
	protected float[] tmpInteractionCoordsLast;
	protected float[] downTiltXY = new float[2];
	
	// ///////////////////////////////////////
	// tmps, caches
	protected float[] tmpTargetPoint = new float[3];
	
	protected float surfaceWidth = 0;
	protected float surfaceHeight = 0;
	protected float surfaceRatio = 0;

	
	public StaticCameraNode(){
		Matrix.setIdentityM(modelMatrix, 0);
		// just start with something that makes almost sense, will be corrected with the first frame.
		Matrix.orthoM(projectionMatrix, 0, -1, 1, -1, 1, 1, 50);
		recomputeViewMatrix();
		transforms = true;
		handlesInteraction = true;
	}

	protected void recomputeViewMatrix() {
		tmpTargetPoint = Vector.aPlusB3(tmpTargetPoint, eyePoint, lookingDirection);
		Matrix.setLookAtM(viewMatrix, 0, 
				eyePoint[0], eyePoint[1], eyePoint[2], 
				tmpTargetPoint[0], tmpTargetPoint[1], tmpTargetPoint[2], 
				upVector[0], upVector[1], upVector[2]);
		
//		Matrix.translateM(viewMatrix, 0, -surfaceWidth/2, +surfaceHeight/2, 0);
		
		Matrix.invertM(invertedViewMatrix, 0, viewMatrix, 0);
	}

	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		sgContext.setCameraInfoProvider(this);
		
		if (pvMatrixDirty) recomputePvMatrix();
		
		float[] sgEyePoint = sgContext.eyePointStack.push();
		Vector.set3(sgEyePoint, eyePoint[0], eyePoint[1], eyePoint[2]);
		sgEyePoint[3] = 1;
		
		float[] stackViewMatrix = sgContext.projectionViewMatrixStack.push();
		System.arraycopy(tmpPvMatrix, 0, stackViewMatrix, 0, MatrixUtil.MATRIX_SIZE);
		
		sgContext.setMvpMatrixDirty();
		
		return true;
	}
	
	protected void recomputePvMatrix() {
		recomputeViewMatrix();
		Matrix.multiplyMM(tmpPvMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
		pvMatrixDirty = false;
	}

	@Override
	public void onUnTransform(SceneGraphContext sgContext) {
		sgContext.eyePointStack.pop();
		sgContext.projectionViewMatrixStack.pop();
		sgContext.setMvpMatrixDirty();
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		pvMatrixDirty = true;
	}
	
	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		surfaceWidth = renderContext.surfaceWidth;
		surfaceHeight = renderContext.surfaceHeight;
        surfaceRatio = surfaceWidth / surfaceHeight;

        setFovAngle(fovAngleX);
	}
	
	/**
	 * Adjust the horizontal FOV of the camera. The focus plane will not be changed, it's still
	 * the z=0 plane, so the camera distance will change! </br>
	 * Think <i>Hitchcock effect ;)</i> </br>
	 * <b>NOTE:</b> GL thread only.
	 * @param nuFovAngle	the new angle in degrees
	 */
	public void setFovAngle(float nuFovAngle){
		fovAngleX = nuFovAngle;

		if (surfaceWidth == 0) return;		// would fuck up the frustum!
		
        float cameraDistance = (float) ((surfaceWidth/2f) / Math.tan(Math.toRadians(fovAngleX)));
        eyePoint[2] = cameraDistance;

        float NEAR_PLANE_DISTANCE_FACTOR = 0.2f;
        float FAR_PLANE_DISTANCE_FACTOR = 10f;
        
        float nearPlaneDistance = cameraDistance * NEAR_PLANE_DISTANCE_FACTOR;
        float farPlaneDistance = cameraDistance * FAR_PLANE_DISTANCE_FACTOR;
        
       	Matrix.frustumM(projectionMatrix, 0, 
       			- surfaceWidth/2 * NEAR_PLANE_DISTANCE_FACTOR, surfaceWidth/2 * NEAR_PLANE_DISTANCE_FACTOR, 
       			- surfaceHeight/2 * NEAR_PLANE_DISTANCE_FACTOR, surfaceHeight/2 * NEAR_PLANE_DISTANCE_FACTOR,
        			nearPlaneDistance, farPlaneDistance);
        pvMatrixDirty = true;
	}

	@Override
	public void onTransformInteraction(InteractionContext ic) {
		if (pvMatrixDirty) recomputePvMatrix();

		float[] tmpVector = ObjectsStore.getVector();
		
		for (int i = 0; i < InteractionContext.MAX_POINTER_COUNT; i++){
			Pointer pointer = ic.getPointers()[i];
			if (!pointer.isActive()) continue;
			float[] rayPoint = pointer.pushRayPoint();
			rayPoint[0] -= surfaceWidth/2;
			rayPoint[1] = -rayPoint[1] + surfaceHeight/2;
			rayPoint[2] = -eyePoint[2];
		
			Vector.set4(tmpVector, rayPoint);
			Matrix.multiplyMV(rayPoint, 0, invertedViewMatrix, 0, tmpVector, 0);
		}
		
		ObjectsStore.recycleVector(tmpVector);
	}
	
	@Override
	public void onUnTransformInteraction(InteractionContext ic) {
		for (int i = 0; i < InteractionContext.MAX_POINTER_COUNT; i++){
			Pointer pointer = ic.getPointers()[i];
			if (pointer.isActive()) pointer.popRayPoint();
		}
	}
	
	@Override
	public void pixel2ZeroPlaneCoord(float[] result, float[] pixelXY) {
		result[0] = pixelXY[0];
		result[1] = - (pixelXY[1] - eyePoint[1]);
	}
}