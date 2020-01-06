package com.suchgame.stackz.game;

import java.nio.ShortBuffer;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.ZLevels;
import com.suchgame.stackz.gl.bound_meshes.BoundFreeColoredQuad;
import com.suchgame.stackz.gl.bound_meshes.Vbo;
import com.suchgame.stackz.gl.primitives.ColorQuad;
import com.suchgame.stackz.gl.primitives.ColoredVertex;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.util.RenderUtil;
import com.suchgame.stackz.ui.GameView;

public class TiledFloorNode extends Node {
	private BoundFreeColoredQuad[] tiles;
	
	// /////////////////////////////////////////////////////
	// plumbing, tmps, caches
	private float boxSize;
	private float tileSize;
	private ShortBuffer indexBuffer;
	
	
	public TiledFloorNode(){
        draws = true;
        transforms = false;
        handlesInteraction = false;

        renderProgramIndex = RenderProgramStore.SIMPLE_COLORED;
        depthTest = ACTIVATE;
        scissorTest = DONT_CARE;

        blending = DEACTIVATE;
        zLevel = ZLevels.GAME_BACKGROUND;
        
        useVboPainting = true;
        
        int tilesCount = Game2.BOARD_SIZE * Game2.BOARD_SIZE;		// ground level
        vbo = new Vbo(tilesCount * ColorQuad.VERTEX_COUNT, ColoredVertex.STRIDE_BYTES);
        indexBuffer = ColorQuad.allocateQuadIndices(tilesCount);
        
        tiles = new BoundFreeColoredQuad[tilesCount];
        for (int i = 0; i < tilesCount; i++){
        	tiles[i] = new BoundFreeColoredQuad();
        	tiles[i].bindToVbo(vbo);
        }
	}
	
	@Override
    public void onSurfaceChanged(RenderContext renderContext) {
        boxSize = GameView.getBoxSize(renderContext);
        tileSize = boxSize * renderContext.resources.getInteger(R.integer.background_tile_size_factor) / 100f;

        repositionTiles(renderContext);
        recolorTiles(renderContext);
    }

    private void recolorTiles(RenderContext renderContext) {
    	float[] groundColor = RenderUtil.color2floatsRGBA(null, 
    				renderContext.resources.getColor(R.color.cage_tiles_floor));
    	
		int tileIndex = 0;
		for (int i = 0; i < Game2.BOARD_SIZE*Game2.BOARD_SIZE; i++){
			tiles[tileIndex++].setColor(groundColor);
		}
	}

	private void repositionTiles(RenderContext renderContext) {
        float halfTileSize = tileSize / 2f;
        
        // reposition tiles
        int tileIndex = 0;
        float z = 0;
        
        // ground level
        float basicXY = -Game2.BOARD_SIZE/2 * boxSize;// + boxSize/2f;
        for (int y = 0; y < Game2.BOARD_SIZE; y++){
        	for (int x = 0; x < Game2.BOARD_SIZE; x++){
        		 BoundFreeColoredQuad nowTile = tiles[tileIndex];
        		 
        		 nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_LEFT, 
        				 basicXY + x*boxSize - halfTileSize, basicXY + y*boxSize + halfTileSize, z);
        		 nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_LEFT, 
        				 basicXY + x*boxSize - halfTileSize, basicXY + y*boxSize - halfTileSize, z);
        		 nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_LOWER_RIGHT, 
        				 basicXY + x*boxSize + halfTileSize, basicXY + y*boxSize - halfTileSize, z);
        		 nowTile.positionXYZ(BoundFreeColoredQuad.VERTEX_UPPER_RIGHT, 
        				 basicXY + x*boxSize + halfTileSize, basicXY + y*boxSize + halfTileSize, z);
        		 
        		 tileIndex++;
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
}
