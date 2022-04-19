package de.olfillasodikno.xtc.prop.decoder;

public class Vector {
	private float x;
	private float y;
	private float z;

	public Vector() {
	}

	public Vector(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getZ() {
		return z;
	}

	public void setZ(float z) {
		this.z = z;
	}

	public float length() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public float lengthSQ() {
		return x * x + y * y + z * z;
	}

	public float lengthXYSQ() {
		return x * x + y * y;
	}
}
