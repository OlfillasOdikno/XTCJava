package de.olfillasodikno.xtc.modules.radar;

import javafx.scene.image.Image;

public class RadarMap {
	private final Image map;
	private final String name;
	private final int x;
	private final int y;
	private final float scale;

	public RadarMap(String name, Image map, int x, int y, float scale) {
		this.map = map;
		this.x = x;
		this.y = y;
		this.scale = scale;
		this.name = name;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public float getScale() {
		return scale;
	}

	public Image getMap() {
		return map;
	}

	public String getName() {
		return name;
	}
}
