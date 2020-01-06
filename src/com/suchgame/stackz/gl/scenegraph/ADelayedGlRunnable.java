package com.suchgame.stackz.gl.scenegraph;

import com.suchgame.stackz.gl.RenderContext;

/**
 * A simple implementation of the IGlRunnable that will reschedule
 * itself until a given delay is reached.
 * 
 * @author matthias.schicker
 */
public abstract class ADelayedGlRunnable implements IGlRunnable {
	private final Node queueNode;
	private final int delayMs;
	private long triggerTime;
	
	private boolean scheduleToIdle = false;

	public ADelayedGlRunnable(Node queueNode, int delayMs){
		this.queueNode = queueNode;
		this.delayMs = delayMs;
	}
	
	public void setScheduleToIdle(boolean scheduleToIdle) {
		this.scheduleToIdle = scheduleToIdle;
	}
	
	@Override
	public final void run(RenderContext rc) {
		if (triggerTime == 0){
			triggerTime = rc.frameNanoTime + delayMs * 1000L * 1000L;
			reschedule();
		} else if (rc.frameNanoTime < triggerTime){
			reschedule();
		} else {
			doRun(rc);
		}
	}

	protected abstract void doRun(RenderContext rc);

	private final void reschedule() {
		SceneGraph sceneGraph = queueNode.sceneGraph;
		if (scheduleToIdle && sceneGraph != null) sceneGraph.queueIdleJob(this);
		else queueNode.queueOnceInGlThread(this);
		
	}
	
	/**
	 * Resets the trigger, so the run method will only be executed
	 * after the set delay.
	 */
	public void postponeTrigger(){
		triggerTime = 0;
	}
}
