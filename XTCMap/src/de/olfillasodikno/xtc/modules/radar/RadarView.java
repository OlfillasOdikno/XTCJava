package de.olfillasodikno.xtc.modules.radar;

import static de.olfillasodikno.xtc.util.EntityUtils.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.prop.decoder.Vector;
import de.olfillasodikno.xtc.proto.Entity;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class RadarView {
	private RadarMap map;
	private float playerX;
	private float playerY;
	private float playerRot;
	private int playerTeam;

	private float scale = 1;

	private Runnable onChangeListener;
	private Runnable onUpdateListener;

	private Paint backgroundPaint = Color.BLACK;

	public void setOnChangeListener(Runnable onChangeListener) {
		this.onChangeListener = onChangeListener;
	}

	public final Map<Integer, RadarEntity> entities = new ConcurrentHashMap<>();

	public void setMap(RadarMap map) {
		this.map = map;
		if (onChangeListener != null) {
			onChangeListener.run();
		}
	}

	public RadarMap getMap() {
		return map;
	}

	public void onDraw(Canvas canvas) {
		if (map == null || map.getMap() == null) {
			return;
		}
		double w = canvas.getWidth();
		double h = canvas.getHeight();
		
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(backgroundPaint);
		gc.fillRect(0, 0, w, h);

		double r = map.getMap().getWidth() / map.getMap().getHeight();

		if (w > h) {
			w = (int) (h * r);
		} else {
			h = (int) (w / r);
		}
		double facX = (w / map.getMap().getWidth()) / map.getScale();
		double facY = (h / map.getMap().getHeight()) / map.getScale();

		double cX = (playerX - map.getX()) * facX;
		double cY = (map.getY() - playerY) * facY;
		
		
		gc.save();
		gc.translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
		gc.rotate(playerRot);
		gc.scale(scale, scale);
		gc.translate(-cX,-cY);

		gc.drawImage(map.getMap(), 0, 0, w, h);


		entities.values().stream().filter(e -> isAlive(e.getEntity()))
				.forEach(e -> e.draw(canvas, facX, facY, map.getX(), map.getY(),scale));
		gc.restore();
	}

	private void addEntity(Entity e) {
		synchronized (entities) {
			entities.put(e.getId(), new RadarEntity(e));
		}
	}

	public void updateEntity(Entity e) {
		if (!entities.containsKey(e.getId())) {
			addEntity(e);
		}
		RadarEntity ent = entities.get(e.getId());
		ent.setEntity(e);
		if (CoreManager.INSTANCE.getThePlayer() == null) {
			return;
		}
		if (e.getId() == CoreManager.INSTANCE.getThePlayer().getEntityID()) {
			updatePlayer(e, ent);
		} else {
			Object pObj = e.getStateByName("m_vecOrigin", 1);
			Object tObj = e.getStateByName("m_iTeamNum");
			if (pObj != null && pObj instanceof Vector) {
				Vector pos = (Vector) pObj;
				ent.setPos(pos);
			}
			if (tObj != null) {
				if (playerTeam != (Integer) tObj) {
					ent.setColor(Color.RED);
				} else {
					ent.setColor(Color.LIGHTGRAY);
				}
			}
		}
		if(onUpdateListener!=null) {
			onUpdateListener.run();
		}
	}
	
	public void setOnUpdateListener(Runnable onUpdateListener) {
		this.onUpdateListener = onUpdateListener;
	}

	private void updatePlayer(Entity e, RadarEntity ent) {
		ent.setColor(Color.WHITE);
		Object pObj = e.getStateByName("m_vecOrigin");
		Object rObj = e.getStateByName("m_angEyeAngles[1]");
		Object tObj = e.getStateByName("m_iTeamNum");
		if (rObj != null) {
			playerRot = (((Float) rObj) - 90 + 360) % 360;
		}
		if (pObj != null && pObj instanceof Vector) {
			Vector pos = (Vector) pObj;
			playerX = pos.getX();
			playerY = pos.getY();
			ent.setPos(pos);
		}
		if (tObj != null) {
			playerTeam = (Integer) tObj;
		}
	}
	
	public float getScale() {
		return scale;
	}
	
	public void setScale(float scale) {
        this.scale = Math.max(1f, Math.min(scale, 10.0f));
	}
}
