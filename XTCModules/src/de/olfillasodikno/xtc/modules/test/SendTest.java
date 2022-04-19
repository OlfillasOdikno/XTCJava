package de.olfillasodikno.xtc.modules.test;

import java.util.ArrayList;

import com.valve.csgo.proto.UserMessages.CCSUsrMsg_SayText2;
import com.valve.csgo.proto.UserMessages.ECstrike15UserMessages;

import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.networking.handlers.UserMessageHandler;

public class SendTest {

	public ArrayList<String> messages = new ArrayList<>();

	public SendTest() {
		CoreManager.INSTANCE.getEventHandler().registerListener(this);
		CoreManager.INSTANCE.getPacketManager().getInChannel().getMsgMgr().registerHandler(this);
		System.out.println("registered test");
	}

	@UserMessageHandler(ECstrike15UserMessages.CS_UM_SayText2_VALUE)
	public byte[] handlePacketEntities(CCSUsrMsg_SayText2 packet, NetChannel from) {
//		packet.getParamsList().forEach(t -> {
//			System.out.println("FUCK THIS: "+t);
//		});
		return null;
	}



}
