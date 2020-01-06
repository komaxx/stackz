package com.suchgame.stackz.model;

public class PatternCoord {
	public int x;
	public int y;

	public PatternCoord(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public PatternCoord(PatternCoord other) {
		this.x = other.x;
		this.y = other.y;
	}

	public void move(int deltaX, int deltaY) {
		x += deltaX;
		y += deltaY;
	}

	public void set(PatternCoord other) {
		x = other.x;
		y = other.y;
	}

	public void set(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PatternCoord other = (PatternCoord) obj;
		if (x != other.x) return false;
		if (y != other.y) return false;
		return true;
	}

	@Override
	public String toString() {
		return "["+x+","+y+"]";
	}
}
