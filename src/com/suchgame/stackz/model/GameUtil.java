//package com.suchgame.stackz.model;
//
//public class GameUtil {
//	private GameUtil(){}
//	
//	/**
//	 * Will increase the board levels in such a way that a '0' in the
//	 * given pattern will be the current highest level, '1' will be 1
//	 * level above and so on.
//	 * @param pattern	The new pattern, as rows.
//	 */
//	public static void increaseToPattern(Game game, int[] pattern){
//		int patternIndex = 0;
//		
//		int base = game.getHighestLevel();
//		
//		for (int y = 0; y < Game.BOARD_SIZE; y++){
//			for (int x = 0; x < Game.BOARD_SIZE; x++){
//				game.setLevelTo(x, y, base + pattern[patternIndex], (x>=Game.BOARD_SIZE-1 && y>=Game.BOARD_SIZE-1));
//				patternIndex++;
//			}
//		}
//	}
//}
