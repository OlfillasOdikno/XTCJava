package de.olfillasodikno.xtc.manager;

import java.util.HashMap;

import de.olfillasodikno.xtc.ClassesManager;
import de.olfillasodikno.xtc.SniffImpl;
import de.olfillasodikno.xtc.handler.EventHandler;
import de.olfillasodikno.xtc.proto.Entity;
import de.olfillasodikno.xtc.proto.UserInfo;

public class CoreManager {
	public static CoreManager INSTANCE;

	private UserInfo thePlayer;
	private final PacketManager pm;

	private final ClassesManager classManager;
	private final EventHandler evHandler;

	private final HashMap<Integer, Entity> entities;
	private final HashMap<Integer, UserInfo> usersByEntity;
	private final HashMap<Integer, UserInfo> usersByID;

	private SniffImpl impl;

	public CoreManager() {
		INSTANCE = this;
		classManager = new ClassesManager();
		entities = new HashMap<>();
		usersByEntity = new HashMap<>();
		usersByID = new HashMap<>();
		evHandler = new EventHandler();
		pm = new PacketManager();
		initModules();
	}

	public SniffImpl getImpl() {
		return impl;
	}

	public void setImpl(SniffImpl impl) {
		this.impl = impl;
	}

	private void initModules() {

	}

	public HashMap<Integer, Entity> getEntities() {
		return entities;
	}

	public HashMap<Integer, UserInfo> getUsersByEntity() {
		return usersByEntity;
	}

	public HashMap<Integer, UserInfo> getUsersByID() {
		return usersByID;
	}

	public ClassesManager getClassManager() {
		return classManager;
	}

	public EventHandler getEventHandler() {
		return evHandler;
	}

	public PacketManager getPacketManager() {
		return pm;
	}

	public void setThePlayer(UserInfo thePlayer) {
		this.thePlayer = thePlayer;
		System.out.println("Player: " + thePlayer.getName() + ": " + thePlayer.getEntityID());
	}

	public UserInfo getThePlayer() {
		return thePlayer;
	}
}
