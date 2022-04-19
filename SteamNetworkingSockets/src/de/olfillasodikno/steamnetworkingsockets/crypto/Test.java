package de.olfillasodikno.steamnetworkingsockets.crypto;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.github.jonatino.process.Module;
import com.github.jonatino.process.Process;
import com.github.jonatino.process.Processes;

public class Test {

	public static void main(String[] args) {
//		Security.setProperty("crypto.policy", "unlimited");
//		DecryptionContext ctx = new DecryptionContext();
//		byte[] key = new byte[32];
//		SecureRandom rnd = new SecureRandom();
//		rnd.nextBytes(key);
//		ctx.init(key, 12, 128);
//		byte[] iv = new byte[12];
//		rnd.nextBytes(iv);
//		try {
//			byte[] encrypted = ctx.encrypt("HelloWorld!".getBytes(), iv, 0, null);
//			System.out.println(new String(encrypted));
//			byte[] decrypted = ctx.decrypt(encrypted, iv, 0, null);
//			System.out.println(new String(decrypted));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		aesKey();
	}

	// TODO: Grab the aes key and iv from memory

	private static Process process;
	private static Module steamnetworkingsockets;

	public static DumpResult aesKey() {
		waitUntilFound("process", () -> (process = Processes.byName("csgo.exe")) != null);
		waitUntilFound("server module",
				() -> (steamnetworkingsockets = process.findModule("steamnetworkingsockets.dll")) != null);

		byte[] key = new byte[32];
		byte[] iv = new byte[16];
		boolean[] mask_key = PatternScanner.convMask("xxxx");
		byte[] signature_key = PatternScanner.convSignature("\\x6a\\x01\\x6a\\x20");
		long offset = PatternScanner.fromPattern(steamnetworkingsockets, signature_key, mask_key, 0, false);
		long offsetKey = steamnetworkingsockets.readInt(offset + 6);
		long offsetIV = steamnetworkingsockets.readInt(offset + 12);
		System.out.println("Key Offset: " + Long.toHexString(offsetKey));
		System.out.println("IV Offset: " + Long.toHexString(offsetIV));

		boolean[] mask_listConnections = PatternScanner.convMask("xxxxxx????xxxxxx");
		byte[] signature_listConnections = PatternScanner
				.convSignature("\\x83\\xF8\\xFF\\x75\\x39\\xA1\\x00\\x00\\x00\\x00\\x83\\xF8\\xFF\\x74\\x41\\x8B");

		long mask_listConnections_ptr = PatternScanner.fromPattern(steamnetworkingsockets, signature_listConnections,
				mask_listConnections, 17, true);
		System.out.println("ConnectionList: " + Long.toHexString(mask_listConnections_ptr));
		long connectionBase_ptr = process.readInt(mask_listConnections_ptr);
		System.out.println("ConnectionBasePtr: " + Long.toHexString(connectionBase_ptr));
		long connectionBase = Integer.toUnsignedLong(process.readInt(connectionBase_ptr));
		System.out.println("ConnectionBase: " + Long.toHexString(connectionBase));

		if (connectionBase == 0) {
			System.out.println("No Connection found!");
			return null;
		}
		if (offsetKey == -1) {
			System.out.println("Key not found");
			return null;
		}
		if (offsetIV == -1) {
			System.out.println("IV not found");
			return null;
		}
		for (int i = 0; i < key.length; i++) {
			key[i] = (byte) process.readByte(connectionBase + offsetKey + i);
		}
		for (int i = 0; i < iv.length; i++) {
			iv[i] = (byte) process.readByte(connectionBase + offsetIV + i);
		}
		System.out.println("Key: " + new HexBinaryAdapter().marshal(key));
		System.out.println("IV: " + new HexBinaryAdapter().marshal(iv));

		return new DumpResult(key, iv);
	}

	private static void waitUntilFound(String message, Clause clause) {
		System.out.print("Looking for " + message + ". Please wait.");
		while (!clause.get())
			try {
				Thread.sleep(3000);
				System.out.print(".");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		System.out.println("\nFound " + message + "!");
	}

	@FunctionalInterface
	private interface Clause {
		boolean get();
	}

	public static class DumpResult {
		private byte[] key;
		private byte[] iv;

		public DumpResult(byte[] key, byte[] iv) {
			this.key = key;
			this.iv = iv;
		}
		
		public byte[] getKey() {
			return key;
		}
		public byte[] getIv() {
			return iv;
		}
	}

}
