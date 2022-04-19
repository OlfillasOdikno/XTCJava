package de.olfillasodikno.xtc.manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.olfillasodikno.xtc.networking.NetChannel;
import de.olfillasodikno.xtc.networking.handlers.SVCMessageHandler;
import de.olfillasodikno.xtc.networking.handlers.UserMessageHandler;

public class MessageManager {

	private final HashMap<Integer, CopyOnWriteArrayList<Handler>> userHandlerMap;
	private final HashMap<Integer, CopyOnWriteArrayList<Handler>> svcHandlerMap;
	private final HashMap<Object, ArrayList<Integer>> registerSVCMap;
	private final HashMap<Object, ArrayList<Integer>> registerUserMap;

	public MessageManager() {
		svcHandlerMap = new HashMap<>();
		userHandlerMap = new HashMap<>();
		registerSVCMap = new HashMap<>();
		registerUserMap = new HashMap<>();
	}

	public byte[] onSVCMessage(int cmd, byte[] data, NetChannel from) {
		if (!svcHandlerMap.containsKey(cmd)) {
			return null;
		}
		CopyOnWriteArrayList<Handler> handlers = svcHandlerMap.get(cmd);
		if (handlers == null) {
			return null;
		}
		byte[] ret = null;
		for (Handler h : handlers) {
			byte[] hRet = h.invoke(data, from);
			if (hRet != null) {
				ret = hRet;
			}
		}
		return ret;
	}
	
	public byte[] onUserMessage(int cmd, byte[] data, NetChannel from) {
		if (!userHandlerMap.containsKey(cmd)) {
			return null;
		}
		CopyOnWriteArrayList<Handler> handlers = userHandlerMap.get(cmd);
		if (handlers == null) {
			return null;
		}
		byte[] ret = null;
		for (Handler h : handlers) {
			byte[] hRet = h.invoke(data, from);
			if (hRet != null) {
				ret = hRet;
			}
		}
		return ret;
	}

	public void unregisterHandler(Object o) {
		unregisterSVCHandler(o);
		unregisterUserHandler(o);
	}

	public void unregisterSVCHandler(Object o) {
		ArrayList<Integer> commands = registerSVCMap.remove(o);
		if (commands == null) {
			return;
		}
		for (Integer cmd : commands) {
			svcHandlerMap.get(cmd).remove(o);
		}
	}

	public void unregisterUserHandler(Object o) {
		ArrayList<Integer> commands = registerUserMap.remove(o);
		if (commands == null) {
			return;
		}
		for (Integer cmd : commands) {
			userHandlerMap.get(cmd).remove(o);
		}
	}

	public void registerHandler(Object o) {
		for (Method method : o.getClass().getMethods()) {
			registerSVCHandler(method, o);
			registerUserHandler(method, o);
		}
	}

	public void registerSVCHandler(Method method, Object o) {
		SVCMessageHandler ev = method.getAnnotation(SVCMessageHandler.class);
		if (ev == null || method.getParameterTypes().length != 2) {
			return;
		}
		if (!svcHandlerMap.containsKey(ev.value())) {
			svcHandlerMap.put(ev.value(), new CopyOnWriteArrayList<>());
		}
		try {
			Handler handler = new Handler(o, method);
			svcHandlerMap.get(ev.value()).add(handler);
			if (!registerSVCMap.containsKey(o)) {
				registerSVCMap.put(o, new ArrayList<>());
			}
			registerSVCMap.get(o).add(ev.value());
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	public void registerUserHandler(Method method, Object o) {
		UserMessageHandler ev = method.getAnnotation(UserMessageHandler.class);
		if (ev == null || method.getParameterTypes().length != 2) {
			return;
		}
		if (!userHandlerMap.containsKey(ev.value())) {
			userHandlerMap.put(ev.value(), new CopyOnWriteArrayList<>());
		}
		try {
			Handler handler = new Handler(o, method);
			userHandlerMap.get(ev.value()).add(handler);
			if (!registerUserMap.containsKey(o)) {
				registerUserMap.put(o, new ArrayList<>());
			}
			registerUserMap.get(o).add(ev.value());
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	private static class Handler {
		private Object instance;
		private Method method;
		private Method create;

		public Handler(Object instance, Method method) throws NoSuchMethodException, SecurityException {
			this.instance = instance;
			this.method = method;
			Class<?> csvcClass = method.getParameterTypes()[0];
			create = csvcClass.getMethod("parseFrom", byte[].class);
		}

		public byte[] invoke(byte[] data, NetChannel from) {
			try {
				method.setAccessible(true);
				Object ret = method.invoke(instance, create.invoke(null, data), from);
				if (ret != null && ret instanceof byte[]) {
					return (byte[]) ret;
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

}
