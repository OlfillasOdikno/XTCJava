package de.olfillasodikno.xtc.modules;

import java.util.ArrayList;

import de.olfillasodikno.xtc.modules.aimbot.AimbotModule;
import de.olfillasodikno.xtc.modules.test.SendTest;

public class ModuleLoader {
	
	private static ArrayList<Class<?>> modules = new ArrayList<>();
	
	public static void loadModules() {
		new AimbotModule();
		new SendTest();
		modules.forEach(c->{
			try {
				c.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public static void addModule(Class<?> c) {
		modules.add(c);
	}
}
