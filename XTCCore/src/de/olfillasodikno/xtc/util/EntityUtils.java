package de.olfillasodikno.xtc.util;

import de.olfillasodikno.xtc.proto.Entity;

public class EntityUtils {
	public static boolean isAlive(Entity ent) {
		return !ent.isDormant()
				&& (ent.getStateByName("m_lifeState") == null || (Integer) ent.getStateByName("m_lifeState") == 0);
	}

	public static boolean isEnemy(Entity a, Entity b) {
		Object teamA = a.getStateByName("m_iTeamNum");
		Object teamB = b.getStateByName("m_iTeamNum");
		return teamA == null || teamB == null || teamA != teamB;
	}
}
