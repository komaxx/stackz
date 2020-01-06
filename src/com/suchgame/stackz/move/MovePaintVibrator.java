package com.suchgame.stackz.move;

import android.content.Context;
import android.os.Vibrator;

public class MovePaintVibrator {
	private static final long TICK_MS = 2;
	private static final long[] MOVE_START_PATTERN = new long[]{0, 2};
	private static final long[] MOVE_ABORT_PATTERN = new long[]{0, 2, 30, 2, 30, 2 };
	private static final long[] MOVE_DEFINED_PATTERN = new long[]{0, 2, 20, 2};
	
	private Vibrator vibrator;

	public MovePaintVibrator(Context c) {
		vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
	}

	public void tick() {
		vibrator.vibrate(TICK_MS);
	}

	public void moveStart() {
		vibrator.cancel();
		vibrator.vibrate(MOVE_START_PATTERN, -1);
	}

	public void abort() {
		vibrator.cancel();
		vibrator.vibrate(MOVE_ABORT_PATTERN, -1);
	}

	public void moveDefined() {
		vibrator.cancel();
		vibrator.vibrate(MOVE_DEFINED_PATTERN, -1);
	}

}
