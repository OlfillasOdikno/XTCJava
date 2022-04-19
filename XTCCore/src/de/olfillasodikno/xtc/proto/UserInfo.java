package de.olfillasodikno.xtc.proto;

public class UserInfo {

	private long version;
	private long xuid;

	private String name;
	private int userId;

	private String guid;
	
	private int friendsID;
	private String friendsName;
	
	private boolean fakePlayer;
	
	private boolean hlTV;
	
	private int entityID;

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getXuid() {
		return xuid;
	}

	public void setXuid(long xuid) {
		this.xuid = xuid;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getFriendsID() {
		return friendsID;
	}

	public void setFriendsID(int friendsID) {
		this.friendsID = friendsID;
	}

	public boolean isFakePlayer() {
		return fakePlayer;
	}

	public void setFakePlayer(boolean fakePlayer) {
		this.fakePlayer = fakePlayer;
	}

	public boolean isHlTV() {
		return hlTV;
	}

	public void setHlTV(boolean hlTV) {
		this.hlTV = hlTV;
	}

	public int getEntityID() {
		return entityID;
	}

	public void setEntityID(int entityID) {
		this.entityID = entityID;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getFriendsName() {
		return friendsName;
	}

	public void setFriendsName(String friendsName) {
		this.friendsName = friendsName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("----------UserInfo----------\n");
		sb.append("Name:\t\t"+getName()+"\n");
		sb.append("GUID:\t\t"+getGuid()+"\n");
		sb.append("UserID:\t\t"+getUserId()+"\n");
		sb.append("EntityID:\t"+getEntityID()+"\n");
		sb.append("----------------------------\n");
		return sb.toString();
	}
}
