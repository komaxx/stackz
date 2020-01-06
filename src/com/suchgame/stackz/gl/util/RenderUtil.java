package com.suchgame.stackz.gl.util;

import java.nio.FloatBuffer;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

import com.suchgame.stackz.gl.texturing.Texture;

/**
 * Contains all sorts of utility constants and functions useful
 * when rendering.
 * 
 * @author Matthias Schicker
 */
public class RenderUtil {
	private static final Random r = new Random(System.currentTimeMillis());
	
	public static final int SHORT_SIZE_BYTES = 2;
	public static final int FLOAT_SIZE_BYTES = 4;
    
	public static final short[] indexArrayDummy = new short[0];

	private static final float PI = (float) Math.PI;

	private static Rect tmpRect1 = new Rect();
	private static Rect tmpRect2 = new Rect();

	
	public static Bitmap emptyBmp = Bitmap.createBitmap(1, 1, Config.ARGB_4444);
	static {
		emptyBmp.eraseColor(Color.TRANSPARENT);
	}
	
    public static boolean checkGlError(String op) {
        int error;
        boolean errorFound = false;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
        	KoLog.e("RenderUtil", op + ": glError " + Integer.toHexString(error) + ", " + GLU.gluErrorString(error));
        	errorFound = true;
        }
        return errorFound;
    }
    
    /**
     * Computes a smooth transition from an arbitrary input space into [0|1]
     */
    public static float smoothStep(float value, float min, float max){
    	value = clamp(value, min, max) - min;
    	return value / (max-min);
    }
    
    public static float degreesToRadians(float degrees){
    	return (degrees / 180f) * PI;
    }
    
    /**
     * Prints the content of the buffer without changing the mark or pos values.
     */
    public static String printBuffer(FloatBuffer buffer, int start, int length){
    	StringBuffer ret = new StringBuffer();
    	ret.append('(');
    	
    	if (length > 0){
    		ret.append(buffer.get(start));
    	}
    	for (int i = start+1; i < start+length; i++){
    		ret.append(" | ").append(buffer.get(start));
    	}
    	
    	ret.append(')');
    	return ret.toString();
    }
    
    public static String toString(PointF p, int digits) {
		float digitFactor = 1;
		for (int i = 0; i < digits; i++) digitFactor *= 10;
		StringBuffer ret = new StringBuffer();
		ret.append('(');
		ret.append( (int)(p.x*digitFactor) / digitFactor );
		ret.append(" | ");
		ret.append( (int)(p.y*digitFactor) / digitFactor );
		ret.append(')');
		return ret.toString();
	}
    
    public static String printFloat(float f, int maxDigits){
		float digitFactor = (float) Math.pow(10, maxDigits);
		return "" + ((int)(f*digitFactor) / digitFactor);
    }
    
	private static float[] tmpHsv = new float[3];
	public static int getRandomColor() {
		tmpHsv[0] = r.nextFloat() * 360f;
		tmpHsv[1] = 1;
		tmpHsv[2] = 1;
		return Color.HSVToColor(tmpHsv);
	}
	
	public static float getRandom(float min, float max){
		return min + r.nextFloat() * (max-min);
	}

	/**
	 * Delivers a rescaled version of the "src"-Bitmap. The result has the same aspect ratio as the input image and
	 * is smaller-or-equal than maxSizeX and maxSizeY
	 */
	public static Bitmap getRescaledBitmapWithSameAspectRatio(Bitmap src, float maxWidth, float maxHeight){
		try {
			float inputAspectRatio = (float)src.getWidth() / (float)src.getHeight();
			if (inputAspectRatio > (float)maxWidth / (float)maxHeight){
				return Bitmap.createScaledBitmap(src, 
						clamp((int)maxWidth, 1, 10000), 
						clamp((int)(maxWidth / inputAspectRatio), 1, 10000), true);
			}
			return Bitmap.createScaledBitmap(src, 
					clamp((int)(maxHeight * inputAspectRatio), 1, 10000),
					clamp((int)maxHeight,1, 10000), true);
		} catch (OutOfMemoryError error){
			KoLog.e("RenderUtil", "[getRescaledBitmap] Caught an OutOfMemory-error!");
			System.gc();
			return emptyBmp;
		}
	}
	

    /**
     * Converts a color defined as HSV to a rgb color array, one float [0,1] for each color,
     * Input outside of their respective ranges will be clamped to a valid value.
     * @param result	an array to fill. Must have at least length=3. May be null.
     * @param h			Hue in [0 .. 360]
     * @param s			Saturation [0...1]
     * @param v			Value [0...1]
     * @return			If result!=null: result, else: new array of length 4.
     */
	public static float[] hsv2rgb(float[] result, int h, int s, int v) {
		tmpHsv[0] = h;
		tmpHsv[1] = s;
		tmpHsv[2] = v;
		return color2floatsRGB(result, Color.HSVToColor(tmpHsv));
	}

	
	/**
	 * Converts a color to a float array, one float [0,1] for each color
	 * @param ret		an array to fill. Must have at least length=3. May be null.
	 * @param color		the color to convert
	 * @return			If ret!=null: ret, else: new array of length 4.
	 */
	public static float[] color2floatsRGB(float[] ret, int color) {
		if (ret == null){
			ret = new float[4];
			ret[3] = 1;
		}
		ret[0] = (float)Color.red(color)/255f;
		ret[1] = (float)Color.green(color)/255f;
		ret[2] = (float)Color.blue(color)/255f;
		return ret;
	}
	
	/**
	 * Converts a color to a float array, one float [0,1] for each color
	 * @param ret		an array to fill. Must have at least length=4. May be null.
	 * @param color		the color to convert
	 * @return			If ret!=null: ret, else: new array of length 4.
	 */
	public static float[] color2floatsRGBA(float[] ret, int color) {
		if (ret == null){
			ret = new float[4];
		}
		ret[0] = (float)Color.red(color)/255f;
		ret[1] = (float)Color.green(color)/255f;
		ret[2] = (float)Color.blue(color)/255f;
		ret[3] = (float)Color.alpha(color)/255f;
		return ret;
	}
	
	/**
	 * Tints / shades a color. 
	 * @param basicColor	The basic color to be modified
	 * @param tintDelta		The tint factor in [-1;1]. 
	 * <code>-1</code> will deliver perfect black, <code>1</code> either white or the
	 * highest possible tint (if hue changes are not allowed). <code>0</code> is neutral,
	 * will not change the color.
	 * @return	the tinted color.
	 */
	public static int tintColor(int basicColor, float tintDelta){
		if (tintDelta == 0) return basicColor;
		
		int r = Color.red(basicColor);
		int g = Color.green(basicColor);
		int b = Color.blue(basicColor);

		if (tintDelta < 0){		// shading
			r = clamp((int)(r*(1+tintDelta)), 0, 255);
			g = clamp((int)(g*(1+tintDelta)), 0, 255);
			b = clamp((int)(b*(1+tintDelta)), 0, 255);
			return Color.rgb(r, g, b);
		}
		
		r = clamp(r + (int)((255-r)*(tintDelta)), 0, 255);
		g = clamp(g + (int)((255-g)*(tintDelta)), 0, 255);
		b = clamp(b + (int)((255-b)*(tintDelta)), 0, 255);
		
		return Color.rgb(r, g, b);
	}
	
	/**
	 * Interpolates a sin-value meandering between min and max. Valuable
	 * for test purposes.
	 */
	public static float sinStep(long frameNanoTime, float intervalLengthMs, float min, float max){
		double phase = (frameNanoTime / 1000000) * Math.PI / intervalLengthMs;
		
		double ret = Math.sin(phase);
		ret = 
			// normalize to [0;1]
			(ret+1.0)/2.0
			// stretch to result area
			* (max-min);
		// translate to result area
		ret += min;

		return (float) ret;
	}
	
	/**
	 * Clamps <code>value</code> to the specified interval. Note: There are
	 * no sanity checks in here, so you should make sure yourself that 
	 * minIncl &lt;= maxIncl
	 * @param value
	 * 		The value to be clamped
	 * @param minIncl
	 * 		The smallest allowed value
	 * @param maxIncl
	 * 		The biggest allowed value
	 * @return
	 * 		value clamped to [minIncl;maxIncl]
	 */
	public static final int clamp(int value, int minIncl, int maxIncl){
		if (value > maxIncl) value = maxIncl;
		if (value < minIncl) value = minIncl;
		return value;
	}
	
	/**
	 * Clamps <code>value</code> to the specified interval. Note: There are
	 * no sanity checks in here, so you should make sure yourself that 
	 * minIncl &lt;= maxIncl
	 * @param value
	 * 		The value to be clamped
	 * @param minIncl
	 * 		The smallest allowed value
	 * @param maxIncl
	 * 		The biggest allowed value
	 * @return
	 * 		value clamped to [minIncl;maxIncl]
	 */
	public static final float clamp(float value, float minIncl, float maxIncl){
		if (value > maxIncl) value = maxIncl;
		if (value < minIncl) value = minIncl;
		return value;
	}

	/**
	 * Clamps <code>value</code> to the specified interval. Note: There are
	 * no sanity checks in here, so you should make sure yourself that 
	 * minIncl &lt;= maxIncl
	 * @param value
	 * 		The value to be clamped
	 * @param minIncl
	 * 		The smallest allowed value
	 * @param maxIncl
	 * 		The biggest allowed value
	 * @return
	 * 		value clamped to [minIncl;maxIncl]
	 */
	public static final double clamp(double value, double minIncl, double maxIncl){
		if (value > maxIncl) value = maxIncl;
		if (value < minIncl) value = minIncl;
		return value;
	}
	
	public static final byte max(byte a, byte b) {
		return (a>b) ? a : b;
	}

	public static boolean isNullOrEmpty(String s) {
		return s == null || s.length() < 1;
	}
	
	/**
     * Finds the minimum of three <code>int</code> values
     * @return  the smallest of the values
     */
    public static int min(int a, int b, int c) {
        if (b < a) a = b;
        if (c < a) a = c;
        return a;
    }
    
    private static final Rect sourceRect = new Rect();
    private static final Rect paintRect = new Rect();
    private static final Paint bmpPaint = new Paint();
    static { 
    	bmpPaint.setAntiAlias(true); 
    	bmpPaint.setFilterBitmap(true);
    }
    public static Bitmap getRescaledBitmapToFill(Bitmap src, float fillWidth, float fillHeight){
	    try {
	    	Config config = src.getConfig();
	    	if (config == null) config = Config.ARGB_8888;
	    	
	        Bitmap ret = Bitmap.createBitmap((int)fillWidth, (int)fillHeight, config);
            float sourceAspectRatio = (float)src.getWidth() / (float)src.getHeight();
            float targetAspectRatio = (float)fillWidth / (float)fillHeight;
            Canvas c = new Canvas(ret);
            
            float sourceWidth = src.getWidth();
            float sourceHeight = src.getHeight();
            if (sourceAspectRatio > targetAspectRatio){
                sourceRect.top = 0;
                sourceRect.bottom = src.getHeight();
                int snip = (int) ((sourceWidth - (targetAspectRatio * sourceHeight)) / 2f);
                sourceRect.left = snip;
                sourceRect.right = src.getWidth() - snip;
            } else {
                sourceRect.left = 0;
                sourceRect.right = (int) sourceWidth;
                int snip = (int) ((sourceHeight - (sourceWidth / targetAspectRatio)) / 2f);
                sourceRect.top = snip;
                sourceRect.bottom = src.getHeight()-snip;
            }
            paintRect.left = 0;
            paintRect.top = 0;
            paintRect.right = (int) fillWidth;
            paintRect.bottom = (int) fillHeight;
            c.drawBitmap(src, sourceRect, paintRect, bmpPaint);
//            c.drawBitmap(src, 0, 0, bmpPaint);
            return ret;
        } catch (OutOfMemoryError error){
            Log.e("Aloqa","[getFillBitmap] Caught an OutOfMemory-error!");
            return null;
        }
	}

    /**
     * Adjusts the toFit rect to fill the fitInto rect but keeps the aspect
     * ratio unchanged. 
     * 
     * @return	the toFit rect
     */
	public static RectF fitIntoWithSameAspectRatio(RectF toFit, RectF fitInto) {
		float sourceAspectRatio = toFit.width() / toFit.height();
        float targetAspectRatio = fitInto.width() / fitInto.height();
        
        if (sourceAspectRatio > targetAspectRatio){
        	toFit.left = fitInto.left;
        	toFit.right = fitInto.right;
        	float snip = (fitInto.height() - (fitInto.width() / sourceAspectRatio)) / 2f;
        	toFit.top = fitInto.top+snip;
        	toFit.bottom = fitInto.bottom-snip;
        } else {
        	toFit.top = fitInto.top;
        	toFit.bottom = fitInto.bottom;
        	float snip = (fitInto.width() - (fitInto.height()) * sourceAspectRatio) / 2f;
        	toFit.left = fitInto.left + snip;
        	toFit.right = fitInto.right - snip;
        }
        
        return toFit;
	}
	
    /**
     * Adjusts the toFit rect to fill the fitInto rect but keeps the aspect
     * ratio unchanged. 
     * 
     * @return	the toFit rect
     */
	public static Rect fitIntoWithSameAspectRatio(Rect toFit, Rect fitInto) {
		float sourceAspectRatio = (float)toFit.width() / (float)toFit.height();
        float targetAspectRatio = (float)fitInto.width() / (float)fitInto.height();
        
        if (sourceAspectRatio > targetAspectRatio){
        	toFit.left = fitInto.left;
        	toFit.right = fitInto.right;
        	int snip = (int)((fitInto.height() - (fitInto.width() / sourceAspectRatio)) / 2f);
        	toFit.top = fitInto.top+snip;
        	toFit.bottom = fitInto.bottom-snip;
        } else {
        	toFit.top = fitInto.top;
        	toFit.bottom = fitInto.bottom;
        	int snip = (int)((fitInto.width() - (fitInto.height()) * sourceAspectRatio) / 2f);
        	toFit.left = fitInto.left + snip;
        	toFit.right = fitInto.right - snip;
        }
        
        return toFit;
	}
    
	/**
	 * Shrinks the given uvCoords by one pixel. This is valuable to avoid "bleeding"
	 * of one texture into another.
	 * @return	The given rect 'uvCoords'
	 */
	public static RectF inlayOnePixel(Texture t, RectF uvCoords) {
		uvCoords.inset(1f / (float)t.getWidth(), 1f / (float)t.getHeight());
		
		return uvCoords;
	}

	/**
	 * Sets the drawables bounds to fit central in the given bounds (downscaling
	 * if necessary but not upscaling).
	 */
	public static void centerIn(Drawable d, Rect bounds) {
		tmpRect1.left = bounds.centerX() - d.getIntrinsicWidth()/2;
		tmpRect1.right = tmpRect1.left + d.getIntrinsicWidth();
		tmpRect1.top = bounds.centerY() - d.getIntrinsicHeight()/2;
		tmpRect1.bottom = tmpRect1.top + d.getIntrinsicHeight();
		
		if (tmpRect1.width() > bounds.width() || tmpRect1.height() > bounds.height()){
			fitIntoWithSameAspectRatio(tmpRect1, bounds);
		}
		d.setBounds(tmpRect1);
	}
	
	/**
	 * Sets the drawables bounds to fit central in the given bounds (downscaling
	 * if necessary but not upscaling).
	 */
	public static void centerIn(Drawable d, int left, int top, int right, int bottom) {
		tmpRect2.set(left, top, right, bottom);
		centerIn(d, tmpRect2);
	}

	/**
	 * Just fills the bmp with a test grid. Valuable to verify pixel-perfect rendering.
	 */
	public static void renderTestGrid(Bitmap updateBmp) {
		updateBmp.eraseColor(Color.WHITE);
		
		Paint p = new Paint();
		p.setColor(Color.BLACK);
		
		Canvas c = new Canvas(updateBmp);
		
		for (int x = 0; x < updateBmp.getWidth(); x +=3){
			c.drawLine(x, 0, x, 10000, p);
		}
		for (int y = 0; y < updateBmp.getHeight(); y +=3){
			c.drawLine(0, y, 10000, y, p);
		}

		p.setColor(0xFF00B000);
		c.drawLine(0, 0, updateBmp.getWidth(), updateBmp.getHeight(), p);
		c.drawLine(0, updateBmp.getHeight(), updateBmp.getWidth(), 0, p);
		
		p.setColor(0xFFA00080);
		float centerX = updateBmp.getWidth()/2;
		float centerY = updateBmp.getHeight()/2;
		c.drawLine(centerX-10000, centerY-10000, centerX+10000, centerY+10000, p);
		c.drawLine(centerX-10000, centerY+10000, centerX+10000, centerY-10000, p);
	}

	public static long nsFromMs(int ms) {
		return ms * 1000l * 1000l;
	}
	
	public static long msFromNs(long ns) {
		return ns / 1000l * 1000l;
	}

	public static int sFromNs(long ns) {
		return (int) (ns/(1000l*1000l*1000l));
	}
}
