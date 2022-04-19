package de.olfillasodikno.xtc.manager;

import static de.olfillasodikno.xtc.networking.Protocol.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.xtc.IceKey;
import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.networking.NetPacket;
import de.olfillasodikno.xtc.networking.NetPacket.NetAddr;
import de.olfillasodikno.xtc.networking.NetPacket.NetAddrType;
import de.olfillasodikno.xtc.util.lzss.LZSS;

public class PacketManager {

	private NetChannel inChannel = new NetChannel();
	private NetChannel outChannel = new NetChannel();
	{
		outChannel.setOut(true);
	}

	private byte[] splitBuf = null;
	private int expectedSplitSize = -1;
	private int splitCount = -1;
	private int currentSequence = -1;
	private int totalSize = -1;
	public NetPacket decodeAndProcessPacket(byte[] payload, int length, boolean isFromClient) {
		return decodeAndProcessPacket(payload,length,isFromClient,true);
	}

	public NetPacket decodeAndProcessPacket(byte[] payload, int length, boolean isFromClient,boolean encrypted) {

		NetPacket packet = new NetPacket();
		NetAddr from = new NetAddr();
		from.type = NetAddrType.NA_IP;
		packet.from = from;
		byte[] work = new byte[length];
		System.arraycopy(payload, 0, work, 0, length);
		packet.data = work;
		packet.wiresize = length;
		packet.size = length;
		packet.message = ByteBuffer.wrap(work);
		packet.message.order(ByteOrder.LITTLE_ENDIAN);
		int p = packet.message.getInt();
		if (p == NET_HEADER_FLAG_SPLITPACKET) {
			int sequenceNumber = packet.message.getInt();
			int total = packet.message.get();
			int num = packet.message.get();
			int splitSize = packet.message.getShort();

			if (currentSequence == -1 || currentSequence != sequenceNumber) {
				expectedSplitSize = splitSize;
				splitBuf = new byte[expectedSplitSize * total];
				splitCount = total;
				currentSequence = sequenceNumber;
			}

			if (splitBuf == null) {
				return null;
			}
			if (num == total - 1) {
				totalSize = (total - 1) * splitSize + packet.message.remaining();
			}
			splitCount--;

			packet.message.get(splitBuf, num * splitSize, packet.message.remaining());
			if (splitCount <= 0) {
				if (totalSize < 0) {
					System.out.println(totalSize);
				}
				currentSequence = -1;
				packet.data = new byte[totalSize];
				System.arraycopy(splitBuf, 0, packet.data, 0, totalSize);
				packet.size = totalSize;
				packet.wiresize = totalSize;
				packet.message = ByteBuffer.wrap(packet.data);
				packet.message.order(ByteOrder.LITTLE_ENDIAN);
			} else {
				return null;
			}
		}

		packet.message.position(0);
		if(encrypted) {
			packet.message = handleEncryption(packet.message, false);			
		}
		packet.message_bits = new BitReader(packet.message.array());
		packet.data = packet.message.array();

		byte[] old = new byte[packet.data.length];
		System.arraycopy(packet.data, 0, old, 0, packet.data.length);

		int offset = packet.message.get() & 0xFF;
		int size = 0;
		int mode = 0;
		if (offset + 1 > packet.size) {
			return null;
		}
		packet.setPosition(offset + 1);
		if (offset + 5 < packet.size) {

			packet.message.order(ByteOrder.BIG_ENDIAN);
			size = packet.message.getInt();
			packet.message.order(ByteOrder.LITTLE_ENDIAN);

			if (offset + size + 5 == packet.size) {
				packet.size = size;
				byte[] data = new byte[size];
				packet.message.get(data);

				packet.data = data;
				packet.message = ByteBuffer.wrap(packet.data);
				packet.message.order(ByteOrder.LITTLE_ENDIAN);
				packet.message_bits = new BitReader(packet.data);

				mode = packet.message.getInt();
				if (mode == NET_HEADER_FLAG_COMPRESSEDPACKET) {
					packet.syncToByteBuffer();
					int pos = packet.message_bits.getPos();
					int magic = packet.message_bits.readUBitInt(32);

					if (magic == 0x53535a4c) { // magic == LZSS
						packet.message_bits.setPos(pos);
						byte[] unData = LZSS.unCompress(packet.message_bits);
						if (unData == null) {
							System.err.println("LOL");
							return null;
						}
						packet.data = unData;
						packet.message = ByteBuffer.wrap(packet.data);
						packet.message.order(ByteOrder.LITTLE_ENDIAN);
						packet.message_bits = new BitReader(packet.data);
					}
				}
				packet.setPosition(0);
			} else {
				return null;
			}

		}else {
			return null;
		}
		packet.setPosition(0);
		if (isFromClient) {
			//outChannel.processPacket(packet);
		} else {
			inChannel.processPacket(packet);
		}
		return packet;
	}

	public static int genChecksum(ByteBuffer buf) {
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

	private static ByteBuffer handleEncryption(ByteBuffer buf, boolean encrypt) {
		if (CoreManager.INSTANCE.getClassManager().getIceKey() == null) {
			return buf;
		}

		int pos = buf.position();

		int i;

		int bytesLeft = buf.remaining();
		IceKey key = new IceKey(2);
		key.set(CoreManager.INSTANCE.getClassManager().getIceKey());
		byte[] bufferIn = new byte[8];
		byte[] bufferOut = new byte[8];

		i = 0;
		while (bytesLeft >= key.blockSize()) {
			buf.get(bufferIn);
			if (encrypt) {
				key.encrypt(bufferIn, bufferOut);
			} else {
				key.decrypt(bufferIn, bufferOut);
			}
			for (int j = 0; j < key.blockSize(); j++) {
				buf.put(i + j, bufferOut[j]);
			}
			i += key.blockSize();
			bytesLeft -= key.blockSize();
		}
		buf.position(pos);
		return buf;
	}

	public static boolean bufferDecompress(byte[] dst, int dstLen, byte[] source, int sourceLen) {
		BitReader br = new BitReader(source);
		int magic = br.readUBitInt(32);

		if (magic == 0x53535a4c) { // magic == LZSS
			int size = br.readUBitInt(32);

			br.setPos(0);
			byte[] data = LZSS.unCompress(br);
			if (data == null) {
				System.err.println("Failes to decompress LZSS!");
				return false;
			}
			System.arraycopy(data, 0, dst, 0, dstLen);
			if (dstLen != size) {
				System.err.println("!Invaild Size");
				return false;
			}
			return true;

		}
		return false;
	}

	private static Random rnd = new Random();

	static int outSplitSequenceNumber = -1;

	public static List<byte[]> getSendData(byte[] toSend, boolean compress) {
		List<byte[]> ret = new ArrayList<>();
		if (compress) {
			System.err.println("LZSS compression not implemented yet");
		}

		byte[] sendData = genSendData(toSend);

		if (sendData.length > MAX_ROUTABLE_PAYLOAD) {
			// splitPacket
			short splitSizeMinusHeader = MAX_ROUTABLE_PAYLOAD - 12; // MAXSize(1260)-splitheaderSize(4+4+1+1+2)
			byte packetCount = (byte) ((toSend.length + splitSizeMinusHeader - 1) / splitSizeMinusHeader);

			outSplitSequenceNumber++;
			ByteBuffer packetBuf = ByteBuffer.allocate(MAX_ROUTABLE_PAYLOAD);
			packetBuf.putInt(NET_HEADER_FLAG_SPLITPACKET);
			packetBuf.putInt(outSplitSequenceNumber);
			packetBuf.put(packetCount);
			int startPos = packetBuf.position();
			packetBuf.position(startPos + 1);
			packetBuf.putShort(splitSizeMinusHeader);

			int left = toSend.length;
			byte packetNumber = 0;
			while (left > 0) {
				int size = Math.min(splitSizeMinusHeader, left);
				packetBuf.mark();
				packetBuf.position(startPos);
				packetBuf.put(packetNumber);
				packetBuf.reset();
				packetBuf.put(toSend, packetNumber * splitSizeMinusHeader, size);

				byte[] splitData = new byte[size + 12];
				System.arraycopy(packetBuf.array(), 0, splitData, 0, size + 12);
				ret.add(splitData);

				left -= size;
				packetNumber++;
			}
		} else {
			ret.add(toSend);
		}

		return ret;
	}

	public NetChannel getInChannel() {
		return inChannel;
	}

	public NetChannel getOutChannel() {
		return outChannel;
	}

	public static byte[] genSendData(byte[] toSend) {
		int rndFac = rnd.nextInt(16 + 72) + 16;
		int rndBytes = 8 - (toSend.length + rndFac + 5) % 8 + rndFac;
		int fullLength = rndBytes + 5 + toSend.length;
		ByteBuffer buf = ByteBuffer.allocate(fullLength);
		buf.put((byte) rndBytes);
		for (int i = 0; i < rndBytes; i++) {
			buf.put((byte) (rnd.nextInt(16 + 250) + 16));
		}

		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(toSend.length);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		buf.put(toSend);

		buf.position(0);
		handleEncryption(buf, true);
		return buf.array();
	}
}
