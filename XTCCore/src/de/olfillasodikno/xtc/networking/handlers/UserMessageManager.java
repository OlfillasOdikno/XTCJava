package de.olfillasodikno.xtc.networking.handlers;

import java.util.ArrayList;

import com.valve.csgo.proto.UserMessages.CCSUsrMsg_SayText2;
import com.valve.csgo.proto.UserMessages.ECstrike15UserMessages;
import com.valve.source.proto.NetMessages.CSVCMsg_UserMessage;
import com.valve.source.proto.NetMessages.SVC_Messages;

import de.olfillasodikno.xtc.events.SayTextEvent;
import de.olfillasodikno.xtc.networking.NetChannel;

public class UserMessageManager {

	@SVCMessageHandler(SVC_Messages.svc_UserMessage_VALUE)
	public byte[] parseUserMessage(CSVCMsg_UserMessage packet, NetChannel from) {
		try {
			ECstrike15UserMessages usermsg = ECstrike15UserMessages.forNumber(packet.getMsgType());
			if (usermsg != null) {
				from.getMsgMgr().onUserMessage(packet.getMsgType(), packet.getMsgData().toByteArray(), from);
				if (usermsg == ECstrike15UserMessages.CS_UM_SayText2) {
					CCSUsrMsg_SayText2 sayText = CCSUsrMsg_SayText2.parseFrom(packet.getMsgData());
					ArrayList<String> lines = new ArrayList<>();
					sayText.getParamsList().forEach(t -> {
						lines.add(t);
					});
					new SayTextEvent(lines).fire();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
