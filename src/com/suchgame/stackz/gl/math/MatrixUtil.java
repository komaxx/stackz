package com.suchgame.stackz.gl.math;

import android.opengl.Matrix;


/**
 * Contains a set of useful static functions, which are missing in
 * Android's Matrix calss, to create and manipulate matrices as needed in 
 * this Gl Lib.
 * 
 * @author Matthias Schicker
 */
public class MatrixUtil {
	public static final byte MATRIX_SIZE = 16;
	
	/**
	 * An identity matrix to be used <b>BUT NOT CHANGED</b> wherever needed.
	 */
	public static final float[] identityMatrix = buildMatrix();
	
	private MatrixUtil(){}
	
	public static float[] buildMatrix(){
		float[] ret = new float[MATRIX_SIZE];
		android.opengl.Matrix.setIdentityM(ret, 0);
		return ret;
	}
	
	public static float[] buildCloneMatrix(float[] matrix){
		float[] ret = new float[MATRIX_SIZE];
		setMatrix(ret, matrix);
		return ret;
	}
	
	public final static float[] setMatrix(float[] result, float[] setSource) {
		System.arraycopy(setSource, 0, result, 0, MATRIX_SIZE);
		return result;
	}

	public static void setRotateEulerM(float[] rm, int rmOffset, 
			float x, float y, float z) {
		x = x * 0.01745329f;		// to radians
		y = y * 0.01745329f;		// to radians
		z = z * 0.01745329f;		// to radians
		float sx = (float) Math.sin(x);
		float sy = (float) Math.sin(y);
		float sz = (float) Math.sin(z);
		float cx = (float) Math.cos(x);
		float cy = (float) Math.cos(y);
		float cz = (float) Math.cos(z);
		float cxsy = cx * sy;
		float sxsy = sx * sy;

		rm[rmOffset + 0] = cy * cz;
		rm[rmOffset + 1] = -cy * sz;
		rm[rmOffset + 2] = sy;
		rm[rmOffset + 3] = 0.0f;

		rm[rmOffset + 4] = sxsy * cz + cx * sz;
		rm[rmOffset + 5] = -sxsy * sz + cx * cz;
		rm[rmOffset + 6] = -sx * cy;
		rm[rmOffset + 7] = 0.0f;

		rm[rmOffset + 8] = -cxsy * cz + sx * sz;
		rm[rmOffset + 9] = cxsy * sz + sx * cz;
		rm[rmOffset + 10] = cx * cy;
		rm[rmOffset + 11] = 0.0f;

		rm[rmOffset + 12] = 0.0f;
		rm[rmOffset + 13] = 0.0f;
		rm[rmOffset + 14] = 0.0f;
		rm[rmOffset + 15] = 1.0f;
	}
	
	/**
	 * Basically the same as Matrix.multiply but with no extra matrix allocation
	 * necessary.
	 */
	private static float[] tmpMultiplyMatrix = buildMatrix();
	public final static void multiplyOnto(float[] target, float[] rhs) {
		setMatrix(tmpMultiplyMatrix, target);
		Matrix.multiplyMM(target, 0, tmpMultiplyMatrix, 0, rhs, 0);
	}
	
	public static void setIdentityM(float[] testMatrix) {
		System.arraycopy(identityMatrix, 0, testMatrix, 0, MATRIX_SIZE);
	}
}
