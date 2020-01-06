package com.suchgame.stackz.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import com.suchgame.stackz.R;
import com.suchgame.stackz.core.Box;
import com.suchgame.stackz.gl.RenderContext;
import com.suchgame.stackz.gl.math.GlRect;
import com.suchgame.stackz.gl.texturing.TextureSegment;
import com.suchgame.stackz.gl.texturing.TextureStrip;
import com.suchgame.stackz.gl.texturing.TextureStripConfig;
import com.suchgame.stackz.gl.util.KoLog;

/**
 * Holds the texture coordinates for the various possible states
 * a box can have (dangling, seated, level node). 
 */
public class UiBoxTextures {
	private Drawable back;
	private Drawable backLevelBox;
	
	private Drawable[] seatedStubsNESW;
	private Drawable[] danglingStubsNESW;
	
	private GlRect[] seated = new GlRect[2*2*2*2];
	private GlRect[] dangling = new GlRect[2*2*2*2];
	private GlRect level;
	
	private TextureStrip texStrip;
	
	public UiBoxTextures(Context c){
		Resources r = c.getResources();
		back = r.getDrawable(R.drawable.box_back);
		backLevelBox = r.getDrawable(R.drawable.box_level);
		
		seatedStubsNESW = new Drawable[]{
				r.getDrawable(R.drawable.box_seated_n),
				r.getDrawable(R.drawable.box_seated_e),
				r.getDrawable(R.drawable.box_seated_s),
				r.getDrawable(R.drawable.box_seated_w)
		};
		danglingStubsNESW = new Drawable[]{
				r.getDrawable(R.drawable.box_dangling_n),
				r.getDrawable(R.drawable.box_dangling_e),
				r.getDrawable(R.drawable.box_dangling_s),
				r.getDrawable(R.drawable.box_dangling_w)
		};
	}
	
	public int create(RenderContext renderContext) {
		TextureStripConfig config = new TextureStripConfig();
		config.alphaChannel = true;
		config.basicColor = Color.TRANSPARENT;
		config.mayRotate = false;
		config.minSegmentCount = seated.length + dangling.length + 1;
		config.segmentWidth = back.getIntrinsicWidth();
		config.segmentHeight = back.getIntrinsicHeight();
		
		texStrip = new TextureStrip(config);
		if (! texStrip.create(renderContext)){
			KoLog.e(this, "Could not create box texture :<");
			return -1;
		}
		
		Bitmap tmpBitmap = texStrip.createSegmentTmpBitmap();
		
		makeTextures(renderContext, tmpBitmap, seated, seatedStubsNESW);
		makeTextures(renderContext, tmpBitmap, dangling, danglingStubsNESW);
		makeLevelBoxTexture(renderContext, tmpBitmap);
		
		return texStrip.getHandle();
	}
	
	private void makeLevelBoxTexture(RenderContext renderContext, Bitmap tmpBitmap) {
		tmpBitmap.eraseColor(Color.TRANSPARENT);
		Canvas c = new Canvas(tmpBitmap);
		
		backLevelBox.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
		backLevelBox.draw(c);
		TextureSegment segment = texStrip.getSegment(1,2);
		segment.update(renderContext, tmpBitmap, 0);
		level = new GlRect(segment.getUvCoords());
	}

	private void makeTextures(RenderContext rc, Bitmap tmpBitmap, GlRect[] uvCoords, Drawable[] stubDrawablesNESW) {
		for (int i = 0; i < 16; i++){
			uvCoords[i] = makeTexture(rc, i, tmpBitmap, stubDrawablesNESW);
		}
	}

	private static boolean[] tmpNESW = new boolean[4]; 
	private GlRect makeTexture(RenderContext rc, int texIndex, Bitmap tmpBitmap, Drawable[] stubDrawablesNESW) {
		tmpBitmap.eraseColor(Color.TRANSPARENT);
		Canvas c = new Canvas(tmpBitmap);
		
		back.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
		back.draw(c);
		
		indexToNESW(texIndex);
		for (int i = 0; i < 4; i++){
			 if (tmpNESW[i]) drawStub(c, stubDrawablesNESW[i]);
		}
		
		TextureSegment segment = texStrip.getSegment(1,2);
		segment.update(rc, tmpBitmap, 0);
		
		return new GlRect(segment.getUvCoords());
	}

	private void drawStub(Canvas c, Drawable drawable) {
		drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
		drawable.draw(c);
	}

	private void indexToNESW(int i) {
		tmpNESW[Box.NORTH] = (i & 8) != 0;
		tmpNESW[Box.EAST] =  (i & 4) != 0;
		tmpNESW[Box.SOUTH] = (i & 2) != 0;
		tmpNESW[Box.WEST] =  (i & 1) != 0;
	}
	
	private int NESWtoIndex(Box box) {
		return 
				(box.connectedIdsNESW[Box.NORTH]==null ? 0 : 8) +
				(box.connectedIdsNESW[Box.EAST]==null ? 0 : 4) +
				(box.connectedIdsNESW[Box.SOUTH]==null ? 0 : 2) +
				(box.connectedIdsNESW[Box.WEST]==null ? 0 : 1);
	}

	public GlRect getTexCoords(Box box) {
		if (box.isLevelBox()) return level;
		
		int texIndex = NESWtoIndex(box);

		if (box.isLanded()) {
			return seated[texIndex];
		} else {
			return dangling[texIndex];
		}
	}
}
