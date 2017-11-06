package net.simpleframework.workflow.engine.participant;

import java.util.HashMap;

import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.UserNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class ParticipantUtils {

	public static IParticipantHandler getParticipantHandler(
			final Class<? extends AbstractParticipantType> typeClass) {
		return handlerCache.get(typeClass);
	}

	private static HashMap<Class<? extends AbstractParticipantType>, IParticipantHandler> handlerCache;
	static {
		handlerCache = new HashMap<>();
		handlerCache.put(AbstractParticipantType.User.class, new ParticipantUserHandler());
		handlerCache.put(UserNode.Role.class, new ParticipantRoleHandler());
		handlerCache.put(UserNode.RelativeRole.class, new ParticipantRelativeRoleHandler());
	}
}
