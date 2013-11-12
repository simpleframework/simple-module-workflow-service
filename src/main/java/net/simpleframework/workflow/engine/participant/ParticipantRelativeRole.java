package net.simpleframework.workflow.engine.participant;

import java.util.ArrayList;
import java.util.Collection;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;
import net.simpleframework.workflow.engine.participant.IParticipants.AbstractParticipants;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.UserNode.ERelativeType;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ParticipantRelativeRole extends AbstractParticipants {
	@Override
	public Collection<Participant> participants(final IScriptEval script, final KVMap variables) {
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		final ActivityComplete activityComplete = (ActivityComplete) variables
				.get("activityComplete");
		WorkitemComplete workitemComplete;
		final TransitionNode transition = (TransitionNode) variables.get("transition");
		final net.simpleframework.workflow.schema.UserNode.RelativeRole rRole = (net.simpleframework.workflow.schema.UserNode.RelativeRole) ((UserNode) transition
				.to()).getParticipantType();
		final String relative = rRole.getRelative();
		final ERelativeType rType = rRole.getRelativeType();
		if (rType == ERelativeType.processInitiator) {
			final ProcessBean process = context.getActivityService().getProcessBean(
					activityComplete.getActivity());
			final ID userId = process.getUserId();
			if (userId != null) {
				if (StringUtils.hasText(relative)) {
					final Collection<Participant> _participants = context.getParticipantService()
							.getRelativeParticipants(userId, process.getRoleId(), relative, variables);
					if (_participants != null) {
						participants.addAll(_participants);
					}
				} else {
					participants.add(new Participant(userId, process.getRoleId()));
				}
			}
		} else if ((workitemComplete = activityComplete.getWorkitemComplete()) != null) {
			ActivityBean preActivity = null;
			if (rType == ERelativeType.preActivityParticipant) {
				preActivity = activityComplete.getActivity();
			} else if (rType == ERelativeType.preNamedActivityParticipant) {
			}
			if (preActivity != null) {
				WorkitemBean workitem = workitemComplete.getWorkitem();
				if (StringUtils.hasText(relative)) {
					final Collection<Participant> _participants = context.getParticipantService()
							.getRelativeParticipants(workitem.getUserId(), workitem.getRoleId(), relative,
									variables);
					if (_participants != null) {
						participants.addAll(_participants);
					}
				} else {
					participants.add(new Participant(workitem.getUserId(), workitem.getRoleId()));
					// 其它已完成任务项
					final IDataQuery<WorkitemBean> qs = context.getWorkitemService().getWorkitemList(
							preActivity, EWorkitemStatus.complete);
					while ((workitem = qs.next()) != null) {
						participants.add(new Participant(workitem.getUserId(), workitem.getRoleId()));
					}
				}
			}
		}
		return participants;
	}
}