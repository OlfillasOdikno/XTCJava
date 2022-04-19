package de.olfillasodikno.xtc.events;


import de.olfillasodikno.xtc.proto.Entity;

public class EntityCreateEvent extends AbstractEvent {
	
	private Entity entity;
	
	public EntityCreateEvent(Entity ent) {
		this.entity = ent;
	}
	
	public Entity getEntity() {
		return entity;
	}
	

}
