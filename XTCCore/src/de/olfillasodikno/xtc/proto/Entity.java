package de.olfillasodikno.xtc.proto;

import de.olfillasodikno.xtc.ClassesManager.SendProp;
import de.olfillasodikno.xtc.ClassesManager.ServerClassInfo;

public class Entity {

	private ServerClassInfo serverClass;

	private int id;
	private int referenceCount;

	private byte[] data;
	private int bits;

	private Object[] state;
	private SendProp[] props;

	private boolean dormant;

	private long lastUpdate;

	public void update() {
		lastUpdate = System.currentTimeMillis();
	}

	public void setDormant(boolean dormant) {
		update();
		this.dormant = dormant;
	}

	public boolean isDormant() {
		return dormant || (System.currentTimeMillis() - lastUpdate) > 1000/5;
	}

	public void setProps(SendProp[] props) {
		this.props = props;
		this.state = new Object[props.length];
	}

	public Object[] getState() {
		return state;
	}

	public SendProp[] getProps() {
		return props;
	}

	public Object getStateByName(String name) {
		for (int i = 0; i < props.length; i++) {
			if (props[i].getVarName().equals(name)) {
				return state[i];
			}
		}
		return null;
	}

	public Object getStateByName(String name, int idx) {
		for (int i = 0; i < props.length; i++) {
			if (props[i].getVarName().equals(name)) {
				if (idx == 0) {
					return state[i];
				}
				idx--;
			}
		}
		return null;
	}

	public boolean hasMultipleStates(String name) {
		int c = 0;
		for (int i = 0; i < props.length; i++) {
			if (props[i].getVarName().equals(name)) {
				c++;
			}
		}
		return c > 1;
	}

	public ServerClassInfo getServerClass() {
		return serverClass;
	}

	public void setServerClass(ServerClassInfo serverClass) {
		this.serverClass = serverClass;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getReferenceCount() {
		return referenceCount;
	}

	public void setReferenceCount(int referenceCount) {
		this.referenceCount = referenceCount;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getBits() {
		return bits;
	}

	public void setBits(int bits) {
		this.bits = bits;
	}

}
