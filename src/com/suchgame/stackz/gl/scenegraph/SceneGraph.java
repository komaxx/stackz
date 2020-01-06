package com.suchgame.stackz.gl.scenegraph;

import java.util.ArrayList;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Handler;
import android.view.MotionEvent;

import com.suchgame.stackz.gl.BasicSceneGraphRenderView;
import com.suchgame.stackz.gl.RenderConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.scenegraph.analysis.AnalysisNode;
import com.suchgame.stackz.gl.scenegraph.analysis.FpsProfiler;
import com.suchgame.stackz.gl.scenegraph.analysis.IRenderProfiler;
import com.suchgame.stackz.gl.scenegraph.analysis.ISceneGraphAnalysor;
import com.suchgame.stackz.gl.scenegraph.analysis.Linearization;
import com.suchgame.stackz.gl.scenegraph.analysis.Path;
import com.suchgame.stackz.gl.scenegraph.analysis.RenderProfiler;
import com.suchgame.stackz.gl.scenegraph.analysis.SceneGraphAnalysor;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.RootNode;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.texturing.TextureStore;
import com.suchgame.stackz.gl.threading.AOnScreenRunnable;
import com.suchgame.stackz.gl.threading.OnScreenHandlerThread;
import com.suchgame.stackz.gl.traversing.NodeFindVisitor;
import com.suchgame.stackz.gl.traversing.SceneGraphBroadFirstTraverser;
import com.suchgame.stackz.gl.traversing.SceneGraphDepthFirstTraverser;
import com.suchgame.stackz.gl.traversing.SurfaceChangedVisitor;
import com.suchgame.stackz.gl.traversing.SurfaceCreatedVisitor;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.gl.util.RenderUtil;

/**
 * This class holds and manages a SceneGraph. 
 * 
 * @author Matthias Schicker
 */
public class SceneGraph {
	private static final boolean DEBUG = false;
	
	private static final boolean DEBUG_IDLE_VISUALIZATION = false;

	private final BasicSceneGraphRenderView view;

	private RootNode root = new RootNode();

	private OnScreenHandlerThread offRenderThread;
	private Handler uiThreadHandler;
	
	private ARenderProgramStore renderProgramStore;
	
	private RenderContext basicRenderContext;
	private InteractionContext basicInteractionContext;
	private ISceneGraphAnalysor graphAnalysis = new SceneGraphAnalysor();
	
	/**
	 * "surfaceCreated" calls will be postponed until the size of the surface
	 * was notified to the sceneGraph!
	 */
	private boolean surfaceCreatedCallPending = true;
	
	/**
	 * idleRunnables are executed between frames. If a job only takes a few ms (<50)
	 * this is the right place to add it. Longer jobs should be executed in the 
	 * OffscreenRenderThread.
	 */
	private ArrayList<IGlRunnable> idleRunnables = new ArrayList<IGlRunnable>();

	private SceneGraphBroadFirstTraverser broadFirstTraverser = new SceneGraphBroadFirstTraverser();
	private SceneGraphDepthFirstTraverser depthFirstTraverser = new SceneGraphDepthFirstTraverser();
	
	private SurfaceCreatedVisitor surfaceCreatedVisitor = new SurfaceCreatedVisitor();
	private SurfaceChangedVisitor surfaceChangedVisitor = new SurfaceChangedVisitor();
	
	// temporary objects used during one frame
	private RenderContext frameRenderContext;
	private InteractionContext frameInteractionContext = new InteractionContext();

	private Path currentInteractionPath;
	
	private IRenderProfiler profiler = RenderConfig.PROFILING ? new RenderProfiler() : new FpsProfiler(null);
	
	private int clearBuffersMask = GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT;
	
	
	private float clearColorR = 0.3f;
	private float clearColorG = 0.3f;
	private float clearColorB = 0.3f;

	private boolean longClicked = false;

	
	/**
	 * Creates the scene graph. Must be called in UI thread.
	 * @param context 
	 * @param basicSceneGraphRenderView	will be available for all nodes.
	 */
	public SceneGraph(Context context, BasicSceneGraphRenderView view, ARenderProgramStore renderProgramStore){
		this.view = view;
		this.renderProgramStore = renderProgramStore;
		
		basicRenderContext = new RenderContext(renderProgramStore);
		basicRenderContext.sceneGraph = this;
		basicRenderContext.resources = view.getResources();
		basicRenderContext.depthTestActivated = true;
		basicRenderContext.textureStore = new TextureStore();
		
		basicInteractionContext = new InteractionContext();
		basicInteractionContext.sceneGraph = this;
		
		offRenderThread = new OnScreenHandlerThread("OffScreenRendering");
		uiThreadHandler = new Handler();
		
		frameRenderContext = new RenderContext(renderProgramStore);
	}
	
	public void onResume(){
		graphAnalysis.onResume();
		root.attach(this);
		offRenderThread.onResume();
		graphAnalysis.setDirty();
	}
	
	public void onPause(){
		graphAnalysis.onPause();
		offRenderThread.onPause();
		root.detach();
	}
	
	public void onDestroy(){
		graphAnalysis.onDestroy();
		offRenderThread.onDestroy();
	}
	
	/**
	 * GlThread. The interaction is handled asynchronously. The incoming MotionEvent
	 * will be recycled when the handling is done (in the asynchronous agent).
	 */
	public boolean handleInteraction(MotionEvent me){
		int action = me.getAction();

		if (longClicked){
			if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_OUTSIDE){
				longClicked = false;
			}
			return false;
		}
		
		prepareFrameInteractionContext();
		frameInteractionContext.update(me);
			
		if (currentInteractionPath != null){
			if (!handleInteractionInPath(currentInteractionPath)){
				currentInteractionPath = null;
				handleInteraction(me);
			}
		} else {
			handleInteractionTraversing(true);
		}

		if (frameInteractionContext.isUpOrCancel()){
			// the interaction is no longer locked to the last found Node!
			currentInteractionPath = null;
		}
		
		frameRenderContext.setNotIdle();
		
		return false;
	}
	
	private void prepareFrameInteractionContext(){
		frameInteractionContext.reset(basicInteractionContext);
		frameInteractionContext.frameNanoTime = System.nanoTime();
		frameInteractionContext.frame++;
	}
	
	private boolean handleInteractionTraversing(boolean lockToPath) {
		Linearization interactionLinearization = graphAnalysis.getInteractionLinearization(this);
		if (interactionLinearization == null) return false;

		int l = interactionLinearization.paths.size();
		for (int i = 0; i < l; i++){
			if (handleInteractionInPath(interactionLinearization.paths.get(i))){
				if (lockToPath){
					buildCurrentInteractionPath(
						interactionLinearization.paths.get(0).startNode,
						interactionLinearization.paths.get(i).endNode);
				}
				return true;
			}
		}
		return false;
	}

	private void buildCurrentInteractionPath(AnalysisNode root, AnalysisNode endNode) {
		currentInteractionPath = new Path(root, endNode);
		currentInteractionPath.compute();
	}

	private boolean handleInteractionInPath(Path path) {
		int l = path.pathUp.size() - 1;
		AnalysisNode nowNode;
		for (int i = 0; i < l; i++){
			nowNode = path.pathUp.get(i);
			nowNode.node.traversalUp(frameInteractionContext);
			nowNode.node.untransformInteraction(frameInteractionContext);
		}
		l = path.pathDown.size();
		for (int i = 1; i < l; i++){
			nowNode = path.pathDown.get(i); 
			nowNode.node.traversalDown(frameInteractionContext);
			nowNode.node.transformInteraction(frameInteractionContext);
		}
		return path.endNode.node.handleInteraction(frameInteractionContext);
	}

	public void postToOffRenderThread(AOnScreenRunnable toRun){
		offRenderThread.post(toRun);
	}
	
	/**
	 * Executes the Runnable in the UI thread.
	 */
	public void postToUiThread(Runnable r){
		uiThreadHandler.post(r);
	}
	
	private void executeIdleRunnables() {
		if (surfaceCreatedCallPending) return;
		
		boolean idle = frameRenderContext.isIdle();
		
		if (DEBUG_IDLE_VISUALIZATION){
			clearColorR = idle ? 0 : 1;
			GLES20.glClearColor(clearColorR, clearColorG, clearColorB, 0f);
		}
		
		long endTime = System.currentTimeMillis() + 
				(idle ? RenderConfig.UI_IDLE_MAX_EXECUTION_TIME_MS :
					   RenderConfig.UI_NOT_IDLE_MAX_EXECUTION_TIME_MS);
		int maxJobs = idle ? RenderConfig.UI_IDLE_MAX_JOBS : RenderConfig.UI_NOT_IDLE_MAX_JOBS;
		
		int i = 0;
		synchronized (idleRunnables) {
			while (!idleRunnables.isEmpty() && i<maxJobs && System.currentTimeMillis() < endTime){
				IGlRunnable nowJob = idleRunnables.remove(0);
				nowJob.run(frameRenderContext);
				i++;
			}
		}
		
//		if (i > 0) AqLog.i(this, (idle ? "IDLE! " : "NOT idle! ") + "Executed " + i + " idleJobs, " + idleRunnables.size() + " left");
	}

	/**
	 * Call this to mark the current linearizations as dirty, which in turn forces
	 * the SceneGraph to re-analyse the graph. To be used, when a node's <code>zLevel</code>
	 * or <code>clusterZlevel</code> is changed. Not necessary when adding or removing
	 * nodes; this will trigger a re-linearization anyway.
	 */
	public void invalidateLinearizations(){
		graphAnalysis.setDirty();
	}
	
	public void render() {
		Linearization renderLinearization = graphAnalysis.getRenderLinearization(this);
		if (renderLinearization == null) return;		// paused/aborted

		executeIdleRunnables();
		cleanBuffers(frameRenderContext);
		
		
		if (surfaceCreatedCallPending) return;		// do not draw until we know the right surface size!
		
		
		profiler.frameStart();
		
		frameRenderContext.reset(basicRenderContext);
		frameRenderContext.frameNanoTime = System.nanoTime();
		frameRenderContext.frame++;
		
//		root.dumbRender(frameRenderContext);		// only used for debugging!
		render(renderLinearization);
		
		profiler.frameDone(frameRenderContext);
		
		if (frameRenderContext.frame == 2){
			postToUiThread(new Runnable(){
				@Override
				public void run() {
					view.onSecondFrameRendered();
				}
			});
		}
	}

	private void render(Linearization renderLinearization) {
		profiler.globalRunnablesStart();
		root.renderSelf(frameRenderContext);		// executes IGlRunnables attached to root.
		profiler.globalRunnablesDone();
		
		int l = renderLinearization.paths.size();
		for (int i = 0; i < l; i++){
			renderPath(renderLinearization.paths.get(i));
		}
	}

	private void renderPath(Path path) {
		profiler.startPath(path);
		
		int l = path.pathUp.size() - 1;
		for (int i = 0; i < l; i++){
			path.pathUp.get(i).node.traversalUp(frameRenderContext);
		}
		
		l = path.pathDown.size();
		for (int i = 1; i < l; i++){
			path.pathDown.get(i).node.traversalDown(frameRenderContext);
		}
		path.endNode.node.renderSelf(frameRenderContext);
		
		if (RenderConfig.GL_DEBUG) RenderUtil.checkGlError("after "+ path.endNode);
		
		profiler.pathDone(path);
	}

	/**
	 * Executed at the beginning of each frame
	 * @param context 
	 */
	private void cleanBuffers(RenderContext context) {
		if (clearBuffersMask != 0) GLES20.glClear(clearBuffersMask);
	}

	/**
	 * Change whether at the beginning of each frame the color buffer (frame-buffer) 
	 * is cleaned (set to values defined in GLES20.GLES20.glClearColor(), a dark grey 
	 * per default) or not. </br>
	 * Default: Enabled.
	 */
	public void toggleColorBufferCleaning(boolean clean){
		if (clean) clearBuffersMask |= GLES20.GL_COLOR_BUFFER_BIT;
		else clearBuffersMask = clearBuffersMask & (~GLES20.GL_COLOR_BUFFER_BIT);
	}
	
	public void setBackgroundColor(float r, float g, float b){
		clearColorR = r;
		clearColorG = g;
		clearColorB = b;

		// now, make that change active in the next frame
		getRoot().queueInGlThread(new IGlRunnable() {
			@Override
			public void run(RenderContext rc) {
		    	GLES20.glClearColor(clearColorR, clearColorG, clearColorB, 0f);		// rgba
			}
		});
	}
	
	public void setBackgroundColor(int color){
		float[] rgb = RenderUtil.color2floatsRGBA(null, color);
		setBackgroundColor(rgb[0],rgb[1],rgb[2]);
	}

	/**
	 * Change whether at the beginning of each frame the depth-buffer is cleaned
	 * (set to a value defined in GLES20.glClearDepthf(), 0 per default) or not.
	 * Default: Enabled.
	 */
	public void toggleDepthBufferCleaning(boolean clean){
		if (clean) clearBuffersMask |= GLES20.GL_DEPTH_BUFFER_BIT;
		else clearBuffersMask = clearBuffersMask & (~GLES20.GL_DEPTH_BUFFER_BIT);
	}
	
	/**
	 * Adds a node to the sceneGraph. UI thread.
	 * @param parent	the Node that will receive the new node as child.
	 * Set <code>null</code> to add the node directly to the root.
	 * @param toAdd		the Node that is to be added to parent.
	 */
	public void addNode(Node parent, Node toAdd){
		if (parent == null) parent = root;
		parent.addChild(toAdd, this);
		root.queueInGlThread(new OnSurfaceCreatedJob(toAdd));
		toAdd.setParent(parent);
		graphAnalysis.setDirty();
	}
	
	/**
	 * Removed a node from the sceneGraph.
	 * @param toRemove		the Node that is to be removed from the graph.
	 */
	public void removeNode(Node toRemove){
		if (toRemove.getParent() != null) {
			toRemove.getParent().removeChild(toRemove);
			toRemove.setParent(null);
			graphAnalysis.setDirty();
		} else {
			KoLog.w(this, "Attempted to remove node that was not added before: " + toRemove 
					+". May cause disrupted node lifecycle, check your code!");
		}
	}
	
	public Node findNodeByName(String name){
		NodeFindVisitor visitor = new NodeFindVisitor(name);
		depthFirstTraverser.traverse(getRoot(), visitor);
		return visitor.getFoundNode();
	}
	
	public void surfaceChanged(int width, int height) {
		if (width < 2 || height < 2 
				|| (width==basicRenderContext.surfaceWidth && height==basicRenderContext.surfaceHeight)) return;
		
		GLES20.glViewport(0, 0, width, height);
		basicRenderContext.surfaceWidth = width;
		basicRenderContext.surfaceHeight = height;
		
		basicInteractionContext.surfaceWidth = width;
		basicInteractionContext.surfaceHeight = height;
		
		frameRenderContext.reset(basicRenderContext);
		frameInteractionContext.reset(basicInteractionContext);
		frameRenderContext.setNotIdle();
		if (surfaceCreatedCallPending){
			surfaceCreatedVisitor.setRenderContext(frameRenderContext);
			frameInteractionContext.reset(basicInteractionContext);
			broadFirstTraverser.traverse(root, surfaceCreatedVisitor);
			surfaceCreatedCallPending = false;
		} else {
			surfaceChangedVisitor.reset(frameRenderContext);
			broadFirstTraverser.traverse(root, surfaceChangedVisitor);
		}
		
		graphAnalysis.setDirty();
	}

	public void surfaceCreated() {
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glClearDepthf(1f);
    	GLES20.glClearColor(clearColorR, clearColorG, clearColorB, 1f);		// rgba
    	GLES20.glDisable(GLES20.GL_DITHER);
    	GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
    	GLES20.glDisable(GLES20.GL_STENCIL_TEST);
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		GLES20.glDepthFunc(GLES20.GL_LESS);
		
		readCaps();

		renderProgramStore.recreate();
		basicRenderContext.surfaceId++;
		basicRenderContext.resetRenderProgram();
		basicRenderContext.resetTextureStore();

		if (basicRenderContext.surfaceHeight > 1 && basicRenderContext.surfaceWidth > 1){
			frameRenderContext.reset(basicRenderContext);
			surfaceCreatedVisitor.setRenderContext(frameRenderContext);
			frameInteractionContext.reset(basicInteractionContext);
			broadFirstTraverser.traverse(root, surfaceCreatedVisitor);
		} else {
			surfaceCreatedCallPending = true;
		}
		
		graphAnalysis.setDirty();
		
	}
	
	private void readCaps() {
		int[] tmp = new int[2];
		GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, tmp, 0);
		RenderConfig.MAX_TEXTURE_SIZE = tmp[0];
		
		// TODO: this simple heuristic may not be adequate in all circumstances!
		int width = view.getWidth();
		if (width < 450){
			RenderConfig.MAX_TEXTURE_SIZE = 1024;
		}
		
		
		if (DEBUG){
			tmp = new int[10];
			KoLog.i(this, "========== GL environment caps ==========");
			KoLog.i(this, "GL_VENDOR: " + GLES20.glGetString(GLES20.GL_VENDOR));
			KoLog.i(this, "GL_RENDERER: " + GLES20.glGetString(GLES20.GL_RENDERER));
			KoLog.i(this, "GL_VERSION: " + GLES20.glGetString(GLES20.GL_VERSION));
			KoLog.i(this, "GL_SL_VERSION: " + GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION));
			KoLog.i(this, "GL_EXTENSIONS: " + GLES20.glGetString(GLES20.GL_EXTENSIONS));
			
			GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, tmp, 0);
			KoLog.i(this, "max combined texture units: " + tmp[0]);
			
			GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, tmp, 3);
			KoLog.i(this, "max renderbuffer size: " + tmp[3] + "x" + tmp[4]);
			
			GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, tmp, 5);
			KoLog.i(this, "max texture size: " + tmp[5] + "x" + tmp[6]);
			
			KoLog.i(this, "===================================");
		}
	}

	public SceneGraphBroadFirstTraverser getBroadFirstTraverser() {
		return broadFirstTraverser;
	}
	
	public SceneGraphDepthFirstTraverser getDepthFirstTraverser() {
		return depthFirstTraverser;
	}

	public Node getRoot() {
		return root;
	}
	
	/**
	 * Adds a runnable that is to be executed in the next frame. Thus, it is valid
	 * to queue a new Runnable in a IGlRunnable without causing a StackOverflowException.
	 */
	public final void queueIdleJob(IGlRunnable r){
		synchronized (idleRunnables) {
			idleRunnables.add(r);
		}
	}
	
	/**
	 * Same as queueIdleJob but the runnable will not be added if it's already queued.
	 */
	public final void queueIdleJobOnce(IGlRunnable r){
		synchronized (idleRunnables) {
			if (!idleRunnables.contains(r)){
				idleRunnables.add(r);
			}
		}
	}

	/**
	 * Calls 'onSurfaceCreated' on a node in the GLThread.
	 * @author Matthias Schicker
	 */
	public static class OnSurfaceCreatedJob implements IGlRunnable {
		private final Node node;
		public OnSurfaceCreatedJob(Node node) {
			this.node = node;
		}
		@Override
		public void run(RenderContext rc) {
			node.surfaceCreated(rc);
		}
	}
}
