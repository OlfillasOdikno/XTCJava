package de.olfillasodikno.xtc.networking.handlers;

import java.util.ArrayList;

import com.valve.source.proto.NetMessages.CSVCMsg_PacketEntities;
import com.valve.source.proto.NetMessages.CSVCMsg_TempEntities;
import com.valve.source.proto.NetMessages.SVC_Messages;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.xtc.ClassesManager;
import de.olfillasodikno.xtc.ClassesManager.SendProp;
import de.olfillasodikno.xtc.ClassesManager.ServerClassInfo;
import de.olfillasodikno.xtc.events.EntityCreateEvent;
import de.olfillasodikno.xtc.events.EntityUpdateEvent;
import de.olfillasodikno.xtc.events.PacketEntityEvent;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.prop.decoder.PropDecoder;
import de.olfillasodikno.xtc.proto.Entity;
import de.olfillasodikno.xtc.proto.EntityInfo.UpdateType;
import de.olfillasodikno.xtc.proto.EntityReadInfo;

public class PacketEntityHandler {

	@SVCMessageHandler(SVC_Messages.svc_PacketEntities_VALUE)
	public void handlePacketEntities(CSVCMsg_PacketEntities packet, NetChannel from) {
		BitReader br = new BitReader(packet.getEntityData().toByteArray());
		EntityReadInfo u = new EntityReadInfo();
		u.setBuf(br);
		u.setAsDelta(packet.getIsDelta());
		u.setHeaderCount(packet.getUpdatedEntries());
		u.setBaseline(packet.getBaseline());
		u.setUpdateBaselines(packet.getUpdateBaseline());

		while (u.getUpdateType() != UpdateType.Finished) {
			u.setHeaderCount(u.getHeaderCount() - 1);
			u.setIsEntity(u.getHeaderCount() >= 0);
			if (u.isEntity()) {
				parseDeltaHeader(u);
			}

			u.setUpdateType(UpdateType.PreserveEnt);

			while (u.getUpdateType() == UpdateType.PreserveEnt) {
				if (determineUpdateType(u)) {
					switch (u.getUpdateType()) {
					case EnterPVS:
						ReadEnterPVS(u, CoreManager.INSTANCE.getClassManager(), from.getServerClassBits());
						break;
					case LeavePVS:
						ReadLeavePVS(u);
						break;
					case DeltaEnt:
						if (!ReadDeltaEnt(u, CoreManager.INSTANCE.getClassManager())) {
							return;
						}
						break;
					case PreserveEnt:
						ReadPreserveEnt(u);
						break;
					default:
						break;
					}
				}
			}
		}
		new PacketEntityEvent().fire();
	}

	private boolean determineUpdateType(EntityReadInfo u) {
		if (!u.isEntity()) {
			u.setUpdateType(UpdateType.Finished);
		} else {
			if ((u.getUpdateFlags() & 4) != 0) {
				u.setUpdateType(UpdateType.EnterPVS);
			} else if ((u.getUpdateFlags() & 1) != 0) {
				u.setUpdateType(UpdateType.LeavePVS);
			} else {
				u.setUpdateType(UpdateType.DeltaEnt);
			}
		}
		return true;
	}

	private void parseDeltaHeader(EntityReadInfo u) {
		u.setUpdateFlags(0);

		u.setNewEntity(u.getHeaderBase() + 1 + u.getBuf().readUBitVar());

		u.setHeaderBase(u.getNewEntity());

		if (u.getBuf().readBit() == 0) {
			if (u.getBuf().readBit() != 0) {
				u.setUpdateFlags(u.getUpdateFlags() | 4);
			}
		} else {
			u.setUpdateFlags(u.getUpdateFlags() | 1);
			if (u.getBuf().readBit() != 0) {
				u.setUpdateFlags(u.getUpdateFlags() | 2);
			}
		}
	}

	private void ReadEnterPVS(EntityReadInfo u, ClassesManager classManager, int serverClassBits) {
		int iClass = u.getBuf().readUBitInt(serverClassBits);
		int iSerialNum = u.getBuf().readUBitInt(10);
		ServerClassInfo info = classManager.getServerClasses().get(iClass);

		ArrayList<SendProp> props = classManager.getFlatTables().get(info.getDatatableName().toLowerCase());
		Entity ent = addEntity(u.getNewEntity(), info);
		ent.setProps(props.toArray(new SendProp[0]));
		readNewEntity(u.getBuf(), info, ent.getState(), classManager, ent);
		ent.setDormant(false);
	}

	private Entity addEntity(int nEntityId, ServerClassInfo info) {
		Entity ent = new Entity();
		ent.setServerClass(info);
		ent.setId(nEntityId);
		CoreManager.INSTANCE.getEntities().put(nEntityId, ent);
		new EntityCreateEvent(ent).fire();
		return ent;
	}

	private void ReadLeavePVS(EntityReadInfo u) {
		Entity e = CoreManager.INSTANCE.getEntities().get(u.getNewEntity());
		if (e != null) {
			CoreManager.INSTANCE.getEntities().remove(u.getNewEntity());
			e.setDormant(true);
		}
	}

	private boolean ReadDeltaEnt(EntityReadInfo u, ClassesManager classManager) {
		Entity e = CoreManager.INSTANCE.getEntities().get(u.getNewEntity());
		if (e != null) {
			readNewEntity(u.getBuf(), e.getServerClass(), e.getState(), classManager, e);
			e.setDormant(false);
		} else {
			System.err.println("Enitity with id: " + u.getNewEntity() + " not found!");
			return false;
		}
		return true;
	}

	@SVCMessageHandler(SVC_Messages.svc_TempEntities_VALUE)
	public void parseTempEntity(CSVCMsg_TempEntities packet, NetChannel from) {
		BitReader br = new BitReader(packet.getEntityData().toByteArray());
		int c = packet.getNumEntries();
		boolean reliable = false;
		if (c == 0) {
			c = 1;
			reliable = true;
		}
		ArrayList<SendProp> props = null;
		ServerClassInfo info = null;
		int iClass = -1;
		for (int i = 0; i < c; i++) {
			if (br.readBit() != 0) {
				br.readUBitInt(8);
			}
			if (br.readBit() != 0) {
				iClass = br.readUBitInt(from.getServerClassBits()) - 1;
				info = CoreManager.INSTANCE.getClassManager().getServerClasses().get(iClass);
				props = CoreManager.INSTANCE.getClassManager().getFlatTables()
						.get(info.getDatatableName().toLowerCase());
			}
			Entity ent = new Entity();
			ent.setServerClass(info);
			if (props == null) {
				System.err.println("Props == NULL");
				return;
			}
			ent.setProps(props.toArray(new SendProp[0]));
			readNewEntity(br, info, ent.getState(), CoreManager.INSTANCE.getClassManager(), ent);
			//ent.setDormant(false);

		}
	}

	private int readNewEntity(BitReader br, ServerClassInfo info, Object[] state, ClassesManager classManager,
			Entity ent) {
		ArrayList<SendProp> props = classManager.getFlatTables().get(info.getDatatableName().toLowerCase());
		boolean newWay = br.readBit() == 1;
		ArrayList<Integer> fieldIndices = new ArrayList<>();
		int index = -1;
		do {
			index = readFieldIndex(br, index, newWay);
			if (index != -1) {
				fieldIndices.add(index);
			}
		} while (index != -1);
		for (int i = 0; i < fieldIndices.size(); i++) {
			int idx = fieldIndices.get(i);
			if (idx < 0 || idx >= props.size()) {
				return -1;
			}
			Object decoded = PropDecoder.decode(props.get(idx), br);
			state[idx] = decoded;
		}
		new EntityUpdateEvent(ent, fieldIndices).fire();
		return fieldIndices.size();
	}

	private int readFieldIndex(BitReader br, int lastIndex, boolean newWay) {
		if (newWay && br.readBit() != 0) {
			return lastIndex + 1;
		}

		int ret = 0;
		if (newWay && br.readBit() != 0) {
			ret = (int) br.readUBitInt(3);
		} else {
			ret = (int) br.readUBitInt(7);
			switch (ret & (32 | 64)) {
			case 32:
				ret = (ret & ~96) | ((int) br.readUBitInt(2) << 5);
				break;

			case 64:
				ret = (ret & ~96) | ((int) br.readUBitInt(4) << 5);
				break;

			case 96:
				ret = (ret & ~96) | ((int) br.readUBitInt(7) << 5);
				break;
			}

		}

		if (ret == 0xFFF) {
			return -1;
		}
		return lastIndex + 1 + ret;
	}

	private void ReadPreserveEnt(EntityReadInfo u) {
		Entity e = CoreManager.INSTANCE.getEntities().get(u.getNewEntity());
		if (e != null) {
			e.setDormant(true);
		}
	}
}
