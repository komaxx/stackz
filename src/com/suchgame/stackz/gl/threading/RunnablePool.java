package com.suchgame.stackz.gl.threading;

import java.util.Stack;

public abstract class RunnablePool<T extends APoolRunnable> {
	private static final int MAX_POOL_SIZE = 20;
	
	private Stack<T> pool = new Stack<T>();
	
	public T get(Object... params){
		synchronized (pool) {
			T ret;
			if (pool.size() < 1){
				ret = createPoolRunnable();
				ret.setPool(this);
			} else {
				ret = pool.pop();
			}
			ret.set(params);
			return ret;
		}
	}
	
	protected abstract T createPoolRunnable();

	public void recycle(T toRecycle) {
		synchronized (pool) {
			if (pool.size() >= MAX_POOL_SIZE) return;
			pool.push(toRecycle);
		}
	}
}
