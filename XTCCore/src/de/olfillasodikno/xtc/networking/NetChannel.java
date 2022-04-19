package de.olfillasodikno.xtc.networking;

import static de.olfillasodikno.xtc.networking.DataFragments.*;
import static de.olfillasodikno.xtc.networking.Protocol.*;
import static de.olfillasodikno.xtc.networking.SubChannel.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.zip.CRC32;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.xtc.manager.MessageManager;
import de.olfillasodikno.xtc.manager.PacketManager;
import de.olfillasodikno.xtc.networking.handlers.GameEventHandler;
import de.olfillasodikno.xtc.networking.handlers.PacketEntityHandler;
import de.olfillasodikno.xtc.networking.handlers.ServerInfoHandler;
import de.olfillasodikno.xtc.networking.handlers.StringTableHandler;
import de.olfillasodikno.xtc.networking.handlers.UserMessageManager;

public class NetChannel {
	private static final int NET_MAX_PALYLOAD_BITS = 26;
	private static final int MAX_FILE_SIZE_BITS = 26;
	private static final int FRAGMENT_BITS = 8;
	private static final int FRAGMENT_SIZE = (1 << FRAGMENT_BITS);

	private int inSequenceNr;
	private int m_PacketDrop;

	private final SubChannel[] m_SubChannels;

	private final Queue<DataFragments>[] m_WaitingList;

	private final DataFragments[] m_ReceiveList;

	private int m_nOutReliableState;
	private int outSequenceNrAck;
	private int m_nInReliableState;

	private int serverClassBits = 8;

	private boolean out;

	public boolean isOut() {
		return out;
	}

	public void setOut(boolean out) {
		this.out = out;
	}

	private MessageManager msgMgr = new MessageManager();
	{
		//msgMgr.registerHandler(new PacketEntityHandler());
		//msgMgr.registerHandler(new GameEventHandler());
		msgMgr.registerHandler(new ServerInfoHandler());
		//msgMgr.registerHandler(new StringTableHandler());
		//msgMgr.registerHandler(new UserMessageManager());
	}

	public MessageManager getMsgMgr() {
		return msgMgr;
	}

	@SuppressWarnings("unchecked")
	public NetChannel() {
		m_SubChannels = new SubChannel[MAX_SUBCHANNELS];
		m_WaitingList = (Queue<DataFragments>[]) new ArrayDeque<?>[MAX_STREAMS];
		m_ReceiveList = new DataFragments[MAX_STREAMS];
		for (int i = 0; i < MAX_SUBCHANNELS; i++) {
			m_SubChannels[i] = new SubChannel();
			m_SubChannels[i].index = i;
		}

		for (int i = 0; i < MAX_STREAMS; i++) {
			m_WaitingList[i] = new ArrayDeque<DataFragments>();
		}

		for (int i = 0; i < MAX_STREAMS; i++) {
			m_ReceiveList[i] = new DataFragments();
		}
	}

	public void processPacket(NetPacket packet) {
		packet.setPosition(0);
		int flags = processPacketHeader(packet);
		if (flags == -1) {
			return;
		}

		packet.syncToByteBuffer();

		if ((flags & PACKET_FLAG_RELIABLE) == 1) {

			int c = packet.message_bits.readUBitInt(3);
			int bit = 1 << c;
			for (int i = 0; i < MAX_STREAMS; i++) {
				if (packet.message_bits.readBit() != 0) {
					if (!ReadSubChannelData(packet.message_bits, i)) {
						return; // error while reading fragments, drop whole packet
					}
				}
			}

			m_nInReliableState = FLIPBIT(m_nInReliableState, bit);

			for (int i = 0; i < MAX_STREAMS; i++) {
				if (!CheckReceivingList(i))
					return; // error while processing
			}
		}

		if (packet.message_bits.getRemaining() > 0) {
			// parse and handle all messeges
			if (!ProcessMessages(packet.message_bits)) {
				return; // disconnect or error
			}
		}
		return;
	}
	
	public static byte[] shiftLeft(byte[] data, int len) {
	    int word_size = (len / 8) + 1;
	    int shift = len % 8;
	    byte carry_mask = (byte) ((1 << shift) - 1);
	    int offset = word_size - 1;
	    for (int i = 0; i < data.length; i++) {
	        int src_index = i+offset;
	        if (src_index >= data.length) {
	            data[i] = 0;
	        } else {
	            byte src = data[src_index];
	            byte dst = (byte) (src << shift);
	            if (src_index+1 < data.length) {
	                dst |= data[src_index+1] >>> (8-shift) & carry_mask;
	            }
	            data[i] = dst;
	        }
	    }
	    return data;
	}

	public boolean ReadSubChannelData(BitReader msg, int stream) {
		DataFragments data = m_ReceiveList[stream];
		int startFragment = 0;
		int numFragments = 0;
		long offset = 0;
		int length = 0;

		boolean bSingleBlock = msg.readBit() == 0;
		if (!bSingleBlock) {
			startFragment = msg.readUBitInt(MAX_FILE_SIZE_BITS - FRAGMENT_BITS);
			numFragments = msg.readUBitInt(3);
			offset = startFragment * FRAGMENT_SIZE;
			length = numFragments * FRAGMENT_SIZE;
		}
		if (offset == 0) // first fragment, read header info
		{
			data.filename = null;
			data.isCompressed = false;
			data.transferID = 0;

			if (bSingleBlock) {
				// data compressed ?
				if (msg.readBit() != 0) {
					data.isCompressed = true;
					data.nUncompressedSize = msg.readUBitInt(MAX_FILE_SIZE_BITS);
				} else {
					data.isCompressed = false;
				}

				data.bytes = msg.readUBitInt(NET_MAX_PALYLOAD_BITS - 7); //I don't known why but -7 works, lol
			} else {
				if (msg.readBit() != 0)// is it a file ?
				{
					data.transferID = msg.readUBitInt(32);
					data.filename = msg.readString(MAX_OSPATH);
				}

				if (msg.readBit() != 0) {// data compressed ?
					data.isCompressed = true;
					data.nUncompressedSize = msg.readUBitInt(MAX_FILE_SIZE_BITS);
				} else {
					data.isCompressed = false;
				}
				data.bytes = msg.readUBitInt(NET_MAX_PALYLOAD_BITS);
			}

			data.bits = data.bytes * 8;
			data.buffer = new byte[PAD_NUMBER(data.bytes, 4)];
			data.asTCP = false;
			data.numFragments = BYTES2FRAGMENTS(data.bytes);
			data.ackedFragments = 0;
			data.file_handle = 0;

			if (bSingleBlock) {
				numFragments = data.numFragments;
				length = numFragments * FRAGMENT_SIZE;
			}
		} else {
			if (data.buffer == null) {
				// This can occur if the packet containing the "header" (offset == 0) is
				// dropped. Since we need the header to arrive we'll just wait
				// for a retry
				return false;
			}
		}

		if ((startFragment + numFragments) == data.numFragments) {

			int rest = FRAGMENT_SIZE - (data.bytes % FRAGMENT_SIZE);
			if (rest < FRAGMENT_SIZE)
				length -= rest;
		}

		byte[] newData = new byte[length];
		if (msg.getRemaining() < length) {
			System.err.println("ERROR");
			return false;
		}
		msg.readByteArray(newData, 0, length * 8);
		System.arraycopy(newData, 0, data.buffer, (int) offset, length);
		data.ackedFragments += numFragments;
		return true;
	}

	public int processPacketHeader(NetPacket packet) {

		int sequence = packet.message.getInt();

		int sequence_ack = packet.message.getInt();

		int flags = packet.message.get() & 0xFF;

		int checksum = packet.message.getShort() & 0xFFFF;

		int cheksum2 = genChecksum(packet.message);

		if (checksum != cheksum2) {
			System.err.println("Inavlid Checksum!");
			return -1;
		}

		int relState = packet.message.get() & 0xFF;

		int nChoked = 0;

		if ((flags & PACKET_FLAG_CHOKED) != 0) {
			nChoked = packet.message.get() & 0xFF;
		}

		packet.syncToByteBuffer();

		if (sequence <= inSequenceNr) {
			//System.out.println("Packet Out of Order");
			return -1;
		}

		m_PacketDrop = sequence - (inSequenceNr + nChoked + 1);

		if (m_PacketDrop > 0) {
			// System.err.println(String.format("Dropped %d packets at %d", m_PacketDrop,
			// sequence));
		}

		for (int i = 0; i < MAX_SUBCHANNELS; i++) {
			int bitmask = (1 << i);
			SubChannel subchan = m_SubChannels[i];
			if ((m_nOutReliableState & bitmask) == (relState & bitmask)) {
				if (subchan.state == SUBCHANNEL_DIRTY) {
					subchan.free();
				} else if (subchan.sendSeqNr > sequence_ack) {
					System.err.println(String.format("reliable state invalid (%d)", i));
					return -1;
				} else if (subchan.state == SUBCHANNEL_WAITING) {
					for (int j = 0; j < MAX_STREAMS; j++) {
						if (subchan.numFragments[j] == 0)
							continue;

						DataFragments data = m_WaitingList[j].peek();

						if (data != null) {
							data.ackedFragments += subchan.numFragments[j];
							data.pendingFragments -= subchan.numFragments[j];
						}
					}

					subchan.free();
				}
			} else {
				if (subchan.sendSeqNr <= sequence_ack) {

					if (subchan.state == SUBCHANNEL_WAITING) {
						System.err.println(String.format("Resending subchan %d: start %d, num %d\n", subchan.index,
								subchan.startFraggment[0], subchan.numFragments[0]));

						subchan.state = SUBCHANNEL_TOSEND;
					} else if (subchan.state == SUBCHANNEL_DIRTY) {
						int bit = 1 << subchan.index;

						m_nOutReliableState = FLIPBIT(m_nOutReliableState, bit);

						subchan.free();
					}
				}
			}
		}

		inSequenceNr = sequence;
		outSequenceNrAck = sequence_ack;

		for (int i = 0; i < MAX_STREAMS; i++)
			CheckWaitingList(i);

		return flags;
	}

	public int getOutSequenceNrAck() {
		return outSequenceNrAck;
	}

	public int getInSequenceNr() {
		return inSequenceNr;
	}

	private void CheckWaitingList(int nList) {
		if (m_WaitingList[nList].size() == 0 || outSequenceNrAck <= 0)
			return;

		DataFragments data = m_WaitingList[nList].peek();
		if (data == null) {
			return;
		}

		if (data.ackedFragments == data.numFragments) {
			System.out
					.println(String.format("Sending complete: %d fragments, %d bytes.", data.numFragments, data.bytes));
			m_WaitingList[nList].poll();
			return;
		} else if (data.ackedFragments > data.numFragments) {

		}

	}

	public boolean CheckReceivingList(int nList) {
		DataFragments data = m_ReceiveList[nList];
		if (data == null) {
			return true;
		}

		if (data.buffer == null || data.ackedFragments < data.numFragments) {
			return true;
		} else if (data.ackedFragments > data.numFragments) {
			System.err.println(String.format("Receiving failed: too many fragments %d/%d", data.ackedFragments,
					data.numFragments));
			return false;
		}
		if (data.isCompressed) {
			UncompressFragments(data);
		}

		if (data.filename == null) {
			BitReader br = new BitReader(data.buffer);
			if (!ProcessMessages(br)) {
				return false;
			}
		}

		data.buffer = null;

		return true;
	}

	public boolean ProcessMessages(BitReader br) {
		int start;
		while (br.getRemaining() >= 32) {
			int v = br.readVarInt();
			int cmd = v & 0xFF;
			int length = br.readVarInt();
			if (length < 0 || length * 8 > br.getRemaining()) {
				// System.out.println("okay, this shouldn't happen, nevermind");
				break;
			}
			start = br.getPos();
			byte[] data = new byte[length];
			br.readByteArray(data, 0, length * 8);
			//System.err.println(cmd);
			if(cmd == 2) {
				System.err.println("FILE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}
			msgMgr.onSVCMessage(cmd, data, this);
			if (start + length * 8 == br.getCapacity()) {
				break;
			}
			br.setPos(start + length * 8);
		}
		return true;
	}

	private void UncompressFragments(DataFragments data) {
		if (!data.isCompressed) {
			return;
		}
		byte[] newBuffer = new byte[PAD_NUMBER(data.nUncompressedSize, 4)];

		boolean ret = PacketManager.bufferDecompress(newBuffer, data.nUncompressedSize, data.buffer, data.bytes);

		if (!ret) {
			System.err.println("Failed to Uncompress Fragments!");
			return;
		}
		data.buffer = newBuffer;
		data.bytes = data.nUncompressedSize;
		data.isCompressed = false;
	}

	public int PAD_NUMBER(int number, int boundary) {
		return (((number) + ((boundary) - 1)) / (boundary)) * (boundary);
	}

	public int FLIPBIT(int in, int num) {
		return (in & num) == 1 ? (in & ~num) : in | num;
	}

	public int BYTES2FRAGMENTS(int i) {
		return ((i + FRAGMENT_SIZE - 1) / FRAGMENT_SIZE);
	}

	private static int genChecksum(ByteBuffer buf) {
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

	public int getServerClassBits() {
		return serverClassBits;
	}

	public void setServerClassBits(int serverClassBits) {
		this.serverClassBits = serverClassBits;
	}

	public static byte[] writeOutHeader(byte[] data, int sequenceNum, int sequenceAck) {
		ByteBuffer buf = ByteBuffer.allocate(12 + data.length);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(sequenceNum);
		buf.putInt(sequenceAck);
		buf.put((byte) 0);

		buf.mark();
		buf.position(buf.position() + 2);
		buf.put((byte) 0xE0);
		buf.put(data);
		buf.reset();
		short checksum = (short) genChecksum(buf);
		buf.reset();
		buf.putShort(checksum);
		return buf.array();
	}
}
