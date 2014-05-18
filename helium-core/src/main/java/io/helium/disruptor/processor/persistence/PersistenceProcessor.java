package io.helium.disruptor.processor.persistence;

import com.lmax.disruptor.EventHandler;

import io.helium.disruptor.processor.persistence.actions.PushAction;
import io.helium.disruptor.processor.persistence.actions.RemoveAction;
import io.helium.disruptor.processor.persistence.actions.SetAction;
import io.helium.disruptor.processor.persistence.actions.SetPriorityAction;
import io.helium.disruptor.processor.persistence.actions.UpdateAction;
import io.helium.event.HeliumEvent;
import io.helium.persistence.Persistence;

public class PersistenceProcessor implements EventHandler<HeliumEvent> {

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
	public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch) {
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
