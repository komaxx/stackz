package com.suchgame.stackz.ui.menu;

import java.util.HashMap;

import android.graphics.Rect;
import android.graphics.RectF;

public interface IMenuNode {
	/**
	 * Drawable resource ids that are required by this node.
	 */
	int[] getNecessaryResourceIds();
	
	/**
	 * Delivers the texture coords and the texture handle. Use the ids delivered
	 * in getNecessaryResourceIds as keys in the HashMaps.
	 */
	void setTexCoords(int textureHandle, HashMap<Integer, Rect> texCoordsPx, HashMap<Integer, RectF> texCoordsUv);
}
