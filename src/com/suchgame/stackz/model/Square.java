package com.suchgame.stackz.model;

public class Square {
	public int llX;
	public int llY;
	
	public int size;
	
	public int z;

	public Square(){
	}
	
	public Square(Square toCopy) {
		llX = toCopy.llX;
		llY = toCopy.llY;
		size = toCopy.size;
		z = toCopy.z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + size;
		result = prime * result + llX;
		result = prime * result + llY;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Square other = (Square) obj;
		if (size != other.size)
			return false;
		if (llX != other.llX)
			return false;
		if (llY != other.llY)
			return false;
		if (z != other.z)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "["+llX+","+llY+", "+z+" - "+size+"]";
	}
}
