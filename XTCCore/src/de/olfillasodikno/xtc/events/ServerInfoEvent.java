package de.olfillasodikno.xtc.events;

public class ServerInfoEvent extends AbstractEvent {
	int protocol;
	int server_count;
	boolean is_dedicated;
	boolean is_official_valve_server;
	boolean is_hltv;
	boolean is_replay;
	boolean is_redirecting_to_proxy_relay;
	int c_os;
	int map_crc;
	int client_crc;
	int String_table_crc;
	int max_clients;
	int max_classes;
	int player_slot;
	float tick_interval;
	String game_dir;
	String map_name;
	String map_group_name; 
	String sky_name; 
	String host_name ; 
	long ugc_map_id;
	public ServerInfoEvent(int protocol, int server_count, boolean is_dedicated, boolean is_official_valve_server,
			boolean is_hltv, boolean is_replay, boolean is_redirecting_to_proxy_relay, int c_os, int map_crc,
			int client_crc, int string_table_crc, int max_clients, int max_classes, int player_slot,
			float tick_interval, String game_dir, String map_name, String map_group_name, String sky_name,
			String host_name, long ugc_map_id) {
		this.protocol = protocol;
		this.server_count = server_count;
		this.is_dedicated = is_dedicated;
		this.is_official_valve_server = is_official_valve_server;
		this.is_hltv = is_hltv;
		this.is_replay = is_replay;
		this.is_redirecting_to_proxy_relay = is_redirecting_to_proxy_relay;
		this.c_os = c_os;
		this.map_crc = map_crc;
		this.client_crc = client_crc;
		String_table_crc = string_table_crc;
		this.max_clients = max_clients;
		this.max_classes = max_classes;
		this.player_slot = player_slot;
		this.tick_interval = tick_interval;
		this.game_dir = game_dir;
		this.map_name = map_name;
		this.map_group_name = map_group_name;
		this.sky_name = sky_name;
		this.host_name = host_name;
		this.ugc_map_id = ugc_map_id;
	}
	public int getProtocol() {
		return protocol;
	}
	public int getServer_count() {
		return server_count;
	}
	public boolean isIs_dedicated() {
		return is_dedicated;
	}
	public boolean isIs_official_valve_server() {
		return is_official_valve_server;
	}
	public boolean isIs_hltv() {
		return is_hltv;
	}
	public boolean isIs_replay() {
		return is_replay;
	}
	public boolean isIs_redirecting_to_proxy_relay() {
		return is_redirecting_to_proxy_relay;
	}
	public int getC_os() {
		return c_os;
	}
	public int getMap_crc() {
		return map_crc;
	}
	public int getClient_crc() {
		return client_crc;
	}
	public int getString_table_crc() {
		return String_table_crc;
	}
	public int getMax_clients() {
		return max_clients;
	}
	public int getMax_classes() {
		return max_classes;
	}
	public int getPlayer_slot() {
		return player_slot;
	}
	public float getTick_interval() {
		return tick_interval;
	}
	public String getGame_dir() {
		return game_dir;
	}
	public String getMap_name() {
		return map_name;
	}
	public String getMap_group_name() {
		return map_group_name;
	}
	public String getSky_name() {
		return sky_name;
	}
	public String getHost_name() {
		return host_name;
	}
	public long getUgc_map_id() {
		return ugc_map_id;
	}
}
