package de.olfillasodikno.xtc.proto;

import de.olfillasodikno.bitutils.BitReader;

public class EntityReadInfo extends EntityInfo {

	private BitReader buf;
	private int updateFlags;

	private boolean isEntity;

	private int baseline;

	private boolean updateBaselines;

	private int localPlayerBits;
	private int otherPlayerBits;

	public BitReader getBuf() {
		return buf;
	}

	public void setBuf(BitReader buf) {
		this.buf = buf;
	}

	public int getUpdateFlags() {
		return updateFlags;
	}

	public void setUpdateFlags(int updateFlags) {
		this.updateFlags = updateFlags;
	}

	public boolean isUpdateBaselines() {
		return updateBaselines;
	}

	public void setUpdateBaselines(boolean updateBaselines) {
		this.updateBaselines = updateBaselines;
	}

	public boolean isEntity() {
		return isEntity;
	}

	public void setIsEntity(boolean isEntity) {
		this.isEntity = isEntity;
	}

	public int getBaseline() {
		return baseline;
	}

	public void setBaseline(int baseline) {
		this.baseline = baseline;
	}

	public int getLocalPlayerBits() {
		return localPlayerBits;
	}

	public void setLocalPlayerBits(int localPlayerBits) {
		this.localPlayerBits = localPlayerBits;
	}

	public int getOtherPlayerBits() {
		return otherPlayerBits;
	}

	public void setOtherPlayerBits(int otherPlayerBits) {
		this.otherPlayerBits = otherPlayerBits;
	}

}
