package de.skiptag.roadrunner.messaging;

import de.skiptag.roadrunner.event.RoadrunnerEvent;

public interface DataListener {
    void distribute(RoadrunnerEvent event);
}
