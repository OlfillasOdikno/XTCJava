package de.olfillasodikno.xtc.modules.radar;

import de.olfillasodikno.xtc.prop.decoder.Vector;
import de.olfillasodikno.xtc.proto.Entity;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

public class RadarEntity {

	private Entity entity;

	private Vector pos;

	private Paint paint;

	public RadarEntity(Entity e) {
		this.entity = e;
	}

	public void draw(Canvas canvas, double facX, double facY, int mapX, int mapY, float scale) {
		if (pos == null) {
			return;
		}
		GraphicsContext gc = canvas.getGraphicsContext2D();

		float radius = 10/scale;
		double pX = (pos.getX() - mapX) * facX;
		double pY = (mapY - pos.getY()) * facY;
		gc.setFill(paint);
		gc.fillOval(pX - radius / 2.0d, pY - radius / 2.0d, radius, radius);
	}

	public void setPos(Vector pos) {
		this.pos = pos;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setColor(Paint paint) {
		this.paint = paint;
	}
}
