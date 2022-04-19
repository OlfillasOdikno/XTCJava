package de.olfillasodikno.bitutils;

import java.nio.ByteBuffer;

public class BitReader implements BitStream {

	private byte currentByte;
	private int bitsAvail;
	private int pos;

	private final ByteBuffer buf;

	public BitReader(byte[] buf) {
		this(ByteBuffer.wrap(buf));
	}

	public BitReader(ByteBuffer buf) {
		this.buf = buf;
		this.pos = buf.position() * 8;
		this.bitsAvail = 0;
	}

	public long readUBitLong(int bits) {
		long ret = 0;
		for (; bits > 0; bits--) {
			ret |= readBit() << (bits - 1);
		}
		return ret;
	}
	
	public long readUBitLongR(int bits) {
		long ret = 0;
		for (int i = 0; i< bits; i++) {
			ret |= readBit() << i;
		}
		return ret;
	}

	public long readSBitLong(int bits) {
		long nRet = readUBitLong(bits);
		return (nRet << (64 - bits)) >> (64 - bits);
	}

	public int readUBitInt(int bits) {
		int ret = 0;
		for (int i = 0; i < bits; i++) {
			ret |= readBit() << i;
		}
		return ret;
	}

	public Object readSBitInt(int bits) {
		int nRet = readUBitInt(bits);
		return (nRet << (32 - bits)) >> (32 - bits);
	}

	public int readUBitVar() {
		int v = readUBitInt(4);
		int type = readUBitInt(2);
		switch (type) {
		case 0:
			return v;
		case 1:
			return readUBitInt(4) << 4 | v;
		case 2:
			return readUBitInt(8) << 4 | v;
		case 3:
			return readUBitInt(28) << 4 | v;
		default:
			break;
		}
		return 0;
	}

	public void setPos(int pos) {
		this.pos = pos;
		int start = Math.floorDiv(pos, 8);
		buf.position(start);
		fetchNextByte();
		if (pos != start * 8) {
			bitsAvail = 8 - (pos % 8);
		}
	}

	public int readBit() {
		if (bitsAvail == 0) {
			fetchNextByte();
		}
		int ret = (currentByte & (1 << (8 - bitsAvail))) == 0 ? 0 : 1;
		pos++;
		bitsAvail--;
		return ret;
	}

	public int readBitR() {
		if (bitsAvail == 0) {
			fetchNextByte();
		}
		int ret = (currentByte & (1 << (bitsAvail - 1))) == 0 ? 0 : 1;
		pos++;
		bitsAvail--;
		return ret;
	}

	public int readVarInt() {
		int ret = 0;
		int count = 0;
		int b;

		do {
			if (count == 5) {
				return ret;
			}
			b = readUBitInt(8);
			ret |= (b & 0x7F) << (7 * count);
			++count;
		} while ((b & 0x80) != 0);

		return ret;
	}

	public long readVarLong() {
		long ret = 0;
		int count = 0;
		long b;

		do {
			if (count == 10) {
				return ret;
			}
			b = readUBitInt(8);
			ret |= (b & 0x7F) << (7 * count);
			++count;
		} while ((b & 0x80) != 0);

		return ret;
	}

	public int readSignedVarInt() {
		int ret = readVarInt();
		return (ret >> 1) ^ -(ret & 1);
	}

	public int readSignedVarLong() {
		int ret = readVarInt();
		return (ret >> 1) ^ -(ret & 1);
	}

	public void readByteArray(byte[] dest, int offset, int bits) {
		if (bitsAvail % 8 == 0) {
			if (bits < 8) {
				for (int i = 0; i < bits; i++) {
					dest[offset] |= readBit() << i % 8;
				}
				return;
			}
			if (bitsAvail == 8) {
				dest[offset] = currentByte;
			} else {
				dest[offset] = buf.get();
			}
			int l = bits / 8 - 1;
			buf.get(dest, offset + 1, l);
			pos = buf.position() * 8;
			bitsAvail = 0;
			if (bits % 8 != 0) {
				for (int i = 0; i < bits % 8; i++) {
					dest[offset + l + 1] |= readBit() << i % 8;
				}
			}
			return;
		} else {
			for (int i = 0; i < bits; i++) {
				dest[offset + i / 8] |= readBit() << i % 8;
			}
			return;
		}

	}

	public String readString(int max) {
		int bits = max * 8;
		byte[] dest = new byte[max];
		if (bitsAvail % 8 == 0) {
			if (bits < 8) {
				for (int i = 0; i < bits; i++) {
					dest[0] |= readBit() << i % 8;
				}
				return new String(dest, 0, 1);
			}
			if (bitsAvail == 8) {
				dest[0] = currentByte;
			} else {
				dest[0] = buf.get();
			}
			pos = buf.position() * 8;
			bitsAvail = 0;
			if (dest[0] == 0) {
				return "";
			}
			int l = bits / 8 - 1;
			for (int i = 0; i < l; i++) {
				byte b = buf.get();
				if (b == 0) {
					return new String(dest, 0, i + 1);
				}
				dest[i + 1] = b;
			}
			if (bits % 8 != 0) {
				for (int i = 0; i < bits % 8; i++) {
					dest[l + 1] |= readBit() << i % 8;
				}
			}
			return new String(dest, 0, max);
		} else {
			for (int i = 0; i < bits; i++) {
				dest[i / 8] |= readBit() << i % 8;
				if (i % 8 == 7 && dest[i / 8] == 0) {
					return new String(dest, 0, i / 8 + 1);
				}
			}
			return new String(dest, 0, bits / 8 + 1);
		}
	}

	private void fetchNextByte() {
		if (!buf.hasRemaining()) {
			System.err.println("No More Avail");
			new Exception().printStackTrace();
			return;
		}
		currentByte = buf.get();
		bitsAvail = 8;
	}

	@Override
	public int getPos() {
		return pos;
	}

	public int getRemaining() {
		return getCapacity() - getPos();
	}

	@Override
	public int getCapacity() {
		return buf.capacity() * 8;
	}
	
	public byte[] raw() {
		return buf.array();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(getPos()).append("|").append(getCapacity()).append("] ");
		int p = getPos();
		int dt = Math.max(0, p - 32);
		setPos(dt);
		for (int i = 0; i < p - dt; i++) {
			sb.append(readBit());
		}
		sb.append("*");
		dt = Math.min(32, getRemaining());
		for (int i = 0; i < dt; i++) {
			sb.append(readBit());
		}
		setPos(p);
		return sb.toString();
	}
}
