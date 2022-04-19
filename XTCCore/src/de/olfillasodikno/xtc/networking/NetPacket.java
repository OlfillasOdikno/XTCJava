package de.olfillasodikno.xtc.networking;

import java.nio.ByteBuffer;

import de.olfillasodikno.bitutils.BitReader;

public class NetPacket {
	public NetAddr from; // sender IP
	public int source; // received source
	public double received; // received time
	public byte[] data; // raw packet data
	public ByteBuffer message; // easy bytebuf data access
	public BitReader message_bits; // easy bytebuf data access
	public int size; // size in bytes
	public int wiresize; // size in bytes before decompression
	public boolean stream; // was send as stream
	public NetPacket pNext;

	public void setPosition(int bytePos) {
		message.position(bytePos);
		message_bits.setPos(bytePos * 8);
	}

	public void syncToByteBuffer() {
		message_bits.setPos(message.position() * 8);
	}

	public void syncToBitBuffer() {
		message.position(message_bits.getPos() / 8);
	}

	public static final class NetAddr {
		public NetAddrType type;
		public int ip;
		public short port;
	}

	public enum NetAddrType {
		NA_NULL(0), NA_LOOPBACK(1), NA_BROADCAST(2), NA_IP(3);
		int id;

		private NetAddrType(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}
	
	
}
