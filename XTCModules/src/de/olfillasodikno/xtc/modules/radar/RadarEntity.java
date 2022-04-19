package de.olfillasodikno.xtc.modules.radar;

import java.awt.Color;
import java.awt.Graphics;

import de.olfillasodikno.xtc.prop.decoder.Vector;
import de.olfillasodikno.xtc.proto.Entity;
import de.olfillasodikno.xtc.proto.UserInfo;

public class RadarEntity {

	private Color color = Color.RED;
	private int id;
	private Entity ent;
	private UserInfo info;

	private int team;

	private Vector posVec;
	private long last;

	public void paint(Graphics g, float facX, float facY, int mapX, int mapY, float mapSize) {
		if (posVec == null || (ent != null 	&& ent.getStateByName("m_lifeState") != null && ((Integer) ent.getStateByName("m_lifeState")).intValue() != 0) || System.currentTimeMillis()-last > 10000) {
			return;
		}
		int size = 10;
		float pX = (posVec.getX() - mapX) / mapSize;
		float pY = (mapY - posVec.getY()) / mapSize;
		g.setColor(color);
		g.fillOval((int) (pX * facX) - size / 2, (int) (pY * facY) - size / 2, size, size);
	}

	public void setTeam(int team) {
		this.team = team;
	}

	public void setEnt(Entity ent) {
		last = System.currentTimeMillis();
		this.ent = ent;
	}
	
	public int getTeam() {
		return team;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setPosVec(Vector posVec) {
		last = System.currentTimeMillis();
		this.posVec = posVec;
	}
}
