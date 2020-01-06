package com.suchgame.stackz.gl.util;

/**
 *
 * Created by Matthias Schicker on 12/17/13.
 */
public class Semaphore {
    /**
     * Encapsulates <code>Object.wait()</code> in a synchronized block.
     * <b>Catches and ignores <code>InterruptedException</code>s!</b>. Do not
     * use this method if you need to know about the occurrence of that Exception.
     */
    public void waitS() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored!
            }
        }
    }

    /**
     * Encapsulates <code>Object.wait(millis)</code> in a synchronized block.
     * <b>Catches and ignores <code>InterruptedException</code>s!</b>. Do not
     * use this method if you need to know about the occurrence of that Exception.
     */
    public void waitS(long millis) {
        synchronized (this) {
            try {
                wait(Math.max(1, millis));				// argh, drex Java: wait(0) blocks FOREVER!
            } catch (InterruptedException e) {
                // ignored!
            }
        }
    }

    public void notifyS() {
        synchronized (this) {
            notify();
        }
    }

    public void notifyAllS() {
        synchronized (this) {
            notifyAll();
        }
    }
}
