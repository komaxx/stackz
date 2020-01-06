package com.suchgame.stackz.gl;

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.gl.GlConfig.ColorDepth;
import com.suchgame.stackz.gl.scenegraph.SceneGraph;
import com.suchgame.stackz.gl.texturing.Texture;
import com.suchgame.stackz.gl.util.KoLog;

/**
 * This is a basic implementation of a Renderer that handles a single SceneGraph
 * and automatically issues SceneGraph traversals to recreate surfaces and draw.
 * 
 * @author Matthias Schicker
 */
@SuppressLint("ViewConstructor")
public class BasicSceneGraphRenderView extends GLSurfaceView implements Renderer {
//	private static final int LONG_CLICK_DELAY_MS = 500;
	
	private static final int MAX_EVENT_POOL_SIZE = 15;

	protected SceneGraph sceneGraph;
	protected GlConfig glConfig;
	
	private final ArrayList<InteractionEventRunnable> interactionEventPool = new ArrayList<InteractionEventRunnable>();
	private ArrayList<MotionEvent> motionEventQueue = new ArrayList<MotionEvent>();
	

	public BasicSceneGraphRenderView(Context context, AttributeSet set,
			GlConfig glConfig, RenderProgramStore renderProgramStore) {
		super(context, set);
		this.glConfig = glConfig;

		if (isInEditMode()) return;
		
		// Check if the system supports OpenGL ES 2.0.
	    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
	    
	    boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000 
	    		|| Build.FINGERPRINT.startsWith("generic");
	    if (!supportsEs2) KoLog.e(this, "OpenGLES 2.0 NOT SUPPORTED!");
		
		setPreserveGlContextIfPossible();
		setEGLContextClientVersion(2);
		
		if (Build.FINGERPRINT.startsWith("generic")){
			setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
		} else {
			setEGLConfigChooser(new GlConfigChooser(glConfig));
		}
		
		if (glConfig.translucentBackground){
			getHolder().setFormat(PixelFormat.TRANSLUCENT);
			setZOrderOnTop(true);
		} else {
			getHolder().setFormat(
					glConfig.colorDepth == ColorDepth.Color8888 ? 
							PixelFormat.RGBA_8888 : PixelFormat.RGB_565);
		}
		
		Texture.setHighResolutionColor(glConfig.colorDepth==ColorDepth.Color8888);
		
		sceneGraph = new SceneGraph(context, this, renderProgramStore);
		setRenderer(this);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

	private void setPreserveGlContextIfPossible() {
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.FROYO){
			Method[] declaredMethods = GLSurfaceView.class.getDeclaredMethods();
			for (Method m : declaredMethods){
				if (m.getName().equals("setPreserveEGLContextOnPause")){
					try {
						m.invoke(this, true);
//						KoLog.v(this, "setPreserveEGLContextOnPause is set.");
					} catch (Exception ex) {
						KoLog.i(this, "Could not setPreserveEGLContextOnPause. No worries, should still work (but slower).");
						ex.printStackTrace();
					}
				}
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// renderer
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		sceneGraph.surfaceChanged(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		sceneGraph.surfaceCreated();
	}
	
	public SceneGraph getSceneGraph() {
		return sceneGraph;
	}
	
	@Override
	public void onResume() {
		sceneGraph.onResume();
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		sceneGraph.onPause();
	}
	
	public void onDestroy() {
		sceneGraph.onDestroy();
	}

	// /////////////////////////////////////////////////////////////
	// new implementation of the touch event handling for less latency

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		synchronized (this) {
			this.motionEventQueue.add(MotionEvent.obtain(event));
		}
		return true;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		doInteraction();
		
		try {
			sceneGraph.render();
		} catch (Exception e){
			KoLog.e(this, "Exception in onDrawFrame: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	//* one per frame approach + folding
	private void doInteraction() {
		synchronized (this) {
			// caution-measure to avoid touch-point flooding
			int queueSize = motionEventQueue.size(); 
			if (queueSize > 2){
				doInteractionFolding();
			} else if (queueSize > 0) {
				// only one per frame
				MotionEvent me = this.motionEventQueue.remove(0);
				try {
					sceneGraph.handleInteraction(me);
				} catch (Exception e){
					KoLog.e(this, "Exception when handling interaction: " + e.getLocalizedMessage());
					e.printStackTrace();
				} finally {
					me.recycle();
				}
			}
		}
	}
	//*/

	//* 'folding' approach (discard movement touch events)
	private void doInteractionFolding() {
//		synchronized (this) {
			while(!this.motionEventQueue.isEmpty()){
				MotionEvent me = this.motionEventQueue.remove(0);
				// skip this one?
				if (motionEventQueue.size() > 0){
					if (me.getAction() == MotionEvent.ACTION_MOVE 
							&& motionEventQueue.get(0).getAction() == MotionEvent.ACTION_MOVE){
						// ignore, the next one is also a 'move' and obliterates this one!
						continue;
					}
				}
				
				try {
					sceneGraph.handleInteraction(me);
				} catch (Exception e){
					KoLog.e(this, "Exception when handling interaction: " + e.getMessage());
					e.printStackTrace();
				} finally {
					me.recycle();
				}
			}
//		}
	}


	// new implementation of the touch event handling for less latency
	// /////////////////////////////////////////////////////////////
	// old implementation of the touch event handling
	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Runnable eventFromPool = getEventFromPool(event);
		queueEvent(eventFromPool);
		return true;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		try {
			sceneGraph.render();
		} catch (Exception e){
			KoLog.e(this, "Exception in onDrawFrame: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private Runnable getEventFromPool(MotionEvent me) {
		InteractionEventRunnable ret = null;
		synchronized (interactionEventPool){
			if (interactionEventPool.size() <= 0){
				ret = new InteractionEventRunnable();
			} else {
				ret = interactionEventPool.remove(interactionEventPool.size()-1);
			}
		}
		ret.setMotionEvent(MotionEvent.obtain(me));
		return ret;
	}
	
	// old implementation of the touch event handling
	// ////////////////////////////////////////////////////////////*/ 

	private void recycleInteractionEvent(InteractionEventRunnable toRecycle){
		synchronized (interactionEventPool) {
			if (interactionEventPool.size() < MAX_EVENT_POOL_SIZE){
				interactionEventPool.add(toRecycle);
			}
			toRecycle.me.recycle();
			toRecycle.me = null;
		}
	}
	
	private class InteractionEventRunnable implements Runnable {
		private MotionEvent me;

		@Override
		public void run() {
			try {
				sceneGraph.handleInteraction(me);
			} catch (Exception e){
				e.printStackTrace();
			}
			recycleInteractionEvent(this);
		}
		@SuppressWarnings("unused")
		public void setMotionEvent(MotionEvent me) {
			this.me = me;
		}
		@Override
		public String toString() {
			int pointerCount = me.getPointerCount();
			StringBuilder ret = new StringBuilder(pointerCount + ": ");
			for (int i = 0; i < pointerCount; i++){
				ret.append(me.getX(i)).append('|').append(me.getY(i)).append(", ");
			}
			return ret.toString();
		}
	}

	/**
	 * Called by the scene graph. By now, the view will not be empty for sure.
	 */
	public void onSecondFrameRendered() {
		setBackgroundDrawable(null);
		if (!glConfig.translucentBackground && getParent() != null){
			((View)getParent()).setBackgroundDrawable(null);
		}
	}
}
