package net.simpleframework.workflow.engine.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.permission.PermissionConst;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.ctx.script.ScriptEvalUtils;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.participant.IParticipantHandler.AbstractParticipantHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ParticipantRoleHandler extends AbstractParticipantHandler {

	@Override
	public Collection<Participant> getParticipants(final IScriptEval script,
			final ActivityComplete activityComplete, final Map<String, Object> variables) {
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		final String participant = ScriptEvalUtils.replaceExpr(script, getParticipantType(variables)
				.getParticipant());
		final ID roleId = permission.getRole(participant).getId();
		final Iterator<ID> users = permission.users(roleId, null, variables);
		while (users.hasNext()) {
			final ID _roleId = (ID) variables.get(PermissionConst.CTX_ROLEID);
			final ID _deptId = (ID) variables.get(PermissionConst.CTX_DEPTID);
			participants.add(new Participant(users.next(), _roleId != null ? _roleId : roleId,
					_deptId != null ? _deptId : null));
		}
		return participants;
	}
}