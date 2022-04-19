package de.olfillasodikno.xtc.dump;

import java.util.ArrayList;

import com.github.jonatino.process.Module;

public class PatternScanner {

	public static long fromPattern(Module module, byte[] old, boolean[] mask, long patternOffset, boolean read) {
		int maxOffset = module.size() - old.length;
		for (int i = 0; i < maxOffset; i++) {
			if (matchesMask(module, i, old, mask)) {
				if (read) {
					return Integer.toUnsignedLong(module.data().getInt(i + patternOffset));
				}
				return i + patternOffset;
			}
		}
		return -1;
	}

	private static boolean matchesMask(Module module, long offset, byte[] old, boolean[] mask) {
		for (int i = 0; i < old.length; i++) {
			if (mask[i] && module.data().getByte(offset + i) != old[i]) {
				return false;
			}
		}
		return true;
	}

	public static boolean[] convMask(String str) {
		boolean[] mask = new boolean[str.length()];
		for (int i = 0; i < str.length(); i++) {
			mask[i] = str.charAt(i) != '?';
		}
		return mask;
	}

	static final ArrayList<Character> hex_chars = new ArrayList<>();
	static {
		for (char c : new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' }) {
			hex_chars.add(c);
		}
	}

	public static byte[] convSignature(String str) {
		StringBuilder sb = new StringBuilder();
		for (char c : str.toUpperCase().toCharArray()) {
			if (hex_chars.contains(c)) {
				sb.append(c);
			}
		}
		return hexStringToByteArray(sb.toString());
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
