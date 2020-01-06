package com.suchgame.stackz.ui.menu;

import com.suchgame.stackz.gl.bound_meshes.AnimatedBoundTexturedQuad;
import com.suchgame.stackz.gl.scenegraph.interaction.ButtonInteractionInterpreter;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;

public class MenuButtonInterpreter 
		extends ButtonInteractionInterpreter 
		implements ButtonInteractionInterpreter.IButtonElement {
	
	private IMenuButtonClickListener listener;
	private AnimatedBoundTexturedQuad quad;

	public MenuButtonInterpreter(AnimatedBoundTexturedQuad quad, IMenuButtonClickListener listener) {
		super();
		element = this;

		this.quad = quad;
		this.listener = listener;
	}

	@Override
	public boolean inBounds(float[] xy) {
		return quad.isVisible() && quad.getTargetPosition().containsXY(xy[0], xy[1]);
	}

	@Override
	public void down(InteractionContext ic) {
		quad.setAlpha(1.3f);
		quad.positionZ(-quad.getPosition().height()/12f, quad.getPosition().height()/12f);
		quad.positionZ(quad.getPosition().height()/4f, -quad.getPosition().height()/4f);
	}

	@Override
	public void cancel(InteractionContext ic) {
		quad.setAlpha(1f);
		quad.positionZ(0, 0);
	}

	@Override
	public void click(InteractionContext ic) {
		quad.setAlphaDirect(2f);
		quad.setAlpha(1f);
		quad.positionDirectZ(quad.getPosition().height()/5f, -quad.getPosition().height()/5f);
		quad.positionZ(0, 0);

		listener.clicked(ic);
	}

	public static interface IMenuButtonClickListener {
		void clicked(InteractionContext ic);
	}
}