package com.suchgame.stackz.ui.menu;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;

import com.suchgame.stackz.core.Core;
import com.suchgame.stackz.core.Core.GameState;
import com.suchgame.stackz.core.listener.IGameStateListener;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.scenegraph.IGlRunnable;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.SceneGraph;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.texturing.Texture;
import com.suchgame.stackz.gl.texturing.TextureConfig;
import com.suchgame.stackz.gl.util.AtlasPainter;
import com.suchgame.stackz.model.BoxCoord;
import com.suchgame.stackz.model.Square;

/**
 * The central class that controls that state of the UI that is not
 * part of the game itself (especially: menus of all kind).
 * 
 * @author Matthias Schicker, SuchGame
 */
public class MenuNode extends Node {
	private CountDownNode countDownNode = new CountDownNode(this);
	private MainMenuNode mainMenuNode = new MainMenuNode(this);
	private PauseMenuNode pausedMenuNode = new PauseMenuNode(this);
	private ChromeScoreNode scoreNode = new ChromeScoreNode();
	private FinishedMenuNode finishedNode = new FinishedMenuNode(this);
	
	private IMenuNode[] menuNodes = new IMenuNode[]{
			countDownNode, mainMenuNode, pausedMenuNode, scoreNode, finishedNode
	};
	private Texture texture;

	
	public void setup(SceneGraph sceneGraph, Context c) {
		sceneGraph.addNode(this, countDownNode);
		sceneGraph.addNode(this, mainMenuNode);
		sceneGraph.addNode(this, pausedMenuNode);
		sceneGraph.addNode(this, scoreNode);
		sceneGraph.addNode(this, finishedNode);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.minHeight = 2048;
		tc.minWidth = 2048;
		tc.mipMapped = false;
		
		texture = new Texture(tc);
		texture.create(renderContext);
		textureHandle = texture.getHandle();
		
		int[] drawableIds = getDrawableIds();
		Rect[] pxCoords = AtlasPainter.drawAtlas(renderContext.resources, drawableIds, texture, 2);
		RectF[] uvCoords = AtlasPainter.convertPxToUv(texture, pxCoords);
		
		HashMap<Integer, Rect> texCoordsPx = new HashMap<Integer, Rect>();
		HashMap<Integer, RectF> texCoordsUv = new HashMap<Integer, RectF>();
		for (int i = 0; i < drawableIds.length; i++){
			texCoordsPx.put(drawableIds[i], pxCoords[i]);
			texCoordsUv.put(drawableIds[i], uvCoords[i]);
		}
		
		for (IMenuNode n : menuNodes){
			n.setTexCoords(textureHandle, texCoordsPx, texCoordsUv);
		}
	}

	private int[] getDrawableIds() {
		ArrayList<Integer> resourceIds = new ArrayList<Integer>();
		for (IMenuNode n : menuNodes){
			int[] resIds = n.getNecessaryResourceIds();
			for (int resId : resIds) resourceIds.add(resId);
		}
		
		// to array
		int[] drawableIds = new int[resourceIds.size()];
		for (int i = 0; i < drawableIds.length; i++) drawableIds[i] = resourceIds.get(i);
		
		return drawableIds;
	}
	
	// ///////////////////////////////////////////////////////////////////////
	// calls from sub-nodes
	
	public void startTapped(SceneGraphContext sc){
		Core.get().changeGameState(sc, GameState.COUNTING_DOWN);
	}
	
	public void countDownDone(SceneGraphContext sc) {
		Core.get().changeGameState(sc, GameState.RUNNING);
	}

	public void pauseTapped(SceneGraphContext sc) {
		Core.get().changeGameState(sc, GameState.PAUSED);
	}

	/**
	 * 'Quit' tapped in pause menu
	 */
	public void pausedQuitTapped(SceneGraphContext sc) {
		Core.get().changeGameState(sc, GameState.MAIN_MENU);
	}

	/**
	 * 'Resume' tapped in pause menu
	 */
	public void pausedResumeTapped(InteractionContext ic) {
		Core.get().changeGameState(ic, GameState.COUNTING_DOWN);
	}

	public void onPause() {
		// Core.get().changeGameState(ic, GameState.PAUSED);
	}
	
	public void onResume() {
		//
	}

}
