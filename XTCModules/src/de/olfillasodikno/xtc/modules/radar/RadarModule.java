package de.olfillasodikno.xtc.modules.radar;

import java.awt.Color;
import java.io.File;

import de.olfillasodikno.xtc.events.EntityUpdateEvent;
import de.olfillasodikno.xtc.events.Event;
import de.olfillasodikno.xtc.events.ServerInfoEvent;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.prop.decoder.Vector;
import de.olfillasodikno.xtc.proto.Entity;
import de.olfillasodikno.xtc.proto.UserInfo;

public class RadarModule {

	private static final File overviews = new File("overviews");

	private Viewer viewer;

	public RadarModule() {
		CoreManager.INSTANCE.getEventHandler().registerListener(this);

		viewer = Viewer.create();
	}

	@Event
	public void onEntityUpdate(EntityUpdateEvent ev) {
		Entity ent = ev.getEntity();
		if (!ent.getServerClass().getClassName().equals("CCSPlayer")) {
			viewer.radar.repaint();
			return;
		}
		updateRotation(ent);
		Vector pos = null;
		for (int i : ev.getIndices()) {
			if (ent.getProps()[i].getVarName().equals("m_vecOrigin")) {
				Object obj = ev.getEntity().getState()[i];
				if (obj instanceof Vector) {
					pos = (Vector) obj;
				}
			}
		}
		if (pos == null) {
			viewer.radar.repaint();
			return;
		}
		updateEntity(ent, pos);
		viewer.radar.repaint();
	}

	private void updateRotation(Entity ent) {
		if (ent.getId() == CoreManager.INSTANCE.getThePlayer().getEntityID()) {
			Object yawObj = ent.getStateByName("m_angEyeAngles[1]");
			if (yawObj instanceof Float) {
				float yaw = (((Float) yawObj).floatValue() - 90 + 360) % 360;
				viewer.radar.setRotation(yaw);
			}
		}
		if (viewer.radar.entities.containsKey(ent.getId())) {
			viewer.radar.entities.get(ent.getId()).setEnt(ent);
		}
	}

	@Event
	public void onServerInfo(ServerInfoEvent ev) {
		File f = new File(overviews, ev.getMap_name() + ".txt");
		if (!f.exists()) {
			System.out.println("File: " + f.getAbsolutePath() + " not found.");
			return;
		}
		Radar radar = Radar.fromFile(f);
		viewer.setRadar(radar);
	}

	private int myTeam = -1;

	private void updateEntity(Entity ent, Vector pos) {

		Object obj = ent.getStateByName("m_iTeamNum");
		int team = -1;
		if (obj != null && obj instanceof Integer) {
			team = ((Integer) obj).intValue();
		}

		if (ent.getId() == CoreManager.INSTANCE.getThePlayer().getEntityID()) {
			myTeam = team;
			viewer.radar.setPlayerXY(pos.getX(), pos.getY());
		}

		if (!viewer.radar.entities.containsKey(ent.getId())) {
			viewer.radar.addEntity(ent);
			updateTeamColor(ent, team);
		}

		updateTeamColor(ent, team);

		viewer.radar.entities.get(ent.getId()).setPosVec(pos);
		viewer.radar.entities.get(ent.getId()).setEnt(ent);
	}

	private void updateTeamColor(Entity ent, int team) {
		viewer.radar.entities.get(ent.getId()).setTeam(team);
		if (ent.getId() == CoreManager.INSTANCE.getThePlayer().getEntityID()) {
			viewer.radar.entities.get(ent.getId()).setColor(Color.WHITE);
		} else {
			if (team == myTeam) {
				viewer.radar.entities.get(ent.getId()).setColor(Color.LIGHT_GRAY);
			} else {
				viewer.radar.entities.get(ent.getId()).setColor(Color.RED);
			}
		}
	}
}
