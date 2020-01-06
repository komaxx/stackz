package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import android.opengl.Matrix;

import com.suchgame.stackz.gl.ICameraInfoProvider;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.math.MatrixUtil;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.OverlayTapCatchNode.IOverlayTapCatchListener;
import com.suchgame.stackz.gl.scenegraph.interaction.FlingScrollInteractionInterpreter;
import com.suchgame.stackz.gl.scenegraph.interaction.FlingScrollInteractionInterpreter.IFlingListener;
import com.suchgame.stackz.gl.scenegraph.interaction.FlingScrollInteractionInterpreter.IFlingable;
import com.suchgame.stackz.gl.scenegraph.interaction.FlingScrollInteractionInterpreter.IScrollListener;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.scenegraph.interaction.Pointer;
import com.suchgame.stackz.gl.util.ObjectsStore;

/**
 * This is a special simplified CameraNode that is suitable for list
 * views. It aligns itself in such a way that in the z0 one pixel
 * corresponds to exactly one virtual unit.
 * 
 * @author Matthias Schicker
 */
public class ListCameraNode extends Node implements ICameraInfoProvider, IFlingable, IOverlayTapCatchListener, IFlingListener {
	/**
	 * If the per-frame scroll delta is bigger than this value (in world coords), the
	 * system is to be considered "not idle", i.e. idle jobs will be postponed.
	 */
	private static final float MIN_IDLE_DELTA = 0;

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
	 *  Whenever the perspective of the camera changes, this is increased. This way, nodes further
	 *  down in the graph know that they may recompute stuff based on the new perspective.
	 */
	protected int worldChangeId = 0; 

	protected OverlayTapCatchNode flingTapCatcher;
	protected FlingScrollInteractionInterpreter interpreter;

	private float scrollTopLimit = 0;
	private float scrollBottomLimit = Integer.MIN_VALUE;
	
	/**
	 * Caches the current viewProjection matrix
	 */
	protected float[] tmpPvMatrix = new float[MatrixUtil.MATRIX_SIZE];
	
	// ///////////////////////////////////////
	// tmps, caches
	protected float[] tmpTargetPoint = new float[3];
	
	protected float surfaceWidth = 0;
	protected float surfaceHeight = 0;
	protected float surfaceRatio = 0;

	
	public ListCameraNode(){
		Matrix.setIdentityM(modelMatrix, 0);
		// just start with something that makes almost sense, will be corrected with the first frame.
		Matrix.orthoM(projectionMatrix, 0, -1, 1, -1, 1, 1, 50);
		recomputeViewMatrix();
		
		transforms = true;
		handlesInteraction = true;
		
		zLevel = 2000;
		
		interpreter = new FlingScrollInteractionInterpreter(this);
		interpreter.setConsumeTouchEvent(false);
		interpreter.setFlingListener(this);
	}
	
	public void setScrollLimits(float topLimit, float bottomLimit){
		scrollTopLimit = topLimit;
		scrollBottomLimit = bottomLimit;
		sanitizeScrollLimits();
		
		interpreter.setScrollLimitMax(bottomLimit);
		interpreter.setScrollLimitMin(topLimit);
	}

	/**
	 * Ensures that no erratic behavior appears due to too short content
	 * or an erroneous "bottom > top".
	 */
	private void sanitizeScrollLimits() {
		if (scrollTopLimit - scrollBottomLimit < surfaceHeight){
			scrollBottomLimit = scrollTopLimit - surfaceHeight;
		}
	}

	protected void recomputeViewMatrix() {
		tmpTargetPoint = Vector.aPlusB3(tmpTargetPoint, eyePoint, lookingDirection);
		Matrix.setLookAtM(viewMatrix, 0, 
				eyePoint[0], eyePoint[1], eyePoint[2], 
				tmpTargetPoint[0], tmpTargetPoint[1], tmpTargetPoint[2], 
				upVector[0], upVector[1], upVector[2]);
		
		Matrix.translateM(viewMatrix, 0, -surfaceWidth/2, +surfaceHeight/2, 0);
		
		Matrix.invertM(invertedViewMatrix, 0, viewMatrix, 0);
	}

	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		sgContext.setCameraInfoProvider(this);
		
		if (interpreter.proceed(sgContext)){
			float nuOffset = interpreter.getCurrentScrollOffset();
			nuOffset = (int)nuOffset;
			
			float delta = Math.abs(eyePoint[1] - nuOffset);
			eyePoint[1] = nuOffset;
			
			if (delta > MIN_IDLE_DELTA){
				sgContext.setNotIdle();
			}
			
			pvMatrixDirty = true;
		}
		
		if (pvMatrixDirty){
			recomputePvMatrix();
			worldChangeId++;
		}
		
		sgContext.worldIdStack.push().add(worldChangeId);
		
		float[] sgEyePoint = sgContext.eyePointStack.push();
		Vector.set3(sgEyePoint, eyePoint[0]+surfaceWidth/2, eyePoint[1]-surfaceHeight/2, eyePoint[2]);
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
		sgContext.worldIdStack.pop();
		sgContext.projectionViewMatrixStack.pop();
		sgContext.setMvpMatrixDirty();
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		pvMatrixDirty = true;
		
		if (flingTapCatcher == null){
			flingTapCatcher = new OverlayTapCatchNode();
			flingTapCatcher.interactive = false;
			flingTapCatcher.setListener(this);
			renderContext.sceneGraph.addNode(this, flingTapCatcher);
		}
	}
	
	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		surfaceWidth = renderContext.surfaceWidth;
		surfaceHeight = renderContext.surfaceHeight;
        surfaceRatio = surfaceWidth / surfaceHeight;

        setFovAngle(fovAngleX);
        sanitizeScrollLimits();
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

        float NEAR_PLANE_DISTANCE_FACTOR = 0.5f;
        float FAR_PLANE_DISTANCE_FACTOR = 1.5f;
        
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
	public boolean onInteraction(InteractionContext interactionContext) {
		return interpreter.onInteraction(interactionContext);
	}
	
	@Override
	public void handleFlingingStarted() {
		this.flingTapCatcher.interactive = true;
	}
	
	@Override
	public void handleFlingingEnded() {
		if (!this.flingTapCatcher.isTouched()){
			// the fling animation just ran out == was not user-interrupted
			this.flingTapCatcher.interactive = false;
		}
	}
	
	@Override
	public void downOnOverlay(SceneGraphContext sc) {
		this.interpreter.jumpTo(sc, this.interpreter.getCurrentScrollOffset());
	}
	
	@Override
	public void upOnOverlay(SceneGraphContext sc) {
		this.flingTapCatcher.interactive = false;
	}
	
	@Override
	public void pixel2ZeroPlaneCoord(float[] result, float[] pixelXY) {
		result[0] = pixelXY[0];
		result[1] = - (pixelXY[1] - eyePoint[1]);
	}
	
	public void centerOnY(SceneGraphContext sc, float y) {
		interpreter.centerOn(sc, y);
	}

	@Override
	public boolean inBounds(float[] p) {
		return true;
	}

	public void setScrollListener(IScrollListener scrollListener) {
		interpreter.setScrollListener(scrollListener);
	}
}
