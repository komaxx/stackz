package com.suchgame.stackz.gl.threading;

import java.util.Stack;

public abstract class GlRunnablePool<T extends AGlPoolRunnable> {
	private static final int DEF_POOL_SIZE = 5;
	private int maxPoolSize;
	
	public GlRunnablePool(){
		this(DEF_POOL_SIZE);
	}
	
	public GlRunnablePool(int maxPoolSize){
		this.maxPoolSize = maxPoolSize;
	}

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
			if (pool.size() >= maxPoolSize) return;
			pool.push(toRecycle);
		}
	}
}
