package de.olfillasodikno.xtc;

import java.io.IOException;

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

import de.olfillasodikno.steamnetworkingsockets.crypto.Decoder;
import de.olfillasodikno.xtc.manager.CoreManager;

public class PassiveSniffer implements SniffImpl {

	private PcapHandle handle;
	
	public static void main(String[] args) {
		PcapNetworkInterface nif;
	    try {
	      nif = new NifSelector().selectNetworkInterface();
	    } catch (IOException e) {
	      e.printStackTrace();
	      return;
	    }

	    if (nif == null) {
	      return;
	    }

		PassiveSniffer sniffer = new PassiveSniffer(nif);
		new CoreManager();
		CoreManager.INSTANCE.setImpl(sniffer);
	}
	static int ia =0;
	public PassiveSniffer(PcapNetworkInterface device){
		Thread thread = new Thread(()-> {

			// Open the device and get a handle
			int snapshotLength = 65536; // in bytes
			int readTimeout = 50; // in milliseconds
			try {
				handle = device.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);
			} catch (PcapNativeException e1) {
				e1.printStackTrace();
				return;
			}

			PacketListener listener = new PacketListener() {
				@Override
				public void gotPacket(Packet packet) {
					if (packet.contains(UdpPacket.class)) {
						UdpPacket udpPacket = packet.get(UdpPacket.class);
						Packet pl = udpPacket.getPayload();
						byte[] data = Decoder.decode(pl.getRawData(), pl.length());
						if(data==null) {
							return;
						}
						CoreManager.INSTANCE.getPacketManager().decodeAndProcessPacket(data, data.length, false);
					}
				}
			};
			String filter = "udp src portrange 27000-27030";
			try {
				handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
				handle.loop(-1, listener);
			} catch (PcapNativeException | NotOpenException e) {
				e.printStackTrace();
			}catch (InterruptedException e2) {
				
			}

			// Cleanup when complete
			handle.close();
		});
		thread.setName("Passive Sniffer");
		thread.start();
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
		try {
			if (handle != null) {
				handle.breakLoop();
			}
		} catch (NotOpenException e) {
			//e.printStackTrace();
		}
	}
}
