package com.suchgame.stackz.gl.scenegraph.interaction;

import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.util.Interpolators;

/**
 * Interprets interaction as fit for a something scroll- and flingable.
 * (scrolled, flinged, clicked).
 * 
 * @author Matthias Schicker
 */
public class FlingScrollInteractionInterpreter {
	public static final long CENTER_DURATION_NS = 500000000L;
	public static final long FLING_DURATION_NS = 1000000000L;
	
	private final IFlingable flingable;
	
	private MotionEventHistory moveEventHistory = new MotionEventHistory(15);
	private Interpolators.Interpolator flingInterpolator;

	private float currentScrollOffset = 0;

	private boolean consumeTouchEvents = true;
	
	private float scrollLimitMax = 10000;
	private float scrollLimitMin = 0;
	
	private long flingDuration = FLING_DURATION_NS;
	private long centerDuration = CENTER_DURATION_NS;
	
	private IScrollListener scrollListener;
	private IFlingListener flingListener;
	
	private int boundPointerIndex = -1;

	// ///////////////////////////////////////
	// tmps, caches
	protected float lastInteraction;

	
	
	public FlingScrollInteractionInterpreter(IFlingable flingable){
		this.flingable = flingable;
	}
	
	/**
	 * To be called each frame.
	 * @return	<code>true</code> when the scrollOffset was actually changed.
	 */
	public boolean proceed(SceneGraphContext sgContext) {
		boolean ret = false;
		if (boundPointerIndex < 0){
			ret = executeFlinging(sgContext);
		} else {  // currently interacting
			ret = true;
		}
		return ret;
	}
	
	public float getCurrentScrollOffset() {
		float ret = -currentScrollOffset;
		// slow down movements outside of scrollbounds
		if (ret > scrollLimitMax){
			ret = scrollLimitMax + (ret-scrollLimitMax)*0.3f;
		} else if (ret < scrollLimitMin){
			ret = scrollLimitMin - (scrollLimitMin - ret)*0.3f;
		}

		return -ret;
	}
	
	protected boolean executeFlinging(SceneGraphContext sgContext) {
		if (flingInterpolator == null) return false;
		float nuOffset = flingInterpolator.getValue(sgContext.frameNanoTime);
		
		if (-nuOffset > scrollLimitMax && flingInterpolator.getEndY() < nuOffset){
			// moving outside of the bounds -> clamp
			nuOffset = -scrollLimitMax;
			setFlingInterpolator(null);
		} else if (-nuOffset < scrollLimitMin && flingInterpolator.getEndY() > nuOffset){
			// moving outside of the bounds -> clamp
			nuOffset = -scrollLimitMin;
			setFlingInterpolator(null);
		} else if (sgContext.frameNanoTime > flingInterpolator.getEndX()){
			setFlingInterpolator(null);
		}
		
		updateScrollOffset(sgContext, nuOffset);
		
		if (flingInterpolator == null){		// the flinging was just finished
			if (scrollListener != null) scrollListener.handleScrollingFinished(sgContext, currentScrollOffset);
		}
		
		return true;	
	}

	private void move(InteractionContext ic){
		Pointer pointer = ic.getPointers()[boundPointerIndex];
		pointer.moveRaypointsToZ0plane(ic);
		
		float nowInteraction = pointer.getScreenCoords()[1];

		float tmpMoveDelta = nowInteraction - lastInteraction;
		moveEventHistory.add(ic.frameNanoTime, tmpMoveDelta, tmpMoveDelta);
		updateScrollOffset(ic, currentScrollOffset + tmpMoveDelta);
		
		lastInteraction = nowInteraction;
	}
	
	private void updateScrollOffset(SceneGraphContext sc, float nuScrollOffset){
		if (nuScrollOffset == currentScrollOffset) return;
		
		currentScrollOffset = nuScrollOffset;
		if (scrollListener != null) scrollListener.handleScrolling(sc, currentScrollOffset);
	}
	
	public boolean onInteraction(InteractionContext ic) {
		if (boundPointerIndex == -1){
			if (ic.getAction() == Pointer.CANCEL || ic.getAction() == Pointer.UP) return false;
			
			int nowPointerIndex = (ic.getAction()==Pointer.MOVE) ? 0 : ic.getActionIndex();
			
			Pointer pointer = ic.getPointers()[nowPointerIndex];
			pointer.moveRaypointsToZ0plane(ic);
			float[] rayPoint = pointer.getRayPoint();
			if (flingable.inBounds(rayPoint)){
				boundPointerIndex = nowPointerIndex;
				lastInteraction = pointer.getScreenCoords()[1];
				
				if (scrollListener != null){
					scrollListener.handleScrollingStarted(ic, currentScrollOffset);
				}
				
				if (consumeTouchEvents) return true;
			}
		} else {
			Pointer pointer = ic.getPointers()[boundPointerIndex];
			int action = pointer.getAction();
			pointer.moveRaypointsToZ0plane(ic);
			
			setFlingInterpolator(null);
			if (pointer.tapRangeLeft()) move(ic);
			
			if (action == Pointer.UP){
				computeFlinging(ic);
				moveEventHistory.clear();
				boundPointerIndex = -1;
			} else if (action == Pointer.CANCEL){
				moveEventHistory.clear();
				boundPointerIndex = -1;
			}
	
			// snap back to allowed area
			if (pointer.isUpOrCancel() && -currentScrollOffset > scrollLimitMax){
				centerOn(ic, scrollLimitMax);
			} else if (pointer.isUpOrCancel() && -currentScrollOffset < scrollLimitMin){
				centerOn(ic, scrollLimitMin);
			}
			
			if (consumeTouchEvents || pointer.tapRangeLeft()) return true;
		}
		
		return false;
	}
	
	private void computeFlinging(InteractionContext interactionContext) {
        // compute the fling speed
        long historyTime = 0;
        float historyDistance = 0;
        
        long frameTime = interactionContext.frameNanoTime;
        
        MotionEventHistory.HistoryMotionEvent historyEvent;
        int historyCount = 0;
        for (; historyCount < moveEventHistory.size(); historyCount++){
            historyEvent = moveEventHistory.get(historyCount);
            historyTime = frameTime - historyEvent.time;
            historyDistance += historyEvent.x;
            if (historyTime > 200000000) break;
        }
        
        if (moveEventHistory.size() >= 3 && historyTime > 0){
        	double flingSpeed = (double)historyDistance/(double)historyTime;

        	if (Math.abs(flingSpeed) > RenderConfig.MIN_FLING_SPEED){
        		setFlingInterpolator(
	                new Interpolators.AttenuationInterpolator(
	                		currentScrollOffset, frameTime, frameTime + flingDuration, flingSpeed ));
        	}
        } 

       	if (flingInterpolator == null && scrollListener != null){
       		scrollListener.handleScrollingFinished(interactionContext, currentScrollOffset);
       	}
        moveEventHistory.clear();
	}

	public void centerOn(SceneGraphContext sc, float y) {
		// avoid 'invisible' scrolling
		if (Math.abs(currentScrollOffset + y) < 6){		// max jump distance that still seems fluid
			setFlingInterpolator(null);
			updateScrollOffset(sc, -y);
			boundPointerIndex = -1;
		} else {
			long frameTime = sc.frameNanoTime;
			setFlingInterpolator(
					new Interpolators.HyperbelInterpolator(currentScrollOffset, -y, 
							frameTime, frameTime + centerDuration));
			boundPointerIndex = -1;
		}
	}
	
	/**
	 * Directly set the scroll offset without animation.
	 */
	public void jumpTo(SceneGraphContext sc, float y){
		setFlingInterpolator(null);
		boundPointerIndex = -1;
		updateScrollOffset(sc, y);
	}
	
	private void setFlingInterpolator(Interpolators.Interpolator nuFlinger) {
		if (this.flingInterpolator==null && nuFlinger != null){
			if (this.flingListener != null) flingListener.handleFlingingStarted();
		} else if (this.flingInterpolator!=null && nuFlinger==null){
			if (this.flingListener != null) flingListener.handleFlingingEnded();
		}
		
		this.flingInterpolator = nuFlinger;
	}

	/**
	 * Defines how far the view will be flinged. The default value is
	 * FLING_DURATION_NS.
	 */
	public void setFlingDuration(long nuFlingDuration){
		flingDuration = nuFlingDuration;
	}
	
	/**
	 * Defines how quickly the view will center on a spot. The default value is
	 * CENTER_DURATION_NS.
	 */
	public void setCenterDuration(long nuCenterDuration){
		centerDuration = nuCenterDuration;
	}
	
	/**
	 * Will clamp values to scrollLimitMin!
	 */
	public void setScrollLimitMax(float scrollLimitMax) {
		this.scrollLimitMax = Math.max(scrollLimitMin, scrollLimitMax);
	}
	
	public void setScrollLimitMin(float scrollLimitMin) {
		this.scrollLimitMin = scrollLimitMin;
	}
	
	
	public static interface IFlingable {
		public boolean inBounds(float[] p);
	}

	public void setConsumeTouchEvent(boolean consume) {
		consumeTouchEvents = consume;
	}
	
	public void setFlingListener(IFlingListener flingListener) {
		this.flingListener = flingListener;
	}
	
	public void setScrollListener(IScrollListener scrollListener) {
		this.scrollListener = scrollListener;
	}
	
	public float getScrollLimitMinimum() {
		return scrollLimitMin;
	}

	public float getScrollLimitMaximum() {
		return scrollLimitMax;
	}
	
	/**
	 * To be set at a FlingScrollInterpreter. Will be notified when scrolling events
	 * occur (start, update, done). 
	 */
	public static interface IScrollListener {
		void handleScrollingStarted(SceneGraphContext sc, float nuOffset);
		void handleScrolling(SceneGraphContext sc, float nowOffset);
		void handleScrollingFinished(SceneGraphContext sc, float currentOffset);
	}
	
	/**
	 * To be set at a FlingScrollInterpreter. Will be notified when flinging happens.
	 */
	public static interface IFlingListener {
		void handleFlingingStarted();
		void handleFlingingEnded();
	}
}
