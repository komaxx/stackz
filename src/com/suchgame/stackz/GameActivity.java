package com.suchgame.stackz;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.crashlytics.android.Crashlytics;
import com.suchgame.stackz.gl.util.KoLog;
import com.suchgame.stackz.ui.GameView;

public class GameActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Crashlytics.start(this);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        makeImmersive();
        
        setContentView(R.layout.main);

        gameView = (GameView) findViewById(R.id.gl_view);

        if (savedInstanceState == null) {
        	// TODO
            // re-create last state.
        }
    }

	@SuppressLint("NewApi")
	private void makeImmersive() {
		if(android.os.Build.VERSION.SDK_INT >= 19) {
        	getWindow().getDecorView()
            .setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.INVISIBLE);
        	
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
            		new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if(visibility == 0) {
                        getWindow().getDecorView().setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                }
            });
        } else {
        	switchOnLightOutMode();
        }
	}
    
    private void switchOnLightOutMode() {
    	View rootView = getWindow().getDecorView();
        try {
            int lightsOutField = View.class.getDeclaredField("STATUS_BAR_HIDDEN").getInt(rootView);

            Method lightsOutMethod = null;
            Method[] methods = View.class.getMethods();
            for (Method m : methods){
            	if (m.getName().equals("setSystemUiVisibility")){
            		lightsOutMethod = m;
            		break;
            	}
            }
            
            if (lightsOutMethod != null){
                lightsOutMethod.invoke(rootView, lightsOutField);
            }
        } catch (Exception e){
        	KoLog.i(this, "Error when trying to set lights-out-mode: " + e.getMessage());
        }
	}

	@Override
    protected void onResume() {
        super.onResume();
        gameView.onResume();
        
        makeImmersive();

    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	gameView.onDestroy();
    }
}
