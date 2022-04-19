package de.olfillasodikno.xtc.util;

public class BitsUtil {

	public static int swapInt(int i) {
		return (i & 0xff) << 24 | (i & 0xff00) << 8 | (i & 0xff0000) >> 8 | (i >> 24) & 0xff;
	}

	public static short swapShort(short i) {
		return (short) ((i & 0xff) << 8 | (i & 0xff00) >> 8);
	}
}
