package de.olfillasodikno.xtc;

public interface SniffImpl {
	
	public boolean isPassive();
	
	public boolean canSendToServer();
	public boolean canSendToClient();
	
	public void sendToServer(byte[] data);
	public void sendToClient(byte[] data);
	
	public void stop();
}
