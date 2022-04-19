package de.olfillasodikno.xtc.networking.handlers;

import java.util.HashMap;

import com.valve.source.proto.NetMessages.CSVCMsg_GameEvent;
import com.valve.source.proto.NetMessages.CSVCMsg_GameEventList;
import com.valve.source.proto.NetMessages.SVC_Messages;

import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.proto.GameEvent;
import de.olfillasodikno.xtc.proto.GameEvent.Key;

public class GameEventHandler {
	private HashMap<Integer, GameEvent> events = new HashMap<>();

	@SVCMessageHandler(SVC_Messages.svc_GameEventList_VALUE)
	public void parseGameEventList(CSVCMsg_GameEventList packet, NetChannel from) {
		packet.getDescriptorsList().forEach(d -> {
			GameEvent ev = new GameEvent(d.getEventid(), d.getName());
			events.put(d.getEventid(), ev);
			d.getKeysList().forEach(k -> {
				ev.addKey(new Key(k.getType(), k.getName()));
			});
		});
	}
	
	@SVCMessageHandler(SVC_Messages.svc_GameEvent_VALUE)
	public void parseGameEventList(CSVCMsg_GameEvent packet, NetChannel from) {
		GameEvent ev = events.get(packet.getEventid());
	}

}
