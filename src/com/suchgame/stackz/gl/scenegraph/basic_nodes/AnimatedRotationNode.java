package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;

/**
 * This is a specialization of the RotationNode that animates the rotation
 * instead of setting it directly.
 * <b>NOTE</b>: Only the rotation angle is animated, changes of the axis are
 * effective IMMEDIATELY!
 * 
 * @author Matthias Schicker
 */
public class AnimatedRotationNode extends RotationNode {
	private InterpolatedValue angleDegrees = new InterpolatedValue(AnimationType.INVERSE_SQUARED, 0);
	
	private float axisX = 1;
	private float axisY = 0;
	private float axisZ = 1;
	
	
	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		if (!angleDegrees.isDone(sgContext.frameNanoTime)){
			super.setRotation(angleDegrees.get(sgContext.frameNanoTime), axisX, axisY, axisZ);
			
			sgContext.setNotIdle();
		}

		return super.onTransform(sgContext);
	}
	
	public void setRotation(float angleDegrees, float axisX, float axisY, float axisZ) {
		this.axisX = axisX;
		this.axisY = axisY;
		this.axisZ = axisZ;

		this.setRotation(angleDegrees);
	}
	
	public void setRotationDirect(float angleDegrees, float axisX, float axisY, float axisZ) {
		setRotation(angleDegrees, axisX, axisY, axisZ);
		this.angleDegrees.shortcut();
		super.setRotation(angleDegrees, axisX, axisY, axisZ);
	}

	/**
	 * Only sets the animation target rotation without changing the rotation axis.
	 */
	public void setRotation(float nuTargetAngleDegrees) {
		float startDegrees = this.angleDegrees.getLast();
		
		float deltaDegrees = nuTargetAngleDegrees-startDegrees;
		if (deltaDegrees>180){
			angleDegrees.setDirect(startDegrees + 360);
		} else if (deltaDegrees<-180){
			angleDegrees.setDirect(startDegrees - 360);
		}
		
		angleDegrees.set(nuTargetAngleDegrees);
	}

	public void setAnimationDuration(long ns) {
		angleDegrees.setDuration(ns);
	}

	public long getAnimationDuration() {
		return angleDegrees.getDuration();
	}

	public boolean isAnimationDone(SceneGraphContext sc) {
		return angleDegrees.isDone(sc.frameNanoTime);
	}

	public void setRotationDirect(float angle) {
		angleDegrees.setDirect(angle);
	}
}
