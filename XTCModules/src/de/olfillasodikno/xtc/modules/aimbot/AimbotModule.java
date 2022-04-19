package de.olfillasodikno.xtc.modules.aimbot;

import static de.olfillasodikno.xtc.util.EntityUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.olfillasodikno.xtc.events.Event;
import de.olfillasodikno.xtc.events.PacketEntityEvent;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.prop.decoder.Vector;
import de.olfillasodikno.xtc.proto.Entity;

public class AimbotModule {

	public static AimbotModule INSTANCE;

	private static MouseMoveImpl impl;

	private boolean enabled = false;
	private float facYaw = 0;
	private float facPitch = 0;

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private final Map<Integer, Vector> entityPositions;
	private final ArrayList<Entity> enemies;

	private boolean test;

	private boolean inTest;
	private long testStartTime;

	private float lastYaw;
	private float lastPitch;

	private static Runnable testResultListener;

	private Entity target;

	public AimbotModule() {
		CoreManager.INSTANCE.getEventHandler().registerListener(this);
		INSTANCE = this;
		entityPositions = new HashMap<>();
		enemies = new ArrayList<>();
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Event
	public void onPacketEntity(PacketEntityEvent ev) {
		updateAimbot();
	}

	public static void setImpl(MouseMoveImpl impl) {
		AimbotModule.impl = impl;
	}

	private void updateAimbot() {
		if (impl == null) {
			return;
		}
		if (test) {
			test();
		}
		if (!enabled) {
			return;
		}
		Set<Integer> playerIds = CoreManager.INSTANCE.getUsersByEntity().keySet();
		int thePlayerID = CoreManager.INSTANCE.getThePlayer().getEntityID();
		for (int i : playerIds) {
			Vector v = entityPositions.get(i);
			if (v == null) {
				v = new Vector();
			}
			entityPositions.put(i, v);
			if (!getPos(i, i == thePlayerID, v)) {
				entityPositions.remove(i);
			}
		}
		Entity player = CoreManager.INSTANCE.getEntities().get(thePlayerID);
		if (player == null) {
			return;
		}
		enemies.clear();
		entityPositions.keySet().stream().filter(i -> i != thePlayerID)
				.filter(i -> CoreManager.INSTANCE.getEntities().containsKey(i))
				.filter(i -> isEnemy(CoreManager.INSTANCE.getEntities().get(i), player))
				.filter(i -> isAlive(CoreManager.INSTANCE.getEntities().get(i)))
				.forEach(i -> enemies.add(CoreManager.INSTANCE.getEntities().get(i)));
		if (target == null || !isAlive(target) || !entityPositions.containsKey(target.getId())) {
			target = getClosestEnemy(player);
			if (target == null) {
				return;
			}
		}
		Vector neededAngles = calcAnglesNeeded(player, target);
		if (neededAngles == null) {
			return;
		}

		// System.out.println("Target:
		// "+CoreManager.INSTANCE.getUsersByEntity().get(target.getId()).getName());
		float yaw = neededAngles.getX();
		float pitch = neededAngles.getY();
		// System.out.println("yaw: " + yaw + " pitch: " + pitch);
		if (Math.abs(yaw) > 35) {
			return;
		}
		if (Math.abs(yaw) <= 35) {
			yaw /= (180 - Math.abs(yaw)) / 60;
		}
		if (Math.abs(pitch) < 20) {
			pitch /= (180 - Math.abs(pitch)) / 60;
		}
		yaw *= facYaw / 2;
		pitch *= facPitch / 2;
		impl.moveMouse((int) (-pitch), (int) (-yaw));
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	private void test() {
		int playerID = CoreManager.INSTANCE.getThePlayer().getEntityID();
		Entity player = CoreManager.INSTANCE.getEntities().get(playerID);
		if (player == null) {
			System.out.println("Failed to get player entity");
			return;
		}
		Object yawObj = player.getStateByName("m_angEyeAngles[1]");
		Object pitchObj = player.getStateByName("m_angEyeAngles[0]");
		if (yawObj == null || pitchObj == null) {
			System.out.println("Failed to get Player Angles");
			return;
		}
		float yaw = (((Float) yawObj) - 90 + 360 + 180) % 360;
		float pitch = (((Float) pitchObj) - 90 + 360 + 180) % 360;
		if (!inTest) {
			lastYaw = yaw;
			lastPitch = pitch;
			impl.moveMouse(127, 127);
			testStartTime = System.currentTimeMillis();
			inTest = true;
			return;
		}
		if (System.currentTimeMillis() - testStartTime > 1000) {
			inTest = false;
			test = false;
			float dYaw = lastYaw - yaw;
			float dPitch = lastPitch - pitch;
			facYaw = 127 / dYaw;
			facPitch = 127 / dPitch;
			if (testResultListener != null) {
				testResultListener.run();
			}
		}
	}

	public float getFacYaw() {
		return facYaw;
	}

	public float getFacPitch() {
		return facPitch;
	}

	public static void setTestResultListener(Runnable testResultListener) {
		AimbotModule.testResultListener = testResultListener;
	}

	private Vector calcAnglesNeeded(Entity player, Entity target) {
		Object yawObj = player.getStateByName("m_angEyeAngles[1]");
		Object pitchObj = player.getStateByName("m_angEyeAngles[0]");
		if (yawObj == null || pitchObj == null) {
			return null;
		}
		float yaw = (((Float) yawObj) - 90 + 360 + 180) % 360;
		float pitch = (((Float) pitchObj) - 90 + 360 + 180) % 360;

		Vector ret = new Vector();
		Vector vB = entityPositions.get(player.getId());
		Vector vA = entityPositions.get(target.getId());
		float dX = vA.getX() - vB.getX();
		float dY = vA.getY() - vB.getY();
		float dZ = vA.getZ() - vB.getZ() - 12;

		float dXY = (float) Math.sqrt(dX * dX + dY * dY);

		float dYaw = (float) ((360 + 180 - Math.toDegrees(Math.atan2(dX, dY)) - yaw) % 360);
		if (dYaw > 180) {
			dYaw -= 360;
		}
		float dPitch = (float) Math.toDegrees(Math.acos((dZ) / dXY)) - pitch;
		ret.setX(dYaw);
		ret.setY(dPitch);
		return ret;
	}

	private Entity getClosestEnemy(Entity player) {
		Entity closest = null;
		float minDT = Float.MAX_VALUE;
		for (Entity ent : enemies) {
			float dt = distanceSQXY(player, ent);
			if (dt < minDT) {
				closest = ent;
				minDT = dt;
			}
		}
		return closest;
	}

	private float distanceSQXY(Entity a, Entity b) {
		Vector vA = entityPositions.get(a.getId());
		Vector vB = entityPositions.get(b.getId());
		float dX = vA.getX() - vB.getX();
		float dY = vA.getY() - vB.getY();
		return dX * dX + dY * dY;
	}

	private boolean getPos(int i, boolean isPlayer, Vector ret) {
		Entity ent = CoreManager.INSTANCE.getEntities().get(i);
		if (ent == null) {
			return false;
		}

		Object xyObj = ent.getStateByName("m_vecOrigin", isPlayer ? 0 : 1);
		Object zObj = ent.getStateByName("m_vecOrigin[2]", isPlayer ? 0 : 1);
		if (xyObj == null || zObj == null) {
			return false;
		}
		ret.setX(((Vector) xyObj).getX());
		ret.setY(((Vector) xyObj).getY());
		ret.setZ(((Float) zObj) + (ent.getStateByName("m_vecViewOffset[2]") != null
				? (Float) ent.getStateByName("m_vecViewOffset[2]")
				: (ent.getStateByName("m_vecMaxs") == null ? 72 : ((Vector) ent.getStateByName("m_vecMaxs")).getZ())));
		return true;
	}

}
