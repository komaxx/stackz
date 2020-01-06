package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.ButtonInteractionInterpreter;
import com.suchgame.stackz.gl.scenegraph.interaction.ButtonInteractionInterpreter.IButtonElement;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;


public class OverlayTapCatchNode extends Node implements IButtonElement {
	private IOverlayTapCatchListener listener;
	
	private ButtonInteractionInterpreter interpreter = new ButtonInteractionInterpreter(this);
	private boolean touched = false;
	
	public OverlayTapCatchNode(){
		draws = false;
		transforms = false;
		handlesInteraction = true;

		zLevel = 10;		// it's an overlay -> bring it to the front!
	}

	public IOverlayTapCatchListener getListener() {
		return listener;
	}
	
	public void setListener(IOverlayTapCatchListener listener) {
		this.listener = listener;
	}
	
	@Override
	protected boolean onInteraction(InteractionContext ic) {
		return interpreter.onInteraction(ic);
	}

	@Override
	public boolean inBounds(float[] xy) {
		return true;
	}

	@Override
	public void down(InteractionContext ic) {
		touched = true;
		if (listener != null) listener.downOnOverlay(ic);
	}

	@Override
	public void cancel(InteractionContext ic) {
		touched = false;
		if (listener != null) listener.upOnOverlay(ic);
	}

	@Override
	public void click(InteractionContext ic) {
		touched = false;
		if (listener != null) listener.upOnOverlay(ic);
	}
	
	public boolean isTouched() {
		return touched;
	}

	public static interface IOverlayTapCatchListener {
		void downOnOverlay(SceneGraphContext sc);
		void upOnOverlay(SceneGraphContext sc);
	}
}
