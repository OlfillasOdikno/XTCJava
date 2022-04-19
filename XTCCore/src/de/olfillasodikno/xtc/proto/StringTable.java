package de.olfillasodikno.xtc.proto;

import java.util.HashMap;

public class StringTable {

	private final String name;
	private final int maxEntries;
	private final int userDataSize;
	private final int userDataSizeBits;
	private final boolean userDataFixedSize;
	
	private final HashMap<Integer, String> names;
	private final HashMap<Integer, byte[]> values;

	public StringTable(String name, int maxEntries, int userDataSize, int userDataSizeBits, boolean userDataFixedSize) {
		this.name = name;
		this.maxEntries = maxEntries;
		this.userDataSize = userDataSize;
		this.userDataSizeBits = userDataSizeBits;
		this.userDataFixedSize = userDataFixedSize;
		names = new HashMap<>();
		values = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	public int getMaxEntries() {
		return maxEntries;
	}

	public int getUserDataSize() {
		return userDataSize;
	}

	public int getUserDataSizeBits() {
		return userDataSizeBits;
	}

	public boolean isUserDataFixedSize() {
		return userDataFixedSize;
	}

	public void set(int idx, String entry, byte[] userdata) {
		names.put(idx, entry);
		values.put(idx, userdata);
	}
	
	public String getName(int idx) {
		return names.get(idx);
	}

}
