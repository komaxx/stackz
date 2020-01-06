package com.suchgame.stackz.gl.scenegraph.interaction;


/**
 * Handles interaction events and interprets them as fit for
 * a button.
 * 
 * @author Matthias Schicker
 */
public class ButtonInteractionInterpreter {
	protected IButtonElement element;

	private int boundPointerIndex = -1;
	

	/**
	 * Only to be used for heirs. Note: You <b>MUST</b> set element in the
	 * child's constructor!
	 */
	protected ButtonInteractionInterpreter(){
	}
	
	public ButtonInteractionInterpreter(IButtonElement element){
		this.element = element;
	}
	
	public boolean onInteraction(InteractionContext ic){
		if (boundPointerIndex == -1 && ic.getAction() != Pointer.DOWN){
			return false;
		}
		
		float[] rayPoint = null;
		if (boundPointerIndex == -1){		// obviously: action is DOWN
			Pointer pointer = ic.getPointers()[ic.getActionIndex()];
			pointer.moveRaypointsToZ0plane(ic);
			rayPoint = pointer.getRayPoint();
			if (element.inBounds(rayPoint)){
				boundPointerIndex = ic.getActionIndex();
				element.down(ic);
				return true;
			}
		} else {
			Pointer pointer = ic.getPointers()[boundPointerIndex];
			int action = pointer.getAction();
			pointer.moveRaypointsToZ0plane(ic);
			rayPoint = pointer.getRayPoint();
			if (action == Pointer.MOVE && ( /* !element.inBounds(rayPoint) || */ pointer.tapRangeLeft())){
				abort(ic);
				return false;
			} else if (action == Pointer.UP){
				if (pointer.tapRangeLeft()){
					abort(ic);
					return false;
				} 
				element.click(ic);
				boundPointerIndex = -1;
				return true;
			} else if (action != Pointer.MOVE){		// some other freaky action
				abort(ic);
				return false;
			}
			return true;
		}
		
		return false;
	}
	
	private void abort(InteractionContext ic) {
		if (boundPointerIndex != -1) element.cancel(ic);
		boundPointerIndex = -1;
	}

	public static interface IButtonElement {
		boolean inBounds(float[] xy);
		
		void down(InteractionContext ic);
		
		void cancel(InteractionContext ic);
		
		void click(InteractionContext ic);
	}
	
	/**
	 * Simple class that purely removes the need to implement 'down' and 'cancel'
	 * in objects using the interpreter.
	 */
	public static abstract class AButtonElement implements IButtonElement {
		@Override
		public boolean inBounds(float[] xy) {
			return false;
		}

		@Override
		public void down(InteractionContext ic) { }
		@Override
		public void cancel(InteractionContext ic) { }
	}
}
