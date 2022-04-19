package de.olfillasodikno.xtc.networking;

public class Protocol {

	public static final int PACKET_FLAG_RELIABLE = (1 << 0);
	public static final int PACKET_FLAG_COMPRESSED = (1 << 1);
	public static final int PACKET_FLAG_ENCRYPTED = (1 << 2);
	public static final int PACKET_FLAG_SPLIT = (1 << 3);
	public static final int PACKET_FLAG_CHOKED = (1 << 4);

	public static final int NET_HEADER_FLAG_QUERY = -1;
	public static final int NET_HEADER_FLAG_SPLITPACKET = -2;
	public static final int NET_HEADER_FLAG_COMPRESSEDPACKET = -3;

	public static final int MAX_ROUTABLE_PAYLOAD = 1260;

}
