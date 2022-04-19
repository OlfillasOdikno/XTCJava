package de.olfillasodikno.xtc.modules.radar;

import java.io.File;

import de.olfillasodikno.xtc.events.EntityUpdateEvent;
import de.olfillasodikno.xtc.events.Event;
import de.olfillasodikno.xtc.events.ServerInfoEvent;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.proto.Entity;

public class RadarModule {

	private static RadarView view;

	private RadarMap currentMap;

	private String mapName;

	public RadarModule() {
		CoreManager.INSTANCE.getEventHandler().registerListener(this);
	}

	@Event
	public void onEntityUpdate(EntityUpdateEvent ev) {
		Entity ent = ev.getEntity();
		if (!ent.getServerClass().getClassName().equals("CCSPlayer")) {
			return;
		}
		if (view != null) {
			if (view.getMap() == null && currentMap != null) {
				view.setMap(currentMap);
			}
			view.updateEntity(ent);
		}
	}

	@Event
	public void onServerInfo(ServerInfoEvent ev) {
		mapName = ev.getMap_name();
		System.out.println("Loading: "+mapName);
		updateMap();
	}

	private void updateMap() {
		File f = new File("overviews");
		if (!f.exists()) {
			System.out.println("File: " + f.getAbsolutePath() + " not found.");
			return;
		}
		currentMap = MapLoader.loadMap(f, mapName);
		if (view != null && currentMap != null) {
			view.setMap(currentMap);
		}
	}

	public static void setRadarView(RadarView v) {
		view = v;
	}
}
