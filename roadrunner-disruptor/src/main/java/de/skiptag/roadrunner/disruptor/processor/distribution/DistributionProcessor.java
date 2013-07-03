package de.skiptag.roadrunner.disruptor.processor.distribution;

import org.json.JSONObject;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.Path;
import de.skiptag.roadrunner.disruptor.event.MessageType;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class DistributionProcessor implements EventHandler<RoadrunnerEvent> {

	private DataService dataService;

	public DistributionProcessor(DataService dataService) {
		this.dataService = dataService;
	}

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		Path path = new Path(event.extractNodePath());
		MessageType type = event.getType();
		JSONObject node = dataService.get(path.toString());
		if (type == MessageType.PUSH) {

			dataService.fireChildAdded((String) event.get("name"),
					path.toString() + "/" + event.get("name"), path.getParent()
							.getLastElement(), node, null, false, 0);
		} else if (type == MessageType.SET) {
			if (event.has("payload") && !event.isNull("payload")) {
				dataService.fireChildChanged(path.getLastElement(),
						path.toString(), path.getParent().getLastElement(),
						node, null, false, 0);
			} else {
				dataService.fireChildRemoved(path.toString(), event.getOldValue());
			}
		}
	}
}
