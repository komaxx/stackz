package com.suchgame.stackz.gl.scenegraph.interaction;


/**
 * Very simple interaction interpreter that consumes all interaction until
 * the touch's tap range is left. Useful to make lists and other scrolling
 * objects less jittery.
 * 
 * @author Matthias Schicker
 */
public class GuardInteractionInterpreter  {
	private final IGuardElement element;
	
	private int boundPointerIndex = -1;
	
	public GuardInteractionInterpreter(IGuardElement element){
		this.element = element;
	}

	public boolean onInteraction(InteractionContext ic){
		if (boundPointerIndex == -1 && ic.getAction() != Pointer.DOWN){
			return false;
		}
		
		if (boundPointerIndex == -1){		// obviously: action is DOWN
			Pointer pointer = ic.getPointers()[ic.getActionIndex()];
			pointer.moveRaypointsToZ0plane(ic);
			float[] rayPoint = pointer.getRayPoint();
			if (element.inBounds(rayPoint)){
				boundPointerIndex = ic.getActionIndex();
				return true;
			}
		} else {
			if (ic.getPointers()[boundPointerIndex].tapRangeLeft()){
				boundPointerIndex = -1;
				return false;
			}

			if (ic.getAction() == Pointer.UP || ic.getAction() == Pointer.CANCEL){
				boundPointerIndex = -1;
			}
			return true;
		}
		
		return false;	
	}
	
	public static interface IGuardElement {
		boolean inBounds(float[] xy);
	}
}
