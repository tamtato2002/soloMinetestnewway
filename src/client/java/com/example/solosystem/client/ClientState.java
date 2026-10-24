package com.example.solosystem.client;

import com.example.solosystem.PlayerData;

public final class ClientState {
	public static PlayerData data = new PlayerData();
	/** client game time at the moment the last sync arrived (quest time is counted down from here) */
	public static long syncGameTime = 0;
	private ClientState() {}
}
