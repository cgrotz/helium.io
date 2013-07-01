package de.skiptag.roadrunner.disruptor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.processor.authorization.AuthorizationProcessor;
import de.skiptag.roadrunner.disruptor.processor.distribution.DistributionProcessor;
import de.skiptag.roadrunner.disruptor.processor.eventsourcing.EventSourceProcessor;
import de.skiptag.roadrunner.disruptor.processor.storage.StorageProcessor;

public class DisruptorRoadrunnerService {
    private static final int RING_SIZE = 256;
    private EventSourceProcessor eventSourceProcessor;
    private StorageProcessor storageProcessor;
    private DistributionProcessor distributionProcessor;
    private Disruptor<RoadrunnerEvent> disruptor;
    private AuthorizationProcessor authorizationProcessor;

    @SuppressWarnings("unchecked")
    public DisruptorRoadrunnerService(File journalDirectory,
	    DataService dataService, AuthorizationService authorizationService,
	    boolean withDistribution) throws IOException, JSONException {
	ExecutorService executor = Executors.newCachedThreadPool();
	disruptor = new Disruptor<RoadrunnerEvent>(
		RoadrunnerEvent.EVENT_FACTORY, RING_SIZE, executor);

	authorizationProcessor = new AuthorizationProcessor(
		authorizationService);
	eventSourceProcessor = new EventSourceProcessor(journalDirectory, this);
	storageProcessor = new StorageProcessor(dataService);
	distributionProcessor = new DistributionProcessor(dataService);

	EventHandlerGroup<RoadrunnerEvent> ehg = disruptor.handleEventsWith(authorizationProcessor)
		.then(eventSourceProcessor)
		.then(storageProcessor);
	if (withDistribution) {
	    ehg.then(distributionProcessor);
	}
	disruptor.start();

	// eventSourceProcessor.restore();
    }

    public void handleEvent(final RoadrunnerEvent roadrunnerEvent) {
	disruptor.publishEvent(new RoadrunnerEventTranslator(roadrunnerEvent));
    }
}
