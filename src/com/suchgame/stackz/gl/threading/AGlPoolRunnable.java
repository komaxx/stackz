package com.suchgame.stackz.gl.threading;

import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;

@SuppressWarnings("rawtypes")
public abstract class AGlPoolRunnable implements IGlRunnable {
	private GlRunnablePool pool;

	@Override
	@SuppressWarnings("unchecked")
	public final void run(RenderContext rc){
		doRun(rc);
		pool.recycle(this);
		reset();
	}

	/**
	 * Called when recycling the APoolRunnable. Remove lingering references
	 * to avoid memory leaks.
	 */
	protected abstract void reset();

	protected abstract void doRun(RenderContext rc);
	protected abstract void doAbort();
	
	public void setPool(GlRunnablePool pool){
		this.pool = pool;
	}
	
	public abstract void set(Object... params);
}