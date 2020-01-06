package com.suchgame.stackz.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.suchgame.stackz.R;
import com.suchgame.stackz.RenderProgramStore;
import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Game2;
import com.suchgame.stackz.game.AcceleRotateNode;
import com.suchgame.stackz.game.TiledBackgroundNode;
import com.suchgame.stackz.game.TiledFloorNode;
import com.suchgame.stackz.gl.BasicSceneGraphRenderView;
import com.suchgame.stackz.gl.GlConfig;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.StaticCameraNode;
import com.suchgame.stackz.gl.scenegraph.basic_nodes.TranslationNode;
import com.suchgame.stackz.move.MoveNode;
import com.suchgame.stackz.ui.menu.MenuNode;

/**
 * The master view class. Creates the scene graph tree.
 *
 * Created by Matthias Schicker on 12/17/13.
 */
public class GameView extends BasicSceneGraphRenderView {
	public static final float CAM_DISTANCE_FACTOR = -.9f;
	public static final float CAM_FOV_ANGLE = 16; //22;
	
	
	private MenuNode menuNode;
	
    private LevelTranslationNode levelTranslationNode = new LevelTranslationNode();
    private BoxesNode boxesNode;
    private MoveNode moveNode;
    
    
    public GameView(Context context) {
    	this(context, null);
    }
    
    public GameView(Context context, AttributeSet set) {
        super(context, set, createGlConfig(), new RenderProgramStore());
        if (isInEditMode() || getResources() == null) return;

        this.sceneGraph.setBackgroundColor(getResources().getColor(R.color.background));
        this.sceneGraph.toggleColorBufferCleaning(true);
        this.sceneGraph.toggleDepthBufferCleaning(true);
        
        this.buildUpScene(context);
    }
    
	@Override
    public void onResume() {
    	sceneGraph.getRoot().queueOnceInGlThread(ticker);
    	super.onResume();
    	menuNode.onResume();
    }
    private IGlRunnable ticker = new IGlRunnable() {
		@Override
		public void run(RenderContext rc) {
			Core.get().runner().tick(rc);
			sceneGraph.getRoot().queueOnceInGlThread(ticker);
		}
	};
    
	@Override
	public void onPause() {
		menuNode.onPause();
		super.onPause();
	}

    private void buildUpScene(Context c) {
    	// ////////////////////////////////////////////////////////
    	// menu
    	StaticCameraNode menuCamNode = new StaticCameraNode();
    	sceneGraph.addNode(null, menuCamNode);
    	
    	menuNode = new MenuNode();
    	sceneGraph.addNode(menuCamNode, menuNode);
    	menuNode.setup(sceneGraph, c);
    	
    	
        // ////////////////////////////////////////////////////////
    	// game
    	StaticCameraNode gameCamNode = new StaticCameraNode();
        gameCamNode.setFovAngle(CAM_FOV_ANGLE);
    	
    	sceneGraph.addNode(null, gameCamNode);

        TranslationNode camTranslationNode = new TranslationNode(){
        	@Override
        	public void onSurfaceChanged(RenderContext rc) {  this.setTranslation(0, 0, CAM_DISTANCE_FACTOR*rc.surfaceWidth);   }
        };
        sceneGraph.addNode(gameCamNode, camTranslationNode);

        AcceleRotateNode acceleRotateNode = new AcceleRotateNode(c);
        sceneGraph.addNode(camTranslationNode, acceleRotateNode);
        
        TranslationNode gameCenterTranslationNode = new TranslationNode();
        gameCenterTranslationNode.setTranslation(
        		c.getResources().getDimension(R.dimen.game_center_x_offset), 
        		c.getResources().getDimension(R.dimen.game_center_y_offset), 0);
        sceneGraph.addNode(acceleRotateNode, gameCenterTranslationNode);
        
        sceneGraph.addNode(gameCenterTranslationNode, new TiledBackgroundNode());

        moveNode = new MoveNode(c);
        moveNode.setUp(sceneGraph, levelTranslationNode);
        sceneGraph.addNode(gameCenterTranslationNode, moveNode);
        
        // ///////////////////////////////////////////////////////
        // stuff that gets pushed down with the score levels
        sceneGraph.addNode(gameCenterTranslationNode, levelTranslationNode);
        
        TiledFloorNode floor = new TiledFloorNode();
        sceneGraph.addNode(levelTranslationNode, floor);

        boxesNode = new BoxesNode(c);
        sceneGraph.addNode(levelTranslationNode, boxesNode);
        
    }

    private static GlConfig createGlConfig() {
        return new GlConfig(GlConfig.ColorDepth.Color8888, GlConfig.DepthBufferBits.DEPTH_16, true);
    }

    public static float getBoxSize(SceneGraphContext sc){
    	return (float)sc.surfaceWidth / (float)Game2.BOARD_SIZE;
    }
    
	public static float getBoxLowerLeftXY(SceneGraphContext sc) {
		float boxSize = getBoxSize(sc);

//		float baseXY = -(float)Game2.BOARD_SIZE/2f * boxSize;
//		if (Game2.BOARD_SIZE%2==0) return baseXY + boxSize*0.5f;
//		return baseXY + boxSize;
		
		return -Game2.BOARD_SIZE/2 * boxSize;// + boxSize/2f;
	}
}
