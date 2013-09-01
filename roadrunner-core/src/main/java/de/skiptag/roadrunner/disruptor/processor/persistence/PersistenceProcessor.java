package de.skiptag.roadrunner.disruptor.processor.persistence;

import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.PushAction;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.RemoveAction;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.SetAction;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.SetPriorityAction;
import de.skiptag.roadrunner.disruptor.processor.persistence.actions.UpdateAction;
import de.skiptag.roadrunner.persistence.Persistence;

public class PersistenceProcessor implements EventHandler<RoadrunnerEvent> {

	private PushAction				pushAction;

	private SetAction					setAction;

	private RemoveAction			removeAction;

	private SetPriorityAction	setPriorityAction;

	private UpdateAction			updateAction;

	public PersistenceProcessor(Persistence persistence) {
		pushAction = new PushAction(persistence);
		updateAction = new UpdateAction(persistence);
		setAction = new SetAction(persistence);
		removeAction = new RemoveAction(persistence);
		setPriorityAction = new SetPriorityAction(persistence);
	}

	@Override
	public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch) {
		switch (event.getType()) {
		case PUSH:
			pushAction.handle(event);
			break;
		case UPDATE:
			updateAction.handle(event);
			break;
		case SET:
			setAction.handle(event);
			break;
		case REMOVE:
			removeAction.handle(event);
			break;
		case SETPRIORITY:
			setPriorityAction.handle(event);
			break;
		default:
			break;
		}
	}
}
