package de.helium.client;

public interface HeliumCallback {
	public void handle(DataSnapshot data, String prevChildName);
}
