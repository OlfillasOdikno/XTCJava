package de.olfillasodikno.xtc;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.util.NifSelector;

public class PacketDumper {

	public PacketDumper() throws PcapNativeException, NotOpenException, IOException {
		String prop = System.getProperty("jna.library.path");
		if (prop == null || prop.isEmpty()) {
			prop = "C:/Windows/System32/Npcap";
		} else {
			prop += ";C:/Windows/System32/Npcap";
		}
		System.setProperty("jna.library.path", prop);
		
		File out = new File("packet_dump");
		if (out.exists()) {
			out.delete();
		}
		try {
			out.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Dumper dmp = new Dumper(new FileOutputStream(out));

		// The class that will store the network device
		// we want to use for capturing.
		PcapNetworkInterface device = null;

		// Pcap4j comes with a convenient method for listing
		// and choosing a network interface from the terminal
		try {
			// List the network devices available with a prompt
			device = new NifSelector().selectNetworkInterface();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (device == null) {
			System.out.println("No device chosen.");
			System.exit(1);
		}

		// Open the device and get a handle
		int snapshotLength = 65536; // in bytes
		int readTimeout = 50; // in milliseconds
		PcapHandle handle = device.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);


		PacketListener listener = new PacketListener() {
			long lastTime = System.currentTimeMillis();
			boolean first = true; 
			@Override
			public void gotPacket(Packet packet) {
				if (packet.contains(UdpPacket.class)) {
					if(first) {
						first =false;
						lastTime = System.currentTimeMillis();
					}
					UdpPacket udpPacket = packet.get(UdpPacket.class);
					Packet pl = udpPacket.getPayload();
					dmp.dump(pl.getRawData(), pl.length());
					long time = System.currentTimeMillis();
					if (time - lastTime > 10_000) {
						try {
							handle.breakLoop();
						} catch (NotOpenException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					lastTime = time;
				}
			}
		};
		String filter = "udp port 27015";
		handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
		try {
			handle.loop(-1, listener);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dmp.os.flush();
		dmp.os.close();

		// Cleanup when complete
		handle.close();
	}

	public static void main(String[] args) throws PcapNativeException, NotOpenException, IOException {
		new PacketDumper();
	}

	private class Dumper {
		private final DataOutputStream os;

		public Dumper(OutputStream os) {
			this.os = new DataOutputStream(os);
		}

		public void dump(byte[] data, int length) {
			int l = Math.min(length, data.length);
			try {
				os.writeInt(l);
				os.write(data, 0, l);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
