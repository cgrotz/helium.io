package de.roadrunner.client;

public interface RoadrunnerCallback {
	public void handle(DataSnapshot data, String prevChildName);
}
