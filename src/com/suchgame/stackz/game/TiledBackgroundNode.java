package com.suchgame.stackz.game;

import java.nio.ShortBuffer;
import java.util.ArrayList;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.BoundFreeColoredQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.math.Vector;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.primitives.ColoredVertex;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.util.RenderUtil;
import com.suchgame.stackz.model.BoxCoord;
import com.suchgame.stackz.ui.GameView;

public class TiledBackgroundNode extends Node {
	private static final int TILE_LEVELS = 11;

	public static final int NEXT_MOVE_SHAFT_HEIGHT = 4;
	public static final int NEXT_MOVE_SHAFT_START_Z = 4;
	
	private BoundFreeColoredQuad[] tiles;
	private BoxFace[] tilePositions;
	
	// /////////////////////////////////////////////////////
	// plumbing, tmps, caches
	private float boxSize;
	private float tileSize;
	private ShortBuffer indexBuffer;
	
	
	public TiledBackgroundNode(){
        draws = true;
        transforms = false;
        handlesInteraction = false;

        renderProgramIndex = RenderProgramStore.SIMPLE_COLORED;
        depthTest = ACTIVATE;
        scissorTest = DONT_CARE;

        blending = DEACTIVATE;
        zLevel = ZLevels.GAME_BACKGROUND;
        
        useVboPainting = true;
        
        defineTilePositions(); 
        
        vbo = new Vbo(tilePositions.length * ColorQuad.VERTEX_COUNT, ColoredVertex.STRIDE_BYTES);
        indexBuffer = ColorQuad.allocateQuadIndices(tilePositions.length);
        
        tiles = new BoundFreeColoredQuad[tilePositions.length];
        for (int i = 0; i < tiles.length; i++){
        	tiles[i] = new BoundFreeColoredQuad();
        	tiles[i].bindToVbo(vbo);
        }
	}

	private void defineTilePositions() {
		ArrayList<BoxFace> faces = new ArrayList<TiledBackgroundNode.BoxFace>();
		
		int nextMoveShaftMin = 1;
		int nextMoveShaftMax = 2;
		
		nextMoveShaftMin = Game2.BOARD_SIZE/2 - 1;
		nextMoveShaftMax = Game2.BOARD_SIZE/2 + 1;

		int rightPlateuZ = NEXT_MOVE_SHAFT_START_Z + 2;
		
		// simple: the shaft
		for (int z = 0; z < TILE_LEVELS; z++){
			for (int i = 0; i < Game2.BOARD_SIZE; i++){
				faces.add(new BoxFace(0, i, z, BoxFace.Face.LEFT));		// left
				faces.add(new BoxFace(Game2.BOARD_SIZE-1, i, z, BoxFace.Face.RIGHT));		// right
				
//				if (z < NEXT_MOVE_SHAFT_HEIGHT || i < 2 || i > 3){
					faces.add(new BoxFace(i, 0, z, BoxFace.Face.BOTTOM));		// bottom
//				}
				
				if (z < NEXT_MOVE_SHAFT_START_Z){
					faces.add(new BoxFace(i, Game2.BOARD_SIZE-1, z, BoxFace.Face.TOP));		// top
				}
				// right plateau
				if (i > nextMoveShaftMax && z >= NEXT_MOVE_SHAFT_START_Z && z < rightPlateuZ){
					faces.add(new BoxFace(i, Game2.BOARD_SIZE-1, z, BoxFace.Face.TOP));		// top
				}
			}
		}

		// the right plateu
		for (int x = nextMoveShaftMax+1 ; x < Game2.BOARD_SIZE; x++){
			for (int i = 0; i < NEXT_MOVE_SHAFT_HEIGHT; i++){
				faces.add(new BoxFace(x, Game2.BOARD_SIZE+i, rightPlateuZ, BoxFace.Face.BACK));
			}
		}
		
		// the central 'next move' position
		for (int i = 0; i < 2; i++){
			// the central ground area
			for (int x = nextMoveShaftMax-2; x <= nextMoveShaftMax; x++){
				faces.add(new BoxFace(x, Game2.BOARD_SIZE+i, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.BACK));
			}
			// the right wall
			for (int z = NEXT_MOVE_SHAFT_START_Z; z < rightPlateuZ; z++){
				faces.add(new BoxFace(nextMoveShaftMax, Game2.BOARD_SIZE+i, z, BoxFace.Face.RIGHT));
			}
		}
		// the back wall
		for (int z = NEXT_MOVE_SHAFT_START_Z; z < rightPlateuZ; z++){
			for (int x = nextMoveShaftMin; x <= nextMoveShaftMax; x++){
				faces.add(new BoxFace(x, Game2.BOARD_SIZE+1, z, BoxFace.Face.TOP));
			}
		}
		
		// the plateau behind the shaft
		for (int y = Game2.BOARD_SIZE+2; y < Game2.BOARD_SIZE + NEXT_MOVE_SHAFT_HEIGHT; y++){
			for (int x = nextMoveShaftMin; x <= nextMoveShaftMax; x++){
				faces.add(new BoxFace(x, y, rightPlateuZ, BoxFace.Face.BACK));
			}
		}
		
		// plateau for the first follow up move
		for (int i = 0; i < 2; i++){
			faces.add(new BoxFace(nextMoveShaftMin, Game2.BOARD_SIZE+i, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.LEFT));
		}
		for (int i = 0; i < NEXT_MOVE_SHAFT_HEIGHT; i++){
			faces.add(new BoxFace(nextMoveShaftMin-1, Game2.BOARD_SIZE+i, NEXT_MOVE_SHAFT_START_Z+1, BoxFace.Face.BACK));
		}
		// the face towards the main shaft
		faces.add(new BoxFace(nextMoveShaftMin-1, Game2.BOARD_SIZE-1, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.TOP));		// top
		
		// the uttermost left part
		for (int x = 0; x < nextMoveShaftMin-1; x++){
			// plateau
			for (int i = 0; i < NEXT_MOVE_SHAFT_HEIGHT; i++){
				faces.add(new BoxFace(x, Game2.BOARD_SIZE+i, NEXT_MOVE_SHAFT_START_Z+2, BoxFace.Face.BACK));
			}
			// front
			for (int z = NEXT_MOVE_SHAFT_START_Z; z < NEXT_MOVE_SHAFT_START_Z+2; z++){
				faces.add(new BoxFace(x, Game2.BOARD_SIZE-1, z, BoxFace.Face.TOP));
			}
		}
		// right wall towards incoming pipe
		for (int i = 0; i < NEXT_MOVE_SHAFT_HEIGHT; i++){
			faces.add(new BoxFace(nextMoveShaftMin-1, Game2.BOARD_SIZE+i, NEXT_MOVE_SHAFT_START_Z+1, BoxFace.Face.LEFT));
		}
		
		
		// now, the plateau for the stored move
//		faces.add(new BoxFace(2, -1, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.BACK));
//		faces.add(new BoxFace(2, -2, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.BACK));
//		faces.add(new BoxFace(3, -1, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.BACK));
//		faces.add(new BoxFace(3, -2, NEXT_MOVE_SHAFT_START_Z, BoxFace.Face.BACK));
//		// the walls of the next move shaft
//		for (int z = NEXT_MOVE_SHAFT_START_Z; z < TILE_LEVELS; z++){
//			for (int i = 0; i < 2; i++){
//				faces.add(new BoxFace(2, -1-i, z, BoxFace.Face.LEFT));
//				faces.add(new BoxFace(3, -1-i, z, BoxFace.Face.RIGHT));
//				faces.add(new BoxFace(2 + i, -2, z, BoxFace.Face.BOTTOM));
//			}
//		}
		
		tilePositions = new BoxFace[faces.size()];
		faces.toArray(tilePositions);
	}

	@Override
    public void onSurfaceChanged(RenderContext renderContext) {
        boxSize = GameView.getBoxSize(renderContext);
        tileSize = boxSize * renderContext.resources.getInteger(R.integer.background_tile_size_factor) / 100f;

        repositionTiles(renderContext);
        recolorTiles(renderContext);
    }

    private void recolorTiles(RenderContext renderContext) {
    	float[] footColorRGBA = RenderUtil.color2floatsRGBA(null, 
    			renderContext.resources.getColor(R.color.cage_tiles_floor));
    	float[] topColorRGBA = RenderUtil.color2floatsRGBA(null, 
    			renderContext.resources.getColor(R.color.background));
    	
    	float[] colorStep = new float[]{0,0,0,1};
    	Vector.aMinusB3(colorStep, topColorRGBA, footColorRGBA);
    	Vector.scalarMultiply3(colorStep, 1f/TILE_LEVELS);
    	
    	float[] nowColor = new float[4];
    	
		for (int i = 0; i < tilePositions.length; i++){
			Vector.set4(nowColor, colorStep);
			Vector.scalarMultiply3(nowColor, tilePositions[i].z);
			Vector.addBtoA3(nowColor, footColorRGBA);
			
			tiles[i].setColor(nowColor);
		}
	}

	private void repositionTiles(RenderContext renderContext) {
        float halfTileSize = tileSize / 2f;
        float basicXY = -Game2.BOARD_SIZE/2 * boxSize;// + boxSize/2f;
        
        float basicZ = boxSize/2f;
        
        for (int i = 0; i < tilePositions.length; i++){
        	BoundFreeColoredQuad nowTile = tiles[i];
        	BoxFace pos = tilePositions[i];
        	
        	float x = 0;
        	float y = 0;
        	float z = 0;
        	
        	switch (pos.face){
			case TOP:
				y = basicXY + pos.y*boxSize + 0.5f*boxSize;
				nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_LEFT, 
        				basicXY + pos.x*boxSize - halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize + halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_LEFT, 
        				basicXY + pos.x*boxSize - halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_RIGHT, 
        				basicXY + pos.x*boxSize + halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_RIGHT, 
        				basicXY + pos.x*boxSize + halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize + halfTileSize);
				break;
			case BOTTOM:
				y = basicXY + pos.y*boxSize - 0.5f*boxSize;
				nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_RIGHT, 
        				basicXY + pos.x*boxSize - halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize + halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_RIGHT, 
        				basicXY + pos.x*boxSize - halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_LEFT, 
        				basicXY + pos.x*boxSize + halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_LEFT, 
        				basicXY + pos.x*boxSize + halfTileSize, 
        				y,
        				basicZ + pos.z*boxSize + halfTileSize);
				break;
			case FRONT:
				// unnecessary for now
				break;
			case LEFT:
				x = basicXY + pos.x * boxSize - boxSize/2f;
				nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_LEFT, 
        				x, 
        				basicXY + pos.y*boxSize - halfTileSize,
        				basicZ + pos.z*boxSize + halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_LEFT, 
        				x, 
        				basicXY + pos.y*boxSize - halfTileSize,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_RIGHT, 
        				x, 
        				basicXY + pos.y*boxSize + halfTileSize,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_RIGHT, 
        				x, 
        				basicXY + pos.y*boxSize + halfTileSize,
        				basicZ + pos.z*boxSize + halfTileSize);
				break;
			case RIGHT:
				x = basicXY + pos.x * boxSize + boxSize/2f;
				nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_RIGHT, 
        				x, 
        				basicXY + pos.y*boxSize - halfTileSize,
        				basicZ + pos.z*boxSize + halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_RIGHT, 
        				x, 
        				basicXY + pos.y*boxSize - halfTileSize,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_LEFT, 
        				x, 
        				basicXY + pos.y*boxSize + halfTileSize,
        				basicZ + pos.z*boxSize - halfTileSize);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_LEFT, 
        				x, 
        				basicXY + pos.y*boxSize + halfTileSize,
        				basicZ + pos.z*boxSize + halfTileSize);
				break;
			case BACK:
				z = basicZ + pos.z * boxSize - boxSize/2f;
				nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_LEFT, 
						basicXY + pos.x*boxSize - halfTileSize, 
						basicXY + pos.y*boxSize + halfTileSize,
        				z);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_LEFT, 
        				basicXY + pos.x*boxSize - halfTileSize, 
						basicXY + pos.y*boxSize - halfTileSize,
        				z);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_RIGHT, 
        				basicXY + pos.x*boxSize + halfTileSize, 
						basicXY + pos.y*boxSize - halfTileSize,
        				z);
        		nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_RIGHT, 
        				basicXY + pos.x*boxSize + halfTileSize, 
						basicXY + pos.y*boxSize + halfTileSize,
        				z);
				break;
			default:
				break;
        	}
        }
	}

	@Override
    public boolean onRender(RenderContext renderContext) {
    	indexBuffer.position(0);
    	int indexCount = 0;
    	for (BoundFreeColoredQuad t : tiles){
    		indexCount += t.render(renderContext, indexBuffer);
    	}
    	
    	renderContext.renderColoredTriangles(0, indexCount, indexBuffer);
    		
        return true;
    }

	private static class BoxFace extends BoxCoord {
		public static enum Face { LEFT, RIGHT, TOP, BOTTOM, FRONT, BACK  };
		public Face face = Face.BACK;

		public BoxFace(int x, int y, int z, BoxFace.Face face) {
			super(x,y,z);
			this.face = face;
		}
	}
}
