package com.suchgame.stackz.gl.scenegraph.interaction;

import android.view.MotionEvent;

import com.suchgame.stackz.gl.BooleanStack;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;

/**
 * A InteractionContext is an object that will be available for every node when
 * traversing through the scene graph for interaction. 
 * It contains the current state of the GL and other states that are necessary 
 * for interaction handling.
 * 
 * @author Matthias Schicker
 */
public class InteractionContext extends SceneGraphContext {
	public static final int MAX_POINTER_COUNT = 10;
	
	private int activePointerCount = 0;
	private Pointer[] pointers = new Pointer[MAX_POINTER_COUNT];
	
	/**
	 * If this is turned false, rendering and transformation will be suspended
	 * for all subsequent nodes.
	 */
	public BooleanStack interactiveStack = new BooleanStack(true);
	
	/**
	 * The one action that triggered this interaction handling.
	 * Note: MOVE may mean that several pointers have moved!
	 * See Pointer.xxx constants for possible values.
	 */
	private int action = Pointer.INACTIVE;
	/**
	 * DOWN/UP/CANCEL events always affect only one pointer. This index
	 * will tell you, which.
	 */
	private int actionIndex = -1;
	
	public InteractionContext(){
		for (int i = 0; i < MAX_POINTER_COUNT; i++){
			pointers[i] = new Pointer();
		}
	}
	
	public final void reset(InteractionContext basicInteractionContext) {
		basicReset(basicInteractionContext);
		interactiveStack.reset();
		for (int i = 0; i < MAX_POINTER_COUNT; i++){
			pointers[i].reset();
		}
	}

	@Override
	public String toString() {
		return activePointerCount + " active pointers";
	}

	public void update(MotionEvent me) {
		// deactivate upped/canceled pointers
		int lastAction;
		for (int i = 0; i < MAX_POINTER_COUNT; i++){
			lastAction = pointers[i].getAction();
			if (lastAction == Pointer.CANCEL || lastAction == Pointer.UP){
				pointers[i].setInactive();
			}
		}
		
		// first: Check the (easy) single-pointer events
		int action = me.getAction();
		if (action == MotionEvent.ACTION_MOVE){
			this.action = Pointer.MOVE;
			// activePointerCount is unchanged
			// actionIndex does not apply in this case; multiple pointers may have been moved
			for (int i = 0; i < me.getPointerCount(); i++){
				int id = me.getPointerId(i);
				id = RenderUtil.clamp(id, 0, MAX_POINTER_COUNT-1);
				pointers[id].set(Pointer.MOVE, me.getX(i), me.getY(i));
			}
			this.actionIndex = -1;
		} else if (action == MotionEvent.ACTION_DOWN){
			pointers[0].set(Pointer.DOWN, me.getX(), me.getY());
			actionIndex = 0;
			this.action = Pointer.DOWN;
			activePointerCount = 1;
		} else if (action == MotionEvent.ACTION_UP){	// the last interaction point was "up"ed
			actionIndex = me.getPointerId(me.getActionIndex());
			actionIndex = RenderUtil.clamp(actionIndex, 0, MAX_POINTER_COUNT-1);
			pointers[actionIndex].set(Pointer.UP, me.getX(), me.getY());
			this.action = Pointer.UP;
			activePointerCount = 1;
		} else if (action == MotionEvent.ACTION_CANCEL){
			actionIndex = me.getPointerId(me.getActionIndex());
			actionIndex = RenderUtil.clamp(actionIndex, 0, MAX_POINTER_COUNT-1);
			pointers[actionIndex].set(Pointer.CANCEL, me.getX(), me.getY());
			this.action = Pointer.CANCEL;
			activePointerCount = 1;
		} else {	// pointer action!
			int eventActionIndex = me.getActionIndex();
			action = me.getActionMasked();
			actionIndex = me.getPointerId(eventActionIndex);
			actionIndex = RenderUtil.clamp(actionIndex, 0, MAX_POINTER_COUNT-1);
			if (action == MotionEvent.ACTION_POINTER_DOWN){
				pointers[actionIndex].set(Pointer.DOWN, me.getX(eventActionIndex), me.getY(eventActionIndex));
				this.action = Pointer.DOWN;
			} else if (action == MotionEvent.ACTION_POINTER_UP){
				pointers[actionIndex].set(Pointer.UP, me.getX(eventActionIndex), me.getY(eventActionIndex));
				this.action = Pointer.UP;
			} else {
				KoLog.w(this, "WEIRD: Unknown action: " + action);
			}
			
			activePointerCount = me.getPointerCount();
		}
	}

	public Pointer[] getPointers() {
		return pointers;
	}
	
	/**
	 * Convenience method for getPointers()[getActionIndex].
	 * Only valid when action != MOVE
	 */
	public Pointer getActionPointer(){
		return pointers[actionIndex];
	}
	
	public int getAction() {
		return action;
	}
	
	public int getActionIndex() {
		return actionIndex;
	}
	
	public int getActivePointerCount() {
		return activePointerCount;
	}

	public boolean isUpOrCancel() {
		return action == Pointer.UP || action == Pointer.CANCEL;
	}
}
