package de.olfillasodikno.xtc.events;

import java.util.List;

public class SayTextEvent extends AbstractEvent {

	private List<String> lines;

	public SayTextEvent(List<String> lines) {
		this.lines = lines;
	}

	public List<String> getLines() {
		return lines;
	}

}
