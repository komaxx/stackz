package com.suchgame.stackz.move;

import java.nio.ShortBuffer;
import java.util.HashSet;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.TexturedBox;
import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Core.GameState;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.core.listener.IGameStateListener;
import com.suchgame.stackz.core.listener.IMovesListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.Vertex;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.scenegraph.interaction.Pointer;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.model.BoxCoord;
import com.suchgame.stackz.model.Move;
import com.suchgame.stackz.model.PatternCoord;
import com.suchgame.stackz.ui.GameView;

/**
 * Handles the interaction need to let the user paint a move directly
 * on the stacks.
 * 
 * @author Matthias Schicker
 */
public class MovePaintNode extends Node implements IGameStateListener, IMovesListener {
	private static final float MIN_ABORT_DISTANCE_BOXES = 2;

	private MovePaintVibrator vibrator;
	
	private TexturedBox[] boxes = new TexturedBox[Game2.BOARD_SIZE*Game2.BOARD_SIZE];
	
	// //////////////////////////////////////////
	// interaction tmps
	private int boundPointerIndex = -1;
	private float minTouchDistanceSquared = -1;
	private float minAbortDistanceSquared = -1;
	
	private float[] tmpRayVector = new float[]{0,0,0,1};
	
	private HashSet<BoxCoord> touchedMoveCoords = new HashSet<BoxCoord>();
	private int nowMoveStartZ = -1;
	private HashSet<BoxCoord> possibleMoveCoords = new HashSet<BoxCoord>();

	private Move tmpMove = new Move();
	private BoxCoord tmpBoxCoord = new BoxCoord();
	
	private int textureHandleComplete = -1;
	private int textureHandleInProgress = -1;
	private int textureHandleAbort = -1;
	
	// /////////////////////////////////////////////////////
	// plumbing, tmps, caches
	private Move currentMove;
	
	private float defaultBoxSize;
	private float baseXY;
	private ShortBuffer indexBuffer;
	
	private enum PaintState { IN_PROGRESS, COMPLETE, ABORT };
	private PaintState paintState = PaintState.IN_PROGRESS;
	
	
	public MovePaintNode(MovePaintVibrator vibrator){
		this.vibrator = vibrator;
		
		transforms = false;
		draws = true;
		handlesInteraction = true;
		
        renderProgramIndex = RenderProgramStore.ALPHA_TEXTURED;
        depthTest = ACTIVATE;
        scissorTest = DONT_CARE;

        blending = ACTIVATE;
		zLevel = ZLevels.MOVE;
		
		useVboPainting = true;
		vbo = new Vbo(boxes.length * TexturedBox.MAX_VERTEX_COUNT, Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
        indexBuffer = TexturedBox.allocateIndices(boxes.length);
        
        for (int i = 0; i < boxes.length; i++){
        	TexturedBox box = new TexturedBox();
        	box.setVisible(false);
        	box.bindToVbo(vbo);
        	boxes[i] = box;
        }
        
        Core.get().registry().addMovesListener(this);
        Core.get().registry().addGameStateListener(this);
	}
	
	private void positionBoxesForMoveStart() {
		float baseZ = defaultBoxSize/2;

		Game2 g = Core.get().game();
		
		int boxIndex = 0;
		for (int y = 0; y < Game2.BOARD_SIZE; y++){
			for (int x = 0; x < Game2.BOARD_SIZE; x++){
				int z = g.getLevel(x, y) + 1;
				
				boxes[boxIndex].position(
						baseXY + x*defaultBoxSize, 
						baseXY + y*defaultBoxSize, 
						baseZ +  z*defaultBoxSize);
				boxes[boxIndex].setSize(defaultBoxSize);
				boxes[boxIndex].shortcutAnimation();
				boxes[boxIndex].getCoord().set(x, y, z);
				boxIndex++;
			}	
		}
	}
	
	private void positionBoxesForMove(int z){
		float baseZ = defaultBoxSize/2;

		int boxIndex = 0;
		for (int y = 0; y < Game2.BOARD_SIZE; y++){
			for (int x = 0; x < Game2.BOARD_SIZE; x++){
				boxes[boxIndex].position(
						baseXY + x*defaultBoxSize, 
						baseXY + y*defaultBoxSize, 
						baseZ +  z*defaultBoxSize);
				boxes[boxIndex].setSize(defaultBoxSize);
				boxes[boxIndex].shortcutAnimation();
				boxes[boxIndex].getCoord().set(x, y, z);
				boxIndex++;
			}	
		}
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		textureHandleComplete = renderContext.textureStore.getResourceTexture(
				renderContext, R.raw.move_active, false).getHandle();
		textureHandleAbort = renderContext.textureStore.getResourceTexture(
				renderContext, R.raw.move_inactive, false).getHandle();
		textureHandleInProgress = renderContext.textureStore.getResourceTexture(
				renderContext, R.raw.move_in_progress, false).getHandle();
		
		textureHandle = textureHandleComplete;
		
		super.onSurfaceCreated(renderContext);
	}
	
	@Override
    public void onSurfaceChanged(RenderContext renderContext) {
        defaultBoxSize = GameView.getBoxSize(renderContext);
        baseXY = GameView.getBoxLowerLeftXY(renderContext);
        float halfSize = defaultBoxSize / 2f;
        minTouchDistanceSquared = halfSize*halfSize + halfSize*halfSize;
        float abortRadius = defaultBoxSize * MIN_ABORT_DISTANCE_BOXES;
        minAbortDistanceSquared = abortRadius*abortRadius + abortRadius*abortRadius;
        
        positionBoxesForMoveStart();
    }

	@Override
    public boolean onRender(RenderContext renderContext) {
    	indexBuffer.position(0);
    	int indexCount = 0;
    	for (TexturedBox box : boxes){
    		indexCount += box.render(renderContext, indexBuffer);
    	}
    	
    	renderContext.renderTexturedTriangles(0, indexCount, indexBuffer);
    	
        return true;
    }
	
	@Override
	protected boolean onInteraction(InteractionContext ic) {
		if (currentMove == null){
			return false;
		}
		
		if (boundPointerIndex == -1 && ic.getAction() != Pointer.DOWN){
			return false;
		}
		
		if (boundPointerIndex == -1){		// obviously: action is DOWN
			positionBoxesForMoveStart();
			possibleMoveCoords.clear();
			for (int i = 0; i < boxes.length; i++) possibleMoveCoords.add(boxes[i].getCoord());
			
			// ray-vector, from 0;0;0 and normalized
			Vector.aMinusB3(tmpRayVector, ic.getPointers()[0].getRayPoint(), ic.eyePointStack.peek());
			int touchedIndex = getTouchedBoxIndex(tmpRayVector, ic.eyePointStack.peek());
			if (touchedIndex < 0) return false;
			
			nowMoveStartZ = boxes[touchedIndex].getCoord().z;
			positionBoxesForMove(nowMoveStartZ);
			boundPointerIndex = ic.getActionIndex();

			touchedMoveCoords.add(boxes[touchedIndex].getCoord());
			showPossibleMoves();
			
			if (possibleMoveCoords.size()+1 < currentMove.getBasicCoords().length){
				KoLog.i(this, "No possible move from here. Aborted.");
				// there's no move possible with this start box! Abort.
				setPaintState(PaintState.ABORT);
				touchedMoveCoords.clear();
				possibleMoveCoords.clear();
				updateBoxes();
				boundPointerIndex = -1;
				return false;
			} 

			setPaintState(PaintState.IN_PROGRESS);
			vibrateStart();
			checkIfDefined();
			return true;
		}
		
		// from here: we have already a bound index
		Pointer pointer = ic.getPointers()[boundPointerIndex];
		int action = pointer.getAction();
		if (action == Pointer.MOVE){
			Vector.aMinusB3(tmpRayVector, ic.getPointers()[boundPointerIndex].getRayPoint(), ic.eyePointStack.peek());
			int touchedIndex = getTouchedBoxIndex(tmpRayVector, ic.eyePointStack.peek());
			
			if (touchedIndex >= 0 && !touchedMoveCoords.contains(boxes[touchedIndex].getCoord())){
				touchedMoveCoords.add(boxes[touchedIndex].getCoord());
				vibrateTick();
				showPossibleMoves();
				checkIfDefined();
			} else {	// no new coord touched. Moved to abort?
				if (getClosestBoxDistanceSquared(tmpRayVector, ic.eyePointStack.peek()) > minAbortDistanceSquared){
					setPaintState(PaintState.ABORT);
					vibrateAbort();
					updateBoxes();
				} else if (paintState == PaintState.ABORT){
					setPaintState(PaintState.IN_PROGRESS);
					checkIfDefined();
				}
			}
		} else if (action == Pointer.UP){
			if (paintState != PaintState.COMPLETE){
				KoLog.i(this, "In Abort state. Aborted.");
				touchedMoveCoords.clear();
				possibleMoveCoords.clear();
			} else if (validMoveTouched()){
				KoLog.i(this, "Move triggered.");
				triggerCurrentMove();
				Core.get().game().executeCurrentMove(ic);
			} else {
				KoLog.e(this, "?????");
			}
			
			positionBoxesForMoveStart();
			abort(ic);
			return true;
		} else {		// some other freaky action
			abort(ic);
		}
		return false;
	}
	
	private void checkIfDefined() {
		if (touchedMoveCoords.size() + possibleMoveCoords.size() == currentMove.getBasicCoords().length){
			KoLog.i(this, "Move is defined!");
			
			// this move is well defined :)
			for (BoxCoord b : possibleMoveCoords){
				touchedMoveCoords.add(b);
			}
			setPaintState(PaintState.COMPLETE);
			vibrateMoveDefined();
			possibleMoveCoords.clear();
			updateBoxes();
		}
	}


	private void setPaintState(PaintState nuState) {
		if (nuState == paintState) return;
		
		switch (nuState){
		case ABORT:
			textureHandle = textureHandleAbort;
			break;
		case COMPLETE:
			textureHandle = textureHandleComplete;
			break;
		case IN_PROGRESS:
		default:
			textureHandle = textureHandleInProgress;
			break;
		}
		paintState = nuState;
	}

	private float getClosestBoxDistanceSquared(float[] rayVector, float[] eyePoint) {
		float closestDistanceSquared = 1000000f;
		for (int i = 0; i < boxes.length; i++){
			if (!touchedMoveCoords.contains(boxes[i].getCoord())) continue;
			
			// vector to test-point
			boxes[i].getCenter(tmpTestPointVector);
			Vector.aMinusB3(tmpTestPointVector, eyePoint);
			
			float dotProduct = Vector.dotProduct3(rayVector, tmpTestPointVector);
			float distanceSquared = Vector.sqrLength3(tmpTestPointVector) - dotProduct*dotProduct;
			
			if (distanceSquared < closestDistanceSquared){
				closestDistanceSquared = distanceSquared;
			}
		}
		return closestDistanceSquared;
	}

	private void vibrateTick() {
		vibrator.tick();
	}

	private void vibrateStart() {
		vibrator.moveStart();
	}

	private void vibrateAbort() {
		vibrator.abort();
	}
	
	private void vibrateMoveDefined() {
		vibrator.moveDefined();
	}

	private void triggerCurrentMove() {
		HashSet<BoxCoord> moveCoords = new HashSet<BoxCoord>();
		
		moveCoords.addAll(touchedMoveCoords);
		moveCoords.addAll(possibleMoveCoords);
		
		currentMove.moveToEqual(moveCoords);
	}

	private boolean validMoveTouched() {
		if (currentMove.sameAs(touchedMoveCoords)) return true;
		// not the full defined. But was it uniquely defined?
		return (possibleMoveCoords.size() + touchedMoveCoords.size() 
				== currentMove.getBasicCoords().length);
	}

	private float[] tmpTestPointVector = new float[]{0,0,0,1};
	private int getTouchedBoxIndex(float[] rayVector, float[] eyePoint) {
		Vector.normalize3(rayVector);

		int closestIndex = -1;
		float closestDistanceSquared = 1000000f;
		
		for (int i = 0; i < boxes.length; i++){
			if (!possibleMoveCoords.contains(boxes[i].getCoord())) continue;
			
			// vector to test-point
			boxes[i].getCenter(tmpTestPointVector);
			Vector.aMinusB3(tmpTestPointVector, eyePoint);
			
			float dotProduct = Vector.dotProduct3(rayVector, tmpTestPointVector);
			float distanceSquared = Vector.sqrLength3(tmpTestPointVector) - dotProduct*dotProduct;
			
			if (distanceSquared < closestDistanceSquared){
				closestDistanceSquared = distanceSquared;
				closestIndex = i;
			}
		}
		
		if (closestDistanceSquared > minTouchDistanceSquared){
			return -1;
		}
		return closestIndex;
	}

	private void showPossibleMoves() {
		computePossibleMoves();
		updateBoxes();
	}

	private void updateBoxes() {
		for (TexturedBox b : boxes){
			if (touchedMoveCoords.contains(b.getCoord())) {
				// show full
				b.setVisible(true);
				b.setSize(defaultBoxSize);
			} else if (possibleMoveCoords.contains(b.getCoord())){
				// show half
				b.setVisible(true);
				b.setSize(defaultBoxSize * 0.3f);
			} else {
				b.setVisible(false);
			}
		}
	}

	private void computePossibleMoves() {
		possibleMoveCoords.clear();
		if (currentMove == null) return;
		
		tmpMove.set(currentMove);
		tmpBoxCoord.z = nowMoveStartZ;
		
		Game2 game = Core.get().game();
		
		for (int orientation = 0; orientation < 9; orientation++){		// 2 * 4 orientations + mirroring
			for (int y = 0; y <= Game2.BOARD_SIZE-tmpMove.getSizeY(); y++){
				for (int x = 0; x <= Game2.BOARD_SIZE-tmpMove.getSizeX(); x++){
					tmpMove.setPosition(x, y);
					PatternCoord[] coords = tmpMove.getCoords();
					
					boolean valid = true;
					int foundTouchedCoords = 0;
					for (PatternCoord pc : coords){
						if (game.getLevel(pc.x, pc.y)+1 > nowMoveStartZ){
							valid = false;
							break;
						}
						
						tmpBoxCoord.x = pc.x;
						tmpBoxCoord.y = pc.y;
						if (touchedMoveCoords.contains(tmpBoxCoord)) foundTouchedCoords++;
					}
					if (!valid || foundTouchedCoords != touchedMoveCoords.size()) continue;
					
					// this is a valid move!
					for (PatternCoord pc : coords){
						tmpBoxCoord.set(pc.x, pc.y, nowMoveStartZ);
						if (!touchedMoveCoords.contains(tmpBoxCoord)) possibleMoveCoords.add(new BoxCoord(tmpBoxCoord));
					}
				}
			}
			if (orientation == 4) tmpMove.mirror();
			else tmpMove.turnCV();
		}
	}

	public void abort(InteractionContext ic) {
//		if (boundPointerIndex != -1){
//			// canceled
//		}
		touchedMoveCoords.clear();
		possibleMoveCoords.clear();
		nowMoveStartZ = -1;
		boundPointerIndex = -1;
		updateBoxes();
	}

	@Override
	public void handleMovesChanged(SceneGraphContext sc) {
		currentMove = Core.get().game().getCurrentMove();
	}

	@Override
	public void handleGameStateChanged(SceneGraphContext sc, GameState oldState, GameState nuState) {
		this.interactive = Core.get().getGameState() == GameState.RUNNING;
		if (!interactive){
			touchedMoveCoords.clear();
			possibleMoveCoords.clear();
			nowMoveStartZ = -1;
			boundPointerIndex = -1;
			updateBoxes();
		}
	}
}