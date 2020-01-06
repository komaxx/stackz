package com.suchgame.stackz.game;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.RotationNode;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedVector;
import com.suchgame.stackz.gl.util.RenderUtil;

public class AcceleRotateNode extends RotationNode implements SensorEventListener {
	/**
	 * If this time or more is between two sensor, updates the view will be centered
	 * on the current orientation immediately.
	 */
	private static final long RESET_TIME_MS = 5000;

	/**
	 * Between two updates of the interpolated base value, this amount of millis has to
	 * pass. This is to give the interpolated values time to actually react on new input. 
	 */
	private static final long BASE_UPDATE_INTERVALS = 250;

	/**
	 * Must be in ]0;1]. The higher the value, the quicker and noisier the adaptation
	 */
	private static final float ADAPTATION_SMOOTHING_FACTOR = 0.08f;
	
	private static final float DEGREE_FACTOR_X = -0.25f;
	private static final float DEGREE_FACTOR_Y = 0.4f;
	
	private static final float MAX_DEGREES_X = 15;
	private static final float MAX_DEGREES_Y = 10;

	
	private Context context;
	
	private InterpolatedVector baseOrientation = new InterpolatedVector();
	
	private float[] sensorOrientation = new float[3];
	private float[] nowOrientation = new float[3];
	
	private boolean sensorListenerRegistered = false;
	
	// ////////////////////////////////////
	// tmps, caches
	private float[] tmpBaseSnapShot = new float[3];
	
	private float[] tmpRotationMatrix = new float[16];
	private float[] tmpInclinationMatrix = new float[16];
	private float[] dummyGeomagnetic = new float[] { 1, 0, 0 };

	private float[] tmpDelta = new float[3];
	
	private long lastBaseTime = 0;
	

	
	public AcceleRotateNode(Context c) {
		this.context = c;
		
		draws = false;
		transforms = true;
		handlesInteraction = false;
		
		baseOrientation.setAnimationDuration(5*InterpolatedValue.ANIMATION_DURATION_SLOW);
	}
	
	@Override
	public void onAttached() {
		sceneGraph.postToUiThread(new Runnable(){
			@Override
			public void run() {
				SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
				Sensor accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				if (accSensor != null){
					sm.registerListener(AcceleRotateNode.this, accSensor, SensorManager.SENSOR_DELAY_GAME);
					sensorListenerRegistered = true;
				}
			}
		});
		super.onAttached();
	}
	
	@Override
	public void onDetach() {
		sceneGraph.postToUiThread(new Runnable(){
			@Override
			public void run() {
				if (!sensorListenerRegistered) return;
				SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
				sm.unregisterListener(AcceleRotateNode.this);
				sensorListenerRegistered = false;
				
			}
		});
		super.onDetach();
	}
	
	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		// animate towards last sensor value
		Vector.aMinusB3(tmpDelta, sensorOrientation, nowOrientation);
		Vector.scalarMultiply3(tmpDelta, ADAPTATION_SMOOTHING_FACTOR);
		Vector.addBtoA3(nowOrientation, tmpDelta);

		// compute current rotation
		this.baseOrientation.get(tmpBaseSnapShot, sgContext.frameNanoTime);
		Vector.aMinusB3(tmpDelta, tmpBaseSnapShot, nowOrientation);
		
//		for landscape mode
//		this.setRotation(
//				RenderUtil.clamp(DEGREE_FACTOR_X * (float) Math.toDegrees(tmpDelta[2]), -MAX_DEGREES_X, MAX_DEGREES_X),
//				RenderUtil.clamp(DEGREE_FACTOR_Y * (float) Math.toDegrees(tmpDelta[0]), -MAX_DEGREES_Y, MAX_DEGREES_Y),
//				0);
		
		
		
		this.setRotation(
				RenderUtil.clamp(DEGREE_FACTOR_X * (float) Math.toDegrees(tmpDelta[1]), -MAX_DEGREES_X, MAX_DEGREES_X),
				RenderUtil.clamp(DEGREE_FACTOR_Y * (float) Math.toDegrees(tmpDelta[0]), -MAX_DEGREES_Y, MAX_DEGREES_Y),
				0);
		
		
		
		return super.onTransform(sgContext);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// don't care for now
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		SensorManager.getRotationMatrix(
				tmpRotationMatrix, tmpInclinationMatrix, 
				event.values, dummyGeomagnetic);
		SensorManager.getOrientation(tmpRotationMatrix, sensorOrientation);
		
		
		long nowMillis = System.currentTimeMillis();
		if (nowMillis - lastBaseTime > RESET_TIME_MS){
			this.baseOrientation.setDirect(sensorOrientation);
			Vector.set3(nowOrientation, sensorOrientation);
			lastBaseTime = nowMillis; 
		} else if (nowMillis - lastBaseTime > BASE_UPDATE_INTERVALS){
			this.baseOrientation.set(sensorOrientation);
			lastBaseTime = nowMillis;
		}
	}
}
