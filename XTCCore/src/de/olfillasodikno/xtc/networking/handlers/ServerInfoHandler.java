package de.olfillasodikno.xtc.networking.handlers;

import com.valve.source.proto.NetMessages.CSVCMsg_ServerInfo;
import com.valve.source.proto.NetMessages.SVC_Messages;

import de.olfillasodikno.xtc.events.ServerInfoEvent;
import de.olfillasodikno.xtc.networking.NetChannel;
public class ServerInfoHandler {

	@SVCMessageHandler(SVC_Messages.svc_ServerInfo_VALUE)
	public void parseServerInfo(CSVCMsg_ServerInfo packet, NetChannel from) {
		System.out.println("Map: " + packet.getMapName());

		int serverClassBits = Q_log2(packet.getMaxClasses()) + 1;
		System.out.println("ServerClassBits: " + serverClassBits);
		if (!from.isOut()) {
			from.setServerClassBits(9);
		}
		new ServerInfoEvent(packet.getProtocol(), packet.getServerCount(), packet.getIsDedicated(),
				packet.getIsOfficialValveServer(), packet.getIsHltv(), packet.getIsReplay(),
				packet.getIsRedirectingToProxyRelay(), packet.getCOs(), packet.getMapCrc(), packet.getClientCrc(),
				packet.getStringTableCrc(), packet.getMaxClients(), packet.getMaxClasses(), packet.getPlayerSlot(),
				packet.getTickInterval(), packet.getGameDir(), packet.getMapName(), packet.getMapGroupName(),
				packet.getSkyName(), packet.getHostName(), packet.getUgcMapId()).fire();
	}

	private int Q_log2(int val) {
		int answer = 0;
		while ((val >>= 1) != 0)
			answer++;
		return answer;
	}

}
