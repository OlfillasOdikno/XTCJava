package de.olfillasodikno.xtc.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.valve.source.proto.NetMessages.CSVCMsg_GameEvent;

public class GameEvent {

	private final int eventId;
	private final String name;

	private final List<Key> keys;

	public GameEvent(int eventId, String name) {
		this.eventId = eventId;
		this.name = name;
		this.keys = new ArrayList<>();
	}

	public void addKey(Key key) {
		keys.add(key);
	}

	public int getEventId() {
		return eventId;
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> map(List<CSVCMsg_GameEvent.key_t> list) {
		if (list.size() != keys.size()) {
			System.out.println("NONONONO");
			return null;
		}

		HashMap<String, Object> map = new HashMap<>();
		for (int i = 0; i < list.size(); i++) {
			String name = keys.get(i).getName();
			int type = keys.get(i).getType();
			if (type == 1) {
				map.put(name, list.get(i).getValString());
			} else if (type == 2) {
				map.put(name, list.get(i).getValFloat());
			}else if (type == 3) {
				map.put(name, list.get(i).getValLong());
			}else if (type == 4) {
				map.put(name, list.get(i).getValShort());
			}else if (type == 5) {
				map.put(name, list.get(i).getValByte());
			}else if (type == 6) {
				map.put(name, list.get(i).getValBool());
			}else if (type == 7) {
				map.put(name, list.get(i).getValUint64());
			}
		}
		return map;

	}

	public static class Key {
		private final int type;
		private final String name;

		public Key(int type, String name) {
			this.type = type;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}
	}
}
