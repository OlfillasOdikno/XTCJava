package de.olfillasodikno.xtc.networking.handlers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.valve.source.proto.NetMessages.CSVCMsg_CreateStringTable;
import com.valve.source.proto.NetMessages.CSVCMsg_UpdateStringTable;
import com.valve.source.proto.NetMessages.SVC_Messages;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.proto.StringTable;
import de.olfillasodikno.xtc.proto.UserInfo;

public class StringTableHandler {
	private ArrayList<StringTable> tables = new ArrayList<>();

	@SVCMessageHandler(SVC_Messages.svc_CreateStringTable_VALUE)
	public void parseServerInfo(CSVCMsg_CreateStringTable packet, NetChannel from) {
		StringTable st = new StringTable(packet.getName(), packet.getMaxEntries(), packet.getUserDataSize(),
				packet.getUserDataSizeBits(), packet.getUserDataFixedSize());
		parseStringTableUpdate(packet.getStringData(), packet.getNumEntries(), packet.getMaxEntries(),
				packet.getUserDataSizeBits(), packet.getUserDataFixedSize(), packet.getName(), st);
		tables.add(st);
	}

	@SVCMessageHandler(SVC_Messages.svc_UpdateStringTable_VALUE)
	public void parseServerInfo(CSVCMsg_UpdateStringTable packet, NetChannel from) {
		StringTable st = tables.get(packet.getTableId());
		if (st != null) {
			parseStringTableUpdate(packet.getStringData(), packet.getNumChangedEntries(), st.getMaxEntries(),
					st.getUserDataSizeBits(), st.isUserDataFixedSize(), st.getName(), st);
		}
	}

	private void parseStringTableUpdate(ByteString stringData, int numEntries, int maxEntries, int userDataSizeBits,
			boolean userDataFixedSize, String tableName, StringTable st) {
		
		BitReader bs = new BitReader(stringData.toByteArray());

		int lastEntry = -1;
		int lastDictionaryIndex = -1;

		int entryBits = Q_log2(maxEntries);

		boolean isEncodedUsingDictionaries = bs.readBit() == 1;
		if (isEncodedUsingDictionaries) {
			//System.out.println("ParseStringTableUpdate: Encoded with dictionaries, unable to decode.");
			return;
		}

		List<String> history = new ArrayList<>();
		for (int i = 0; i < numEntries; i++) {
			int idx = lastEntry + 1;
			if (bs.readBit() == 0) {
				idx = (int) bs.readUBitLongR(entryBits);
			}
			lastEntry = idx;
			if (idx < 0 || idx > maxEntries) {
				return;
			}
			String entry = "";
			if (bs.readBit() == 1) {
				boolean substringchk = bs.readBit() == 1;
				if (substringchk) {
					int index = bs.readUBitInt(5);
					int bytestocopy = bs.readUBitInt(5);
					entry = history.get(index).substring(0, bytestocopy);
					entry = entry + bs.readString(1024);
				} else {
					entry = bs.readString(1024);
				}
			}

			if (entry == null) {
				entry = "";
			}

			if (history.size() > 31) {
				history.remove(0);
			}

			history.add(entry);
			byte[] userdata = new byte[0];
			if (bs.readBit() == 1) {
				if (userDataFixedSize) {
					userdata = new byte[userDataSizeBits / 8 + 1];
					bs.readByteArray(userdata,0, userDataSizeBits);
				} else {
					int bytes = bs.readUBitInt(14);
					userdata = new byte[bytes];
					bs.readByteArray(userdata, 0,bytes * 8);
				}
			}
			if (userdata.length == 0) {
				continue;
			}
			if (tableName.equals("userinfo")) {
				try {
					ByteBuffer buf = ByteBuffer.wrap(userdata).order(ByteOrder.BIG_ENDIAN);
					UserInfo user = new UserInfo();
					user.setVersion(buf.getLong());
					user.setXuid(buf.getLong());
					user.setName(readString(buf, 128));
					user.setUserId(buf.getInt());
					user.setGuid(readString(buf, 33));
					buf.position(buf.position() + 3);
					user.setFriendsID(buf.getInt());
					user.setFriendsName(readString(buf, 128));
					user.setFakePlayer(buf.get() == 1);
					buf.position(buf.position() + 3);
					user.setHlTV(buf.get() == 1);
					buf.position(buf.position() + 3);
					user.setEntityID(idx + 2);
					if (CoreManager.INSTANCE.getThePlayer() == null) {
						CoreManager.INSTANCE.setThePlayer(user);
					}
					CoreManager.INSTANCE.getUsersByEntity().put(user.getEntityID(), user);
					CoreManager.INSTANCE.getUsersByID().put(user.getUserId(), user);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String readString(ByteBuffer buf, int length) {
		byte[] raw = new byte[length];
		buf.get(raw);
		int i = 0;
		while(raw[i]!=0) {
			i++;
		}	
		return new String(raw,0,i);
	}
	
	private int Q_log2(int val) {
		int answer = 0;
		while ((val >>= 1) != 0)
			answer++;
		return answer;
	}

}
