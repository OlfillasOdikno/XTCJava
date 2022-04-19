package de.olfillasodikno.xtc.util.lzss;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.bitutils.BitWriter;

public class LZSS {

	private static final int MAGIC = 0x53535a4c;
	private static final int LZSS_LOOKSHIFT = 4;
	private static final int LZSS_LOOKAHEAD = (1 << LZSS_LOOKSHIFT);

	private static int getSize(BitReader br) {
		int pos = br.getPos();
		int size = -1;
		if (br.readUBitInt(32) == MAGIC) {
			size = br.readUBitInt(32);
		}
		br.setPos(pos);
		return size;
	}

	public static byte[] unCompress(BitReader br) {
		int size = getSize(br);
		if (size == -1) {
			return null;
		}

		byte[] out = new byte[size];
		int cmd = 0;

		br.setPos(br.getPos() + 2 * 32);
		int splitPos = 0;

		for (byte iterator = 0;; iterator = (byte) ((iterator + 1) % 8)) {
			if (splitPos >= size) {
				break;
			}
			if (iterator == 0) {
				cmd = br.readUBitInt(8); // read cmd bit for the next 8
			}
			if ((cmd & 0x01) != 0) {
				int a = br.readUBitInt(8);
				int b = br.readUBitInt(8);
				// bitw aaaaaaaabbbbcccc (16)
				// offest aaaaaaaabbbb (12)
				// length cccc (4)
				int offset = a << LZSS_LOOKSHIFT | (b >>> LZSS_LOOKSHIFT);
				int length = (b & 0x0F) + 1;

				if (length == 1) {
					break;
				}
				int start = splitPos - offset - 1;
				for (int i = 0; i < length; i++) {
					out[splitPos++] = out[start++];
				}
			} else {
				out[splitPos++] = (byte) br.readUBitInt(8);
			}
			cmd = cmd >>> 1;
		}

		return out;
	}

	public static byte[] compress(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(data);
		int maxOffset = 1 << 12;
		byte[] lookBuf = new byte[LZSS_LOOKAHEAD];
		ArrayList<Token> tokens = new ArrayList<>();
		int compSize = 0;
		while (buf.hasRemaining()) {
			int start = buf.position();
			byte b = buf.get();
			int offset = -1;
			int length = 1;
			for (int j = 0; j < Math.min(start, maxOffset); j++) {
				buf.position(start - j - 1);
				if (buf.get() == b) {
					int tempOffset = j;
					for (int i = 1; i < LZSS_LOOKAHEAD; i++) {
						buf.position(start + i);
						if (!buf.hasRemaining()) {
							break;
						}
						lookBuf[i] = buf.get();
						buf.position(start - tempOffset + i - 1);
						if (lookBuf[i] != buf.get()) {
							if (i > length) {
								offset = tempOffset;
								length = i;
							}
							break;
						}
					}
				}
			}
			offset++;
			buf.position(start + length);
			Token t = new Token();
			if (offset > 0) {
				t.match = true;
				t.offset = offset;
				t.length = length;
				compSize += 2;
			} else {
				t.match = false;
				t.b = b;
				compSize += 1;
			}
			if (tokens.size() % 8 == 0) {
				compSize++;
			}
			tokens.add(t);
		}
		byte[] dest = new byte[compSize + 8];
		fromTokens(tokens, dest, 8);
		dest[0] = 'L';
		dest[1] = 'Z';
		dest[2] = 'S';
		dest[3] = 'S';

		BitWriter bw = new BitWriter();
		bw.writeUBitInt(data.length, 32);
		System.arraycopy(bw.toByteArray(), 0, dest, 4, 4);
		return dest;
	}

	private static void fromTokens(ArrayList<Token> tokens, byte[] dest, int offset) {
		int idx = offset;
		int lastCmd = 0;
		for (int i = 0; i < tokens.size(); i++) {
			if (i % 8 == 0) {
				lastCmd = idx++;
			}
			Token t = tokens.get(i);
			dest[lastCmd] |= (t.match ? 1 : 0) << i % 8;
			if (t.match) {
				// bitw aaaaaaaabbbbcccc (16)
				// offest aaaaaaaabbbb (12)
				// length cccc (4)
				int o = t.offset - 1;
				byte a = (byte) (o >>> LZSS_LOOKSHIFT);
				byte b = (byte) (((o << LZSS_LOOKSHIFT) & 0xFF) | ((t.length - 1) & 0x0f));
				dest[idx++] = a;
				dest[idx++] = b;
			} else {
				dest[idx++] = t.b;
			}
		}
	}

	private static class Token {
		boolean match;
		int offset;
		int length;
		byte b;

		@Override
		public String toString() {
			if (match) {
				return "(1, " + offset + ", " + length + ")";
			}
			return "(0, " + (char) b + ")";
		}
	}

}
