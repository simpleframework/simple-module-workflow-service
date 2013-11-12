package net.simpleframework.workflow.engine.participant;

import java.util.ArrayList;
import java.util.Collection;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.ctx.script.ScriptEvalUtils;
import net.simpleframework.workflow.engine.participant.IParticipants.AbstractParticipants;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ParticipantUser extends AbstractParticipants {
	@Override
	public Collection<Participant> participants(final IScriptEval script, final KVMap variables) {
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		final TransitionNode transition = (TransitionNode) variables.get("transition");
		final String participant = ScriptEvalUtils.replaceExpr(script, ((UserNode) transition.to())
				.getParticipantType().getParticipant());
		final ID userId = context.getParticipantService().getUser(participant).getId();
		if (userId != null) {
			participants.add(new Participant(userId));
		}
		return participants;
	}
}