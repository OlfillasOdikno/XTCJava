package de.olfillasodikno.xtc.proto;

public class EntityInfo {

	private boolean asDelta;
	
	private UpdateType updateType;

	private int oldEntity;
	private int newEntity;

	private int headerBase;
	private int headerCount;

	public boolean isAsDelta() {
		return asDelta;
	}

	public void setAsDelta(boolean asDelta) {
		this.asDelta = asDelta;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}

	public void setUpdateType(UpdateType updateType) {
		this.updateType = updateType;
	}

	public int getOldEntity() {
		return oldEntity;
	}

	public void setOldEntity(int oldEntity) {
		this.oldEntity = oldEntity;
	}

	public int getNewEntity() {
		return newEntity;
	}

	public void setNewEntity(int newEntity) {
		this.newEntity = newEntity;
	}

	public int getHeaderBase() {
		return headerBase;
	}

	public void setHeaderBase(int headerBase) {
		this.headerBase = headerBase;
	}

	public int getHeaderCount() {
		return headerCount;
	}

	public void setHeaderCount(int headerCount) {
		this.headerCount = headerCount;
	}
	


	public enum UpdateType {
		EnterPVS, LeavePVS, DeltaEnt, PreserveEnt, Finished;
	}
}
