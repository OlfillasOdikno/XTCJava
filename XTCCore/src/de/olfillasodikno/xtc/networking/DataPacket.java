package de.olfillasodikno.xtc.networking;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.CRC32;

import de.olfillasodikno.bitutils.BitWriter;
import de.olfillasodikno.xtc.manager.PacketManager;

public class DataPacket extends NetPacket {

	private int sequence;
	private int sequence_ack;

	private int flags;

	private int checksum;
	private boolean createChecksum = true;

	private int relState;

	private int nChoked;

	private BitWriter buf;

	private boolean isChoked;

	public void encodeHeader() {
		buf = new BitWriter();
		buf.writeUBitInt(sequence, 32);
		buf.writeUBitInt(sequence_ack, 32);

		buf.writeUBitInt(flags, 8);

		if (createChecksum) {
			checksum = genChecksum(message);
		}

		buf.writeUBitInt(checksum, 16);

		buf.writeUBitInt(relState, 8);

		if (isChoked) {
			buf.writeUBitInt(nChoked, 8);
		}

		buf.writeByteArray(message.array());
	}
	
	public void fromNetPacket(NetPacket pk) {
		this.message = pk.message;
		this.message_bits = pk.message_bits;
		this.data = pk.data;
	}

	public List<byte[]> encodeEncryptionStuff() {
		return PacketManager.getSendData(buf.toByteArray(), false);
	}

	public void writeRaw() {
		buf.writeByteArray(message.array());
	}

	private static int genChecksum(ByteBuffer buf) {
		int pos = buf.position();
		int rem = buf.limit() - pos;
		CRC32 c = new CRC32();
		c.update(buf.array(), pos, rem);
		long crc = c.getValue();
		long lowpart = (crc & 0xffff);
		long highpart = ((crc >> 16) & 0xffff);
		buf.position(pos);
		return (int) (lowpart ^ highpart) & 0xFFFF;
	}
}
