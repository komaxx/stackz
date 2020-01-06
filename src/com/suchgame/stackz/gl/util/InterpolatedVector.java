package com.suchgame.stackz.gl.util;

import com.suchgame.stackz.gl.util.InterpolatedValue.AnimationType;

/**
 * Simply binds three interpolated values 
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public final class InterpolatedVector {
	private InterpolatedValue[] v;
	
	public InterpolatedVector(){
		v = new InterpolatedValue[3];
		for (int i = 0; i < 3; i++){
			v[i] = new InterpolatedValue(AnimationType.INVERSE_SQUARED, 0, InterpolatedValue.ANIMATION_DURATION_NORMAL);
		}
	}
	
	public float[] getLast(float[] ret) {
		for (int i = 0; i < 3; i++) ret[i] = v[i].getLast();
		return ret;
	}
	
	public void setAnimationDuration(long nuDuration){
		for (int i = 0; i < 3; i++) v[i].setDuration(nuDuration);
	}
	
	public float[] get(float[] ret, long time) {
		for (int i = 0; i < 3; i++) ret[i] = v[i].get(time);
		return ret;
	}
		
	public void set(float[] nuTargets){
		for (int i = 0; i < 3; i++) v[i].set(nuTargets[i]);
	}
	
	public void set(float x, float y, float z) {
		v[0].set(x);
		v[1].set(y);
		v[2].set(z);
	}

	public void setDirect(float[] nuTargets){
		for (int i = 0; i < 3; i++) v[i].setDirect(nuTargets[i]);
	}
	
	public boolean isDone(long time){
		return v[0].isDone(time);
	}
}
