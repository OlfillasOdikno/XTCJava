package de.olfillasodikno.xtc.events;

import de.olfillasodikno.xtc.handler.EventHandler;

public abstract class AbstractEvent {
	
	private boolean canceled;
	
	private static EventHandler handler;

	public void fire() {
		if(handler == null) {
			System.err.println("No event handler registered.");
			return;
		}
		handler.onEvent(this);
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void cancel() {
		canceled = true;
	}
	
	public static void setHandler(EventHandler handler) {
		AbstractEvent.handler = handler;
	}

}