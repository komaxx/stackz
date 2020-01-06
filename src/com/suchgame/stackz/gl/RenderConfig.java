package com.suchgame.stackz.gl;

public class RenderConfig {
	/**
	 * Caution: MASSIVE performance hit.
	 */
	public static boolean GL_DEBUG = false;
	
	/**
	 * When true, the current fps will be written to the log or a registered TextView
	 */
	public static final boolean PRINT_FPS = false;

	public static boolean PROFILING = false;
	
	/**
	 * To be set in <code>setDeviceDependents</code>!
	 */
	public static float TAP_DISTANCE = 10;

	/**
	 * When the movement delta is smaller than this, no flinging is executed.
	 * (worldX per NS)
	 */
	public static float MIN_FLING_SPEED = 0.00000001f;
 

	public static int TAP_MAX_TIME_MS = 250;

	/**
	 * The maximum size that is supported by the local hardware. Read out from
	 * the GL and set in SceneGraph when the surface for the GL is created for 
	 * the first time. 
	 */
	public static int MAX_TEXTURE_SIZE = 1024;

	/**
	 * When the view is idle, the sceneGraph will execute idleJobs until
	 * this amount of time was spent or the max number of jobs was reached
	 * Remaining jobs are rescheduled for the next frame.
	 */
	public static final int UI_IDLE_MAX_EXECUTION_TIME_MS = 150;
	
	/**
	 * Same as UI_IDLE_MAX_EXECUTION_TIME_MS but in the not-idle state.
	 */
	public static final int UI_NOT_IDLE_MAX_EXECUTION_TIME_MS = 5;
	
	/**
	 * When the view is idle, the sceneGraph will execute at most this amount
	 * of idleJobs or until the max amount of time was spent.
	 * Remaining jobs are rescheduled for the next frame.
	 */
	public static final int UI_IDLE_MAX_JOBS = 5;
	/**
	 * Same as UI_IDLE_MAX_JOBS but in the not-idle state.
	 */
	public static final int UI_NOT_IDLE_MAX_JOBS = 0;

	/**
	 * TODO Investigate whether turning this off is actually helpful! May introduce
	 * memory issues!!
	 * 
	 * Set false to turn off all Bitmap.recycle calls. May help to solve a
	 * tombstoning problem...
	 */
	public static final boolean RECYCLE_BITMAPS = true;

	/**
	 * When true, the texture filtering will be degraded to NEAREST while the
	 * render system is 'not idle'.
	 */
	public static final boolean DEGRADE_TEXTURE_FILTERING_ON_NOT_IDLE = false;
	
	/**
	 * VBO rendering is the more powerful way to render stuff, with the geometry
	 * data living in GL memory. Unfortunately only supported from Android 2.3
	 */
	public static boolean VBO_RENDERING = true;
	
	
	public static void setDeviceDependents(
			float flingPixelsPerSecond,
			float tapDistance){
		MIN_FLING_SPEED = flingPixelsPerSecond / (1000f*1000f*1000f);
		TAP_DISTANCE = tapDistance;
	}
}
