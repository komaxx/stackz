package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.util.InterpolatedValue;
import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;

/**
 * Extension of the translation node that makes translations animated instead
 * of executing them directly.
 * 
 * @author Matthias Schicker
 */
public class AnimatedTranslationNode extends TranslationNode {
	private InterpolatedValue xTranslation = new InterpolatedValue(AnimationType.INVERSE_SQUARED, 0);
	private InterpolatedValue yTranslation = new InterpolatedValue(AnimationType.INVERSE_SQUARED, 0);
	private InterpolatedValue zTranslation = new InterpolatedValue(AnimationType.INVERSE_SQUARED, 0);
	
	private boolean forcedUpdate = false; 
	
	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		if (forcedUpdate 
				|| !xTranslation.isDone(sgContext.frameNanoTime) 
				|| !yTranslation.isDone(sgContext.frameNanoTime)
				|| !zTranslation.isDone(sgContext.frameNanoTime)){
			super.setTranslation(
					xTranslation.get(sgContext.frameNanoTime), 
					yTranslation.get(sgContext.frameNanoTime), 
					zTranslation.get(sgContext.frameNanoTime));
			
			sgContext.setNotIdle();
			forcedUpdate = false;
		}
		
		super.onTransform(sgContext);
		
		return true;
	}
	
	public void setZ(float targetZ) {
		zTranslation.set(targetZ);
	}
	
	public void setX(float targetX) {
		xTranslation.set(targetX);
	}
	
	public void setY(float targetY) {
		yTranslation.set(targetY);
	}
	
	public void setXDirect(float nuX) {
		xTranslation.setDirect(nuX);
		forcedUpdate = true;
	}
	
	public void setYDirect(float nuY) {
		yTranslation.setDirect(nuY);
		forcedUpdate = true;
	}
	
	public void setTranslation(float x, float y, float z) {
		xTranslation.set(x);
		yTranslation.set(y);
		zTranslation.set(z);
	}
	
	public void setTranslationDirect(float x, float y, float z) {
		xTranslation.setDirect(x);
		yTranslation.setDirect(y);
		zTranslation.setDirect(z);
		
		forcedUpdate = true;
	}

	public long getAnimationDuration() {
		return xTranslation.getDuration();
	}

	public float getTargetY() {
		return yTranslation.getTarget();
	}

	public float getCurrentY(SceneGraphContext sgc) {
		return yTranslation.get(sgc.frameNanoTime);
	}
	
	public float getCurrentX(SceneGraphContext sgc) {
		return xTranslation.get(sgc.frameNanoTime);
	}
	public void setAnimationDuration(long nuDuration) {
		xTranslation.setDuration(nuDuration);
		yTranslation.setDuration(nuDuration);
		zTranslation.setDuration(nuDuration);
	}
}
