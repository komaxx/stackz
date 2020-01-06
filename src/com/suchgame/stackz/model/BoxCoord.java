package com.suchgame.stackz.model;

/**
 * A simple, abstract 3D coord.
 * 
 * @author Matthias Schicker, Pockets United (matthias@pocketsunited.com)
 */
public class BoxCoord {
	public int x;
	public int y;
	public int z;
	
	public BoxCoord() {
	}
	
	public BoxCoord(BoxCoord c) {
		set(c);
	}

	public BoxCoord(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public int hashCode() {
		return z<<16 | x<<8 | y;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BoxCoord)) return false;
		
		BoxCoord b = (BoxCoord) o;
		return x==b.x && y==b.y && z==b.z; 
	}

	public final void set(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final void set(BoxCoord c) {
		this.x = c.x;
		this.y = c.y;
		this.z = c.z;
	}
	
	@Override
	public String toString() {
		return "["+x+","+y+","+z+"]";
	}
}
