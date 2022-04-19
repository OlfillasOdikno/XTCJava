import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

import de.olfillasodikno.xtc.IceKey;

public class Test {
	
	public static void main(String[] args) {
		byte[] iceKey = "ABCDEFGHIJKLMNOQ".getBytes();
		String text = "BBCDEFGHIJKLMNOPQR";
		ByteBuffer buf = ByteBuffer.wrap(text.getBytes());
		handleEncryption(buf, iceKey, true);
		byte[] encrypted = new byte[buf.remaining()];
		buf.get(encrypted);
		System.out.println(DatatypeConverter.printHexBinary(encrypted));
	}
	private static ByteBuffer handleEncryption(ByteBuffer buf,byte[] iceKey, boolean encrypt) {

		int pos = buf.position();

		int i;

		int bytesLeft = buf.remaining();
		IceKey key = new IceKey(2);
		key.set(iceKey);
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

}
