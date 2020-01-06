package com.suchgame.stackz;

import com.suchgame.stackz.core.Core;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class StackzApplication extends Application {
	private static final String STORAGE_FILE_NAME = "storage";
	private static final String STORAGE_KEY_HIGH_SCORE = "hiScore";
	
	private static StackzApplication runningApplication;


	@Override
	public void onCreate() {
		runningApplication = this;
		Core.init(this);
		super.onCreate();
	}
	
	public static int getHighScore(){
		SharedPreferences preferences = runningApplication.getSharedPreferences(STORAGE_FILE_NAME, Context.MODE_PRIVATE);
		return preferences.getInt(STORAGE_KEY_HIGH_SCORE, 0);
	}
	
	public static void setHighScore(int nuHighScore){
		SharedPreferences preferences = runningApplication.getSharedPreferences(STORAGE_FILE_NAME, Context.MODE_PRIVATE);
		Editor edit = preferences.edit();
		edit.putInt(STORAGE_KEY_HIGH_SCORE, nuHighScore);
		edit.commit();
	}
}
