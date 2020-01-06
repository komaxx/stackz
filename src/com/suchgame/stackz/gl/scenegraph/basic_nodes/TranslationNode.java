package com.suchgame.stackz.gl.scenegraph.basic_nodes;

import android.opengl.Matrix;

import com.suchgame.stackz.gl.SceneGraphContext;
import com.suchgame.stackz.gl.math.MatrixUtil;
import com.suchgame.stackz.gl.scenegraph.Node;
import com.suchgame.stackz.gl.scenegraph.interaction.InteractionContext;
import com.suchgame.stackz.gl.scenegraph.interaction.Pointer;
import com.suchgame.stackz.gl.util.ObjectsStore;

/**
 * This node has it's own transformation matrix. This matrix will be applied
 * when rendering and also affect all following nodes in the branch. Does not
 * have any specific Rendering capabilities otherwise.
 * 
 * @author Matthias Schicker
 */
public class TranslationNode extends Node {
	private float[] transformationMatrix = MatrixUtil.buildMatrix();
	
	private int changeId = 0; 
	
	protected float currentTranslationX = 0;
	protected float currentTranslationY = 0;
	protected float currentTranslationZ = 0;
	
	public TranslationNode(){
		transforms = true;
	}
	
	@Override
	public boolean onTransform(SceneGraphContext sgContext) {
		float[] modelMatrix = sgContext.modelMatrixStack.push();
		
		sgContext.worldIdStack.push().add(changeId);
		
		float[] cloneMatrix = ObjectsStore.getCloneMatrix(modelMatrix);
		Matrix.multiplyMM(modelMatrix, 0, cloneMatrix, 0, transformationMatrix, 0);
		ObjectsStore.recycleMatrix(cloneMatrix);
		
		sgContext.setMvpMatrixDirty();
		
		inverseTranslate(sgContext.eyePointStack.push());
		
		return true;
	}
	
	@Override
	public void onTransformInteraction(InteractionContext interactionContext) {
		for (int i = 0; i < InteractionContext.MAX_POINTER_COUNT; i++){
			Pointer pointer = interactionContext.getPointers()[i];
			if (!pointer.isActive()) continue;
			float[] rayPoint = pointer.pushRayPoint();
			inverseTranslate(rayPoint);
		}
	}
	
	private void inverseTranslate(float[] v4){
		v4[0] -= currentTranslationX;
		v4[1] -= currentTranslationY;
		v4[2] -= currentTranslationZ;
	}
	
	@Override
	public void onUnTransform(SceneGraphContext sgContext) {
		sgContext.eyePointStack.pop();
		sgContext.modelMatrixStack.pop();
		sgContext.worldIdStack.pop();
		sgContext.setMvpMatrixDirty();
	}
	
	@Override
	public void onUnTransformInteraction(InteractionContext interactionContext) {
		for (int i = 0; i < InteractionContext.MAX_POINTER_COUNT; i++){
			Pointer pointer = interactionContext.getPointers()[i];
			if (pointer.isActive()) pointer.popRayPoint();
		}
	}

	public void setTranslation(float x, float y, float z) {
		if (x == currentTranslationX && y == currentTranslationY && z == currentTranslationZ){
			// nothing changed!
			return;
		}
		
		currentTranslationX = x;
		currentTranslationY = y;
		currentTranslationZ = z;
		
		MatrixUtil.setIdentityM(transformationMatrix);
		Matrix.translateM(transformationMatrix, 0, x, y, z);
		changeId++;
	}
}
