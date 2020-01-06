package com.suchgame.stackz.gl.scenegraph.interaction;

import com.suchgame.stackz.gl.bound_meshes.BoundTexturedQuad;
import com.suchgame.stackz.gl.math.GlCube;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.scenegraph.interaction.ButtonInteractionInterpreter.IButtonElement;

/**
 * Handles interaction events and interprets them as fit for
 * a button. Visualizes interaction per alpha.
 * 
 * @author Matthias Schicker
 */
public class BoundQuadButtonInteractionInterpreter implements IButtonElement {
	private final ButtonInteractionInterpreter interpreter;
	private final IButtonElement element;
	private final BoundTexturedQuad quad;

	private float disabledAlpha = 0.6f;
	private float normalAlpha = 1f;
	private float downAlpha = 1.2f;
	private float clickAlpha = 1.6f;
	
	private boolean enabled = true;
	
	private float touchSizeFactor = 1;
	
	// ////////////////////////////////////////////////////////////////////
	// tmps, caches
	private static GlRect touchBounds = new GlRect();
	
	public BoundQuadButtonInteractionInterpreter(IButtonElement element, BoundTexturedQuad quad){
		this.element = element;
		this.quad = quad;
		interpreter = new ButtonInteractionInterpreter(this);
	}

	/**
	 * Use this to increase (or decrease) the quad that will be used to check
	 * for taps. The factor is simply multiplied to the bounds (based on it's center)
	 * of the quad. This has no visible implications
	 * @param factor	Multiplicator for the bounds of the quad. E.g. a value of 2 will result
	 * in a touch quad that is twice as wide and high as the quad, covering 4 times the area.
	 */
	public void setTouchFieldSizeFactor(float factor) {
		this.touchSizeFactor = factor;
	}

	
	public boolean onInteraction(InteractionContext ic){
		return quad.isVisible() && interpreter.onInteraction(ic);
	}
	
	@Override
	public boolean inBounds(float[] xy) {
		if (!enabled) return false;
		if (touchSizeFactor == 1) return quad.contains(xy[0], xy[1]);
		
		GlCube position = quad.getPosition();
		float centerX = position.centerX();
		float centerY = position.centerY();
		float halfWidth = position.width() * (0.5f * touchSizeFactor);
		float halfHeight = position.height() * (0.5f * touchSizeFactor);
		touchBounds.set(centerX-halfWidth, centerY+halfHeight, centerX+halfWidth, centerY-halfHeight);
		return touchBounds.contains(xy);
	}

	@Override
	public void down(InteractionContext ic) {
		quad.setAlphaDirect(downAlpha);
		quad.setAlpha(downAlpha + 0.1f);
		element.down(ic);
	}

	@Override
	public void cancel(InteractionContext ic) {
		quad.setAlpha(normalAlpha);
		element.cancel(ic);
	}

	@Override
	public void click(InteractionContext ic) {
		quad.setAlphaDirect(clickAlpha);
		element.click(ic);
		quad.setAlpha(normalAlpha);
	}

	public void enable(boolean b) {
		this.enabled = b;
		quad.setAlpha(enabled ? normalAlpha : disabledAlpha);
	}
	
	public void setNormalAlpha(float normalAlpha) {
		this.normalAlpha = normalAlpha;
	}
	
	public void setDisabledAlpha(float disabledAlpha) {
		this.disabledAlpha = disabledAlpha;
	}

	public void setAlphas(float disabled, float normal, float down, float clicked) {
		this.disabledAlpha = disabled;
		this.normalAlpha = normal;
		this.downAlpha = down;	
		this.clickAlpha = clicked;
	}
}
