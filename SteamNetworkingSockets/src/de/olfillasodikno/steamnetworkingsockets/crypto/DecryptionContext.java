package de.olfillasodikno.steamnetworkingsockets.crypto;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptionContext {
	private Key key;
	private int iv_size;
	private int tag_size;

	public void init(byte[] key, int iv_size, int tag_size) {
		this.key = new SecretKeySpec(key, "AES");
		this.iv_size = iv_size;
		this.tag_size = tag_size;
	}
	
	public byte[] encrypt(byte[] msg, byte[] iv, int iv_offset, byte[] auth_data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec params = new GCMParameterSpec(tag_size, iv, iv_offset, iv_size);
		cipher.init(Cipher.ENCRYPT_MODE, key, params);
		if (auth_data != null) {
			cipher.updateAAD(auth_data);
		}
		return cipher.doFinal(msg);
	}

	public byte[] decrypt(byte[] msg, byte[] iv, int iv_offset, byte[] auth_data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec params = new GCMParameterSpec(tag_size, iv, iv_offset, iv_size);
		cipher.init(Cipher.DECRYPT_MODE, key, params);
		if (auth_data != null) {
			cipher.updateAAD(auth_data);
		}
		return cipher.doFinal(msg);
	}
}
