package com.suchgame.stackz.move;
//package com.suchgame.stackz.game;
//
//import java.nio.FloatBuffer;
//import java.nio.ShortBuffer;
//import java.util.ArrayDeque;
//import java.util.ArrayList;
//
//import com.suchgame.stackz.RenderProgramStore;
//import com.suchgame.stackz.gl.RenderContext;
//import com.suchgame.stackz.gl.ZLevels;
//import com.suchgame.stackz.gl.bound_meshes.ABoundMesh;
//import com.suchgame.stackz.gl.bound_meshes.AnimatedBoundTexturedQuad;
//import com.suchgame.stackz.gl.bound_meshes.Vbo;
//import com.suchgame.stackz.gl.primitives.TexturedQuad;
//import com.suchgame.stackz.gl.primitives.Vertex;
//import com.suchgame.stackz.gl.scenegraph.ADelayedGlRunnable;
//import com.suchgame.stackz.gl.scenegraph.Node;
//import com.suchgame.stackz.gl.util.KoLog;
//import com.suchgame.stackz.gl.util.RenderUtil;
//import com.suchgame.stackz.model.BoxCoord;
//import com.suchgame.stackz.model.Game;
//import com.suchgame.stackz.model.Game.GameState;
//import com.suchgame.stackz.model.Game.IGameListener;
//import com.suchgame.stackz.model.Square;
//
//public class SquaresScoredNode extends Node implements IGameListener {
//	/**
//	 * Max buffer size. 
//	 */
//	private static final int MAX_SCORE_POINTS = 250;
//	/**
//	 * Time between visualizations of scoring squares.
//	 */
//	private static final int SQUARE_DELAY_MS = 20;
//
//	/**
//	 * Time it takes for a scorePoint to appear
//	 */
//	private static final long SCORE_POINT_APPEAR_TIME_NS = RenderUtil.nsFromMs(100);
//	/**
//	 * How long score points take to tush to the score
//	 */
//	private static final long SCORE_POINT_RUSH_TO_SCORE_SPEED_NS = RenderUtil.nsFromMs(600);
//
//	
//	private Game game;
//	
//	private boolean active = false;
//	
//	/**
//	 * Currently inactive points visualization.
//	 */
//	private ArrayDeque<ScorePoint> scorePointsPool = new ArrayDeque<ScorePoint>();
//	
//	/**
//	 * Active score point visualizations.
//	 */
//	private ArrayDeque<ScorePoint> scorePoints = new ArrayDeque<ScorePoint>();
//	
//	// //////////////////////////////////////////////////////////////////////
//	// plumbing, tmps
//	private ShortBuffer indexBuffer;
//	private ArrayList<ScorePoint> tmpToRemove = new ArrayList<ScorePoint>();
//	private float tmpBoxSize;
//	private float tmpBaseXY;
//	
//	
//	public SquaresScoredNode(){
//		draws = true;
//		handlesInteraction = false;
//		transforms = false;
//		
//		renderProgramIndex = RenderProgramStore.DEPPEN_SHADER;
//		blending = ACTIVATE;
//		depthTest = ACTIVATE;
//		
//		zLevel = ZLevels.SCORE_POINTS;
//		
//		vbo = new Vbo(2 * TexturedQuad.VERTEX_COUNT * MAX_SCORE_POINTS, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES);
//		indexBuffer = allocateScorePoints(MAX_SCORE_POINTS);
//		
//		for (int i = 0; i < MAX_SCORE_POINTS; i++){
//			ScorePoint nuScorePoint = new ScorePoint();
//			nuScorePoint.bindToVbo(vbo);
//			scorePointsPool.add(nuScorePoint);
//		}
//	}
//
//	private static ShortBuffer allocateScorePoints(int count) {
//		return TexturedQuad.allocateQuadIndices(2 * count);
//	}
//
//	public void setGame(Game game){
//		this.game = game;
//		this.game.addListener(this);
//	}
//
//	@Override
//	protected void onSurfaceCreated(RenderContext renderContext) {
//		
//		// TODO   do the texture thing
//		
//	}
//	
//	@Override
//	public void onSurfaceChanged(RenderContext renderContext) {
//		tmpBoxSize = OldGameView.getBoxSize(renderContext);
//		tmpBaseXY = OldGameView.getBoxLowerLeftXY(renderContext);
//	}
//	
//	@Override
//	public boolean onRender(RenderContext renderContext) {
//		int indexCount = 0;
//		indexBuffer.position(0);
//		tmpToRemove.clear();
//		
//		for (ScorePoint sp : scorePoints){
//			indexCount += sp.render(renderContext, indexBuffer);
//			if (sp.isDone(renderContext)) tmpToRemove.add(sp);
//		}
//		
//		Vertex.renderTexturedTriangles(
//				renderContext.currentRenderProgram, 0, indexCount, indexBuffer);
//
//		scorePoints.removeAll(tmpToRemove);
//		scorePointsPool.addAll(tmpToRemove);
//		
//		return true;
//	}
//	
//	@Override
//	public void scored(Square[] nuSquares, BoxCoord[] scoredCoords) {
//		if (active){
//			for (int i = 0; i < nuSquares.length; i++){
//				queueInGlThread(new SquareAddJob(nuSquares[i], i));
//			}
//		}
//	}
//
//	@Override
//	public void currentMoveChanged() { /* nothing */ }
//	@Override
//	public void gameStateChanged() {
//		active = game.getGameState()==GameState.RUNNING;
//	}
//	@Override
//	public void levelsChanged() { /* nothing */ }
//	
//	
//	private class SquareAddJob extends ADelayedGlRunnable {
//		private Square square;
//
//		public SquareAddJob(Square square, int squareIndex) {
//			super(SquaresScoredNode.this, squareIndex * SQUARE_DELAY_MS);
//			this.square = square;
//		}
//
//		@Override
//		protected void doRun(RenderContext rc) {
//			// horizontal points
//			for (int i = 0; i < square.size; i++){
//				addScorePoint(rc, square.llX + i, square.llY, square.z);
//				addScorePoint(rc, square.llX + i, square.llY + square.size-1, square.z);
//			}
//			
//			// vertical points
//			for (int i = 1; i < square.size-1; i++){
//				addScorePoint(rc, square.llX, square.llY + i, square.z);
//				addScorePoint(rc, square.llX + square.size-1, square.llY + i, square.z);
//			}
//		}
//	}
//	
//	public void addScorePoint(RenderContext rc, int x, int y, int z) {
//		if (scorePointsPool.isEmpty()){
//			KoLog.w(this, "OUT OF SCORE POINTS!");
//			return;
//		}
//		
//		ScorePoint nowScorePoint = scorePointsPool.pop();
//		scorePoints.add(nowScorePoint);
//		
//		nowScorePoint.appearFor(rc, x, y, z);
//	}
//
//	
//	private class ScorePoint extends ABoundMesh {
//		private AnimatedBoundTexturedQuad lowerQuad = new AnimatedBoundTexturedQuad();
//		private AnimatedBoundTexturedQuad upperQuad = new AnimatedBoundTexturedQuad();
//		
//		private long rushToScoreTime;
//		private long doneTime;
//		
//		
//		public ScorePoint(){
//			lowerQuad.setAlphaAnimationDuration(RenderUtil.nsFromMs(60));
//			upperQuad.setAlphaAnimationDuration(RenderUtil.nsFromMs(60));
//		}
//		
//		@Override
//		public void bindToVbo(Vbo vbo) {
//			lowerQuad.bindToVbo(vbo);
//			upperQuad.bindToVbo(vbo);
//		}
//		
//		public void appearFor(RenderContext rc, int x, int y, int z) {
//			rushToScoreTime = rc.frameNanoTime + SCORE_POINT_APPEAR_TIME_NS;
//			doneTime = rc.frameNanoTime + SCORE_POINT_APPEAR_TIME_NS + SCORE_POINT_RUSH_TO_SCORE_SPEED_NS;
//			
//			float appearCenterX = tmpBaseXY + x*tmpBoxSize;
//			float appearCenterY = tmpBaseXY + y*tmpBoxSize;
//			
//			// position for the start
//			float appearStartZ = (z+0.6f)*tmpBoxSize;
//			lowerQuad.position(appearCenterX, appearCenterY, appearStartZ, appearCenterX, appearCenterY, appearStartZ);
//			lowerQuad.shortcutPosition();
//			upperQuad.position(appearCenterX, appearCenterY, appearStartZ, appearCenterX, appearCenterY, appearStartZ);
//			upperQuad.shortcutPosition();
//
//			lowerQuad.setAlphaDirect(0);
//			upperQuad.setAlphaDirect(0);
//			
//			float halfSize = tmpBoxSize / 4f;
//			// set appear position
//			float appearZ = (z+1.2f)*tmpBoxSize;
//			lowerQuad.setPositionAnimationDuration(SCORE_POINT_APPEAR_TIME_NS);
//			lowerQuad.position( appearCenterX - halfSize, appearCenterY + halfSize, appearZ, 
//								appearCenterX + halfSize, appearCenterY - halfSize, appearZ);
//			
//			appearZ = (z+1.4f)*tmpBoxSize;
//			upperQuad.setPositionAnimationDuration(SCORE_POINT_APPEAR_TIME_NS);
//			upperQuad.position( appearCenterX - halfSize, appearCenterY + halfSize, appearZ, 
//								appearCenterX + halfSize, appearCenterY - halfSize, appearZ);
//
//			lowerQuad.setAlpha(1);
//			upperQuad.setAlpha(1);
//		}
//
//		public boolean isDone(RenderContext rc) {
//			return rc.frameNanoTime > doneTime;
//		}
//
//		@Override
//		public int render(RenderContext rc, ShortBuffer frameIndexBuffer) {
//			if (rc.frameNanoTime > rushToScoreTime && lowerQuad.getTargetAlpha() > 0){
//				lowerQuad.setAlphaAnimationDuration(RenderUtil.sFromNs(SCORE_POINT_RUSH_TO_SCORE_SPEED_NS));
//				upperQuad.setAlphaAnimationDuration(RenderUtil.sFromNs(SCORE_POINT_RUSH_TO_SCORE_SPEED_NS));
//				
//				lowerQuad.setAlpha(0);
//				upperQuad.setAlpha(0);
//			}
//			
//			return lowerQuad.render(rc, frameIndexBuffer) + upperQuad.render(rc, frameIndexBuffer);
//		}
//		
//		@Override
//		public int render(RenderContext rc, FloatBuffer vertexBuffer, ShortBuffer frameIndexBuffer) {
//			return 
//					lowerQuad.render(rc, vertexBuffer, frameIndexBuffer) + 
//					upperQuad.render(rc, vertexBuffer, frameIndexBuffer);
//		}
//
//		@Override
//		public int getMaxVertexCount() {
//			return 2 * TexturedQuad.VERTEX_COUNT;
//		}
//
//		@Override
//		public int getMaxIndexCount() {
//			return 2 * TexturedQuad.INDICES_COUNT;
//		}
//	}
//}
