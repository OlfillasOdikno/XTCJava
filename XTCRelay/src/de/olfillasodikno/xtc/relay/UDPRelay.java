package de.olfillasodikno.xtc.relay;

import static de.olfillasodikno.xtc.networking.Protocol.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

import de.olfillasodikno.xtc.SniffImpl;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.manager.PacketManager;
import de.olfillasodikno.xtc.networking.NetPacket;

public class UDPRelay implements Runnable, SniffImpl {

	protected DatagramSocket proxyClientSocket;
	protected DatagramSocket proxyServerSocket;

	protected Queue<byte[]> sendToClientQueue;
	protected Queue<byte[]> sendToServerQueue;

	protected Thread proxyClientThread;
	protected Thread proxyServerThread;

	protected boolean running;

	protected InetAddress clientIP;
	protected int clientPort;

	public UDPRelay(InetAddress serverIP, int serverPort) throws SocketException {
		proxyClientSocket = new DatagramSocket(1337);
		proxyServerSocket = new DatagramSocket();

		sendToClientQueue = new LinkedList<>();
		sendToServerQueue = new LinkedList<>();

		proxyServerSocket.connect(serverIP, serverPort);

		proxyClientThread = new Thread(() -> {
			byte[] receiveBuf = new byte[0xFFFF];
			DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
			while (running) {
				try {
					proxyClientSocket.receive(receivePacket);
					clientIP = receivePacket.getAddress();
					clientPort = receivePacket.getPort();
					ByteBuffer buf = ByteBuffer.wrap(receivePacket.getData(), 0, receivePacket.getLength());
					handleFromClient(buf);
					// DatagramPacket sendPacket = new DatagramPacket(receivePacket.getData(),
					// receivePacket.getLength(), serverIP, serverPort);

					// proxyServerSocket.send(sendPacket);

					while (!sendToServerQueue.isEmpty()) {
						byte[] data = sendToServerQueue.poll();
						DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverIP, serverPort);
						proxyServerSocket.send(sendPacket);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				receivePacket.setLength(receiveBuf.length);
			}
			proxyClientSocket.close();
		});

		proxyServerThread = new Thread(() -> {
			byte[] receiveBuf = new byte[0xFFFF];
			DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
			DatagramPacket sendPacket;
			while (running) {
				try {
					proxyServerSocket.receive(receivePacket);
					ByteBuffer buf = ByteBuffer.wrap(receivePacket.getData(), 0, receivePacket.getLength());
					handleFromServer(buf);

					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), clientIP,
							clientPort);
					proxyClientSocket.send(sendPacket);

					// handle queue
					while (!sendToClientQueue.isEmpty()) {
						byte[] data = sendToClientQueue.poll();
						sendPacket = new DatagramPacket(data, data.length, clientIP, clientPort);
						proxyClientSocket.send(sendPacket);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
				receivePacket.setLength(receiveBuf.length);
			}
			proxyServerSocket.close();
		});

	}

	protected boolean handleFromServer(ByteBuffer buf) {
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		// CoreManager.INSTANCE.getPacketManager().decodeAndProcessPacket(data,
		// data.length, false);
		return true;
	}

	public static Random rnd = new Random();

	public boolean shouldflip = false;

	public static byte[] flip(byte[] data) {
		float r = 0.01f;
		int n1 = 1;
		int n = (int) (data.length*0.2f);
		for (int i = 0; i < n; i++) {
			if (rnd.nextFloat() > r) {
				continue;
			}
			int idx = rnd.nextInt(data.length);
			for (int i2 = 0; i2 < n1; i2++) {
				int k = rnd.nextInt(8);
				int p = 1 << k;
				data[idx] = (byte) (data[idx] ^ p);
			}
		}
		return data;
	}

	public static byte[] reheader(ByteBuffer msg) {
		msg.position(0);
		int sequence = msg.getInt();

		int sequence_ack = msg.getInt();

		int flags = msg.get() & 0xFF;

		int checksum = msg.getShort() & 0xFFFF;

		int relState = msg.get() & 0xFF;

		int nChoked = 0;

		if ((flags & PACKET_FLAG_CHOKED) != 0) {
			nChoked = msg.get() & 0xFF;
		}
		msg.mark();
		byte[] d = new byte[msg.remaining()];
		msg.get(d);
		msg.reset();
		msg.put(flip(d));
		msg.position(11);
		int cheksum2 = PacketManager.genChecksum(msg);
		msg.position(9);
		msg.putShort((short)(cheksum2 & 0xFFFF));
		return msg.array();
	}
	
	protected boolean handleFromClient(ByteBuffer buf) {
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		NetPacket r = CoreManager.INSTANCE.getPacketManager().decodeAndProcessPacket(data, data.length, true);
		if (r == null || !shouldflip) {
			sendToServer(data);
		} else {
			// Modify
			byte[] mo = reheader(r.message);
			byte[] data2 = PacketManager.genSendData(mo);

			sendToServer(data2);
		}
		return true;
	}

	@Override
	public void run() {
		running = true;
		proxyClientThread.start();
		proxyServerThread.start();
		Scanner scanner = new Scanner(System.in);
		while (running) {
			System.out.println("Press enter to toggle bit flipping (current state " + shouldflip + "): ");
			scanner.nextLine();
			shouldflip = !shouldflip;
		}
		scanner.close();
	}

	@Override
	public boolean isPassive() {
		return false;
	}

	@Override
	public boolean canSendToServer() {
		return false;
	}

	@Override
	public boolean canSendToClient() {
		return true;
	}

	public void sendToClient(byte[] data) {
		sendToClientQueue.offer(data);
	}

	@Override
	public void sendToServer(byte[] data) {
		sendToServerQueue.offer(data);
	}

	@Override
	public void stop() {
		running = false;
	}

	public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException {
		UDPRelay sniffer = new UDPRelay(InetAddress.getByName("192.168.178.27"), 27015);
		new CoreManager();
		CoreManager.INSTANCE.setImpl(sniffer);
		sniffer.run();
	}
}
