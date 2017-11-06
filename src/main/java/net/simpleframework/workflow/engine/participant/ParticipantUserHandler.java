package net.simpleframework.workflow.engine.participant;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.participant.IParticipantHandler.AbstractParticipantHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ParticipantUserHandler extends AbstractParticipantHandler {

	@Override
	public Collection<Participant> getParticipants(final IScriptEval script,
			final ActivityComplete activityComplete, final Map<String, Object> variables) {
		final List<Participant> participants = new ArrayList<>();
		final Object participant = eval(script, getParticipantType(variables).getParticipant());
		if (participant != null) {
			if (participant instanceof Iterable) {
				for (final Object user : (Iterable<?>) participant) {
					addParticipant(participants, user);
				}
			} else if (participant.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(participant); i++) {
					addParticipant(participants, Array.get(participant, i));
				}
			} else {
				addParticipant(participants, participant);
			}
		}
		return participants;
	}

	private void addParticipant(final List<Participant> participants, final Object user) {
		final PermissionUser puser = permission.getUser(user);
		if (puser.exists()) {
			participants.add(new Participant(puser));
		}
	}
}