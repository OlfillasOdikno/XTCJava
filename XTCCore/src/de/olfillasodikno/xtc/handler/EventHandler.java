package de.olfillasodikno.xtc.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import de.olfillasodikno.xtc.events.AbstractEvent;
import de.olfillasodikno.xtc.events.Event;

public class EventHandler {

	public final Map<Class<? extends AbstractEvent>, CopyOnWriteArrayList<Listener>> listenermap;
	private final Map<Object, ArrayList<Listener>> eventmap;

	public EventHandler() {
		this.listenermap = new HashMap<>();
		this.eventmap = new HashMap<>();
		AbstractEvent.setHandler(this);
	}

	public void onEvent(AbstractEvent event) {
		CopyOnWriteArrayList<Listener> listeners = listenermap.get(event.getClass());
		if (listeners == null) {
			return;
		}
		for (Listener listener : listeners) {
			listener.invoke(event);
		}
	}

	public void unregisterListener(Object o) {
		ArrayList<Listener> listeners = eventmap.remove(o);
		if (listeners == null) {
			System.err.println("Listener not registered!");
			return;
		}
		for (Listener listener : listeners) {
			listenermap.get(listener.evClass).remove(listener);
		}
	}

	public void registerListener(Object o) {
		for (Method method : o.getClass().getMethods()) {
			Event ev = method.getAnnotation(Event.class);
			if (ev == null || method.getParameterTypes().length != 1
					|| method.getParameterTypes()[0].getSuperclass() != AbstractEvent.class) {
				continue;
			}
			Class<?> parameter = method.getParameterTypes()[0];
			Class<? extends AbstractEvent> evClass = parameter.asSubclass(AbstractEvent.class);
			if (!listenermap.containsKey(parameter)) {
				listenermap.put(evClass, new CopyOnWriteArrayList<>());
			}
			Listener listener = new Listener(o, method, evClass);
			listenermap.get(evClass).add(listener);
			if (!eventmap.containsKey(o)) {
				eventmap.put(o, new ArrayList<>());
			}
			eventmap.get(o).add(listener);
		}
	}

	private static class Listener {
		private Object instance;
		private Method method;
		private Class<? extends AbstractEvent> evClass;

		public Listener(Object instance, Method method, Class<? extends AbstractEvent> evClass) {
			this.instance = instance;
			this.method = method;
			this.evClass = evClass;
		}

		public void invoke(AbstractEvent ev) {
			try {
				method.setAccessible(true);
				method.invoke(instance, ev);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}