package de.olfillasodikno.xtc.events;

import java.util.List;

import de.olfillasodikno.xtc.proto.Entity;

public class EntityUpdateEvent extends AbstractEvent {
	
	private Entity entity;
	private List<Integer> indices;
	
	public EntityUpdateEvent(Entity ent,List<Integer> indices) {
		this.entity = ent;
		this.indices = indices;
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	public List<Integer> getIndices() {
		return indices;
	}
}
