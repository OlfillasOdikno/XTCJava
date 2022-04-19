package de.olfillasodikno.bitutils;

import java.io.ByteArrayOutputStream;

public class BitWriter implements BitStream {

	private byte currentByte;
	private int bitsRemaining;
	private int pos;

	private final ByteArrayOutputStream bos;

	public BitWriter() {
		bos = new ByteArrayOutputStream();
		pos = 0;
		bitsRemaining = 8;
		currentByte = 0;
	}

	public void writeBit(int v) {
		if (v != 0) {
			currentByte |= (1 << 8 - bitsRemaining);
		}
		bitsRemaining--;
		pos++;
		if (bitsRemaining <= 0) {
			nextByte();
		}
	}

	public void writeBitRev(int v) {
		if (v != 0) {
			currentByte |= (1 << bitsRemaining - 1);
		}
		bitsRemaining--;
		pos++;
		if (bitsRemaining <= 0) {
			nextByte();
		}
	}

	public void writeUBitInt(int v, int bits) {
		for (int i = 0; i < bits; i++) {
			writeBit(v & (1 << i));
		}
	}

	public void writeUBitIntRev(int v, int bits) {
		for (int i = 0; i < bits; i++) {
			writeBitRev(v & (1 << i));
		}
	}

	public void writeBitVar(int value) {
		int a = value & 15;
		writeUBitInt(a, 4);
		if ((value | 0xf) == 0xf) {
			writeUBitInt(0, 2);
		} else {
			if ((value | 0xff) == 0xff) {
				writeUBitInt(1, 2);
				writeUBitInt(value>>>4, 4);
			} else {
				if ((value | 0xfff) == 0xfff) {
					writeUBitInt(2, 2);
					writeUBitInt(value>>>4, 8);
				} else {
					writeUBitInt(3, 2);
					writeUBitInt(value>>>4, 28);
				}
			}
		}
	}

	public void writeVarInt(int data) {
		int length = 0;
		for (int i = 31; i >= 0; i--) {
			if ((data & (1 << i)) != 0) {
				length = i + 1;
				break;
			}
		}

		int m = 1 << 7;
		int c = (length - 1) / 7;
		for (int i = 0; i < c; i++) {
			int w = (data >> 7 * i) & 0x7F | m;
			writeUBitInt(w, 8);
		}
		int w = (data >> 7 * c) & 0x7F;
		writeUBitInt(w, 8);
	}
	
	public void writeVarLong(long data) {
		int length = 0;
		for (int i = 63; i >= 0; i--) {
			if ((data & (1 << i)) != 0) {
				length = i + 1;
				break;
			}
		}

		int m = 1 << 7;
		int c = (length - 1) / 7;
		for (int i = 0; i < c; i++) {
			int w = (int) ((data >> 7 * i) & 0x7F | m);
			writeUBitInt(w, 8);
		}
		int w = (int) ((data >> 7 * c) & 0x7F);
		writeUBitInt(w, 8);
	}
	
	public void writeSignedVarInt(int value) {
		int ret =  (value << 1) ^ (value >> 31);
		writeVarInt(ret);
	}
	
	public void writeSignedVarLong(long value) {
		long ret =  (value << 1) ^ (value >> 63);
		writeVarLong(ret);
	}

	private void nextByte() {
		bos.write(currentByte);
		currentByte = 0;
		bitsRemaining = 8;
	}

	@Override
	public int getPos() {
		return pos;
	}

	@Override
	public int getCapacity() {
		return -1;
	}

	public byte[] toByteArray() {
		byte[] ret;
		if (bitsRemaining % 8 != 0) {
			ret = new byte[bos.size() + 1];
			System.arraycopy(bos.toByteArray(), 0, ret, 0, bos.size());
			ret[ret.length - 1] = currentByte;
		} else {
			ret = bos.toByteArray();
		}
		return ret;
	}

	public void writeByteArray(byte[] data) {
		for (byte b : data) {
			for (int i = 8; i > 0; i--) {
				writeBit(b & 0xff & (1 << (i - 1)));
			}
		}
	}
}
