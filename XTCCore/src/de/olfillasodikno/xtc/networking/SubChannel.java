package de.olfillasodikno.xtc.networking;

public class SubChannel {
	public static final int MAX_SUBCHANNELS = 8; // we have 8 alternative send&wait bits
	public static final int MAX_STREAMS = 2;

	public static final int SUBCHANNEL_FREE = 0; // subchannel is free to use
	public static final int SUBCHANNEL_TOSEND = 1;// subchannel has data, but not send yet
	public static final int SUBCHANNEL_WAITING = 2; // sbuchannel sent data, waiting for ACK
	public static final int SUBCHANNEL_DIRTY = 3; // subchannel is marked as dirty during changelevel
	
	public int[] startFraggment = new int[2];
	public int[] numFragments = new int[2];
	public int sendSeqNr;
	public int state; // 0 = free, 1 = scheduled to send, 2 = send & waiting, 3 = dirty
	public int index;

	public void free() {
		state = SUBCHANNEL_FREE;
		sendSeqNr = -1;
		for ( int i = 0; i < 2; i++ )
		{
			numFragments[i] = 0;
			startFraggment[i] = -1;
		}
	}
}
