package de.olfillasodikno.steamnetworkingsockets.crypto;

import java.security.Security;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import de.olfillasodikno.bitutils.BitReader;
import de.olfillasodikno.steamnetworkingsockets.crypto.Test.DumpResult;

public class Decoder {
	private static DumpResult result;
	private static DecryptionContext ctx;
	static {
		Security.setProperty("crypto.policy", "unlimited");
	}
	
	public static byte[] decode(byte[] raw_data, int length)
	{
		
		
		
		if(result == null) {
			result = Test.aesKey();
			initCrypto();
		}
		BitReader br = new BitReader(raw_data);
		byte flag = (byte)br.readUBitInt(8);
		short seqNum = (short)br.readUBitInt(16);
		int toConnectionID = (int)br.readUBitInt(32);
		if((flag&8)!=0) {
			br.setPos(br.getPos()+2*8);
		}
		
		if((flag&1)!=0) {
			int stats_length = br.readVarInt();
			int p = br.getPos()/8;
			if(p+stats_length>length) {
				System.out.println("WTF");
				return null;
			}
			br.setPos(br.getPos()+stats_length*8);
		}
		
		if((flag&2)!=0) {
			br.setPos(br.getPos()+2*8);
		}

		byte[] encrypted_chunk = new byte[length-br.getPos()*8];
		br.readByteArray(encrypted_chunk, 0, encrypted_chunk.length*8);
		System.out.println(Integer.toHexString(encrypted_chunk.length));
		try {
			byte[] iv = new byte[16];
			System.arraycopy(result.getIv(), 0, iv, 0, 16);
			iv[0]=(byte) (seqNum&0xff);
			iv[1]=(byte) ((seqNum>>8)&0xff);
			//byte[] decrypted = ctx.decrypt(encrypted_chunk, iv, 0, null);
			//System.out.println(new HexBinaryAdapter().marshal(decrypted));
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return null;
	}

	private static void initCrypto() {
		ctx = new DecryptionContext();
		ctx.init(result.getKey(), 16, 128);	
	}
}
