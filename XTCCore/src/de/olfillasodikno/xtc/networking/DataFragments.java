package de.olfillasodikno.xtc.networking;

public class DataFragments {
	public static final int MAX_OSPATH = 260;
	public static final int FRAGMENT_SIZE = 0;

	public long file_handle; // open file handle
	public String filename; // filename
	public byte[] buffer = null; // if NULL it's a file

	public int bytes; // size in bytes

	public int bits; // size in bits

	public int transferID; // only for files
	public boolean isCompressed; // true if data is bzip compressed

	public int nUncompressedSize; // full size in bytes
	public boolean asTCP; // send as TCP stream
	public int numFragments; // number of total fragments
	public int ackedFragments; // number of fragments send & acknowledged
	public int pendingFragments; // number of fragments send, but not acknowledged yet
}
