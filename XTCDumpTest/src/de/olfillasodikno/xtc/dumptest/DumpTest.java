package de.olfillasodikno.xtc.dumptest;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.xtc.SniffImpl;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.networking.NetPacket;
import de.olfillasodikno.xtc.util.lzss.LZSS;

public class DumpTest implements SniffImpl {
	public DumpTest() {
		new CoreManager().setImpl(this);;
	
		NetChannel channel = new NetChannel();

		File file = new File("dump3.dmp");
		try {
			byte[] data = Files.readAllBytes(file.toPath());
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			while (buf.hasRemaining()) {
				int length = buf.getInt();
				// System.out.println(length);
				byte[] packetData = new byte[length];
				buf.get(packetData);
				NetPacket packet = new NetPacket();

				packet.data = packetData;
				packet.wiresize = length;
				packet.size = length;
				packet.message = ByteBuffer.wrap(packetData);
				packet.message.order(ByteOrder.LITTLE_ENDIAN);
				packet.message_bits = new BitReader(packetData);

				int offset = packet.message.get() & 0xFF;
				if (offset + 1 > packet.size) {
					// System.out.println("Oversized");
					// TODO: Oversized
					continue;
				}
				packet.setPosition(offset + 1);
				if (offset + 5 < packet.size) {

					packet.message.order(ByteOrder.BIG_ENDIAN);
					int size = packet.message.getInt();
					packet.message.order(ByteOrder.LITTLE_ENDIAN);

					if (offset + size + 5 == packet.size) {
						packet.size = size;
						byte[] data1 = new byte[size];
						packet.message.get(data1);

						packet.data = data1;
						packet.message = ByteBuffer.wrap(packet.data);
						packet.message.order(ByteOrder.LITTLE_ENDIAN);
						packet.message_bits = new BitReader(packet.data);

						int m = packet.message.getInt();
						if (m == -3) {
							packet.syncToByteBuffer();
							int pos = packet.message_bits.getPos();
							int magic = packet.message_bits.readUBitInt(32);

							if (magic == 0x53535a4c) { // magic == LZSS
								packet.message_bits.setPos(pos);
								byte[] unData = LZSS.unCompress(packet.message_bits);
								packet.data = unData;
								packet.message = ByteBuffer.wrap(packet.data);
								packet.message.order(ByteOrder.LITTLE_ENDIAN);
								packet.message_bits = new BitReader(packet.data);
							}
						}
						packet.setPosition(0);
					} else {

					}

				}

				packet.setPosition(0);
				channel.processPacket(packet);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new DumpTest();
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
		// TODO Auto-generated method stub

	}

	@Override
	public void sendToClient(byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
