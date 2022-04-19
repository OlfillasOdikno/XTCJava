package de.olfillasodikno.xtc;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.olfillasodikno.xtc.manager.CoreManager;

public class PacketParser implements SniffImpl {

	public PacketParser() throws IOException {
		File in = new File("packet_dump");
		if (!in.exists()) {
			System.err.println("Failed to load packet_dump");
			return;
		}

		new CoreManager().setImpl(this);

		Loader load = new Loader(new FileInputStream(in));
		byte[] data;
		while ((data = load.get()) != null) {
			CoreManager.INSTANCE.getPacketManager().decodeAndProcessPacket(data, data.length, false);
		}
	}

	public static void main(String[] args) throws IOException {
		new PacketParser();
	}

	private class Loader {
		private final DataInputStream is;

		public Loader(InputStream is) {
			this.is = new DataInputStream(is);
		}

		public byte[] get() {
			try {
				int l = is.readInt();
				byte[] data = new byte[l];
				is.read(data);
				return data;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	@Override
	public boolean isPassive() {
		return true;
	}

	@Override
	public boolean canSendToServer() {
		return false;
	}

	@Override
	public boolean canSendToClient() {
		return false;
	}

	@Override
	public void sendToServer(byte[] data) {
	}

	@Override
	public void sendToClient(byte[] data) {
	}

	@Override
	public void stop() {
		
	}
}
