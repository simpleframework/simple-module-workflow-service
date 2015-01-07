package net.simpleframework.workflow.engine.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.simpleframework.common.StringUtils;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.participant.IParticipantHandler.AbstractParticipantHandler;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.UserNode.ERelativeType;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ParticipantRelativeRoleHandler extends AbstractParticipantHandler {

	@Override
	public Collection<Participant> getParticipants(final IScriptEval script,
			final ActivityComplete activityComplete, final Map<String, Object> variables) {
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		final UserNode.RelativeRole rRole = (UserNode.RelativeRole) getParticipantType(variables);
		final ERelativeType rType = rRole.getRelativeType();
		if (rType == ERelativeType.processInitiator) {
			final ProcessBean process = aService.getProcessBean(activityComplete.getActivity());
			if (StringUtils.hasText(rRole.getRelative())) {
				final Collection<Participant> _participants = permission.getRelativeParticipants(
						process, rRole, variables);
				if (_participants != null) {
					participants.addAll(_participants);
				}
			} else {
				participants.add(new Participant(process.getUserId(), process.getRoleId(), process
						.getDeptId()));
			}
		} else {
			ActivityBean preActivity = null;
			if (rType == ERelativeType.preActivityParticipant) {
				preActivity = activityComplete.getActivity();
			} else if (rType == ERelativeType.preNamedActivityParticipant) {
				preActivity = aService.getPreActivity(activityComplete.getActivity(),
						rRole.getPreActivity());
			}

			if (preActivity != null) {
				final AbstractTaskNode tasknode = aService.getTaskNode(preActivity);
				if (tasknode instanceof UserNode && ((UserNode) tasknode).isEmpty()) {
					participants.addAll(aService.getEmptyParticipants(preActivity));
				} else {
					List<WorkitemBean> completes = null;
					WorkitemBean workitem = activityComplete.getWorkitem();
					if (workitem == null || !workitem.getActivityId().equals(preActivity.getId())) {
						completes = wService.getWorkitems(preActivity, EWorkitemStatus.complete);
						if (completes.size() > 0) {
							workitem = completes.get(0);
						}
					}

					if (workitem != null) {
						if (StringUtils.hasText(rRole.getRelative())) {
							final Collection<Participant> _participants = permission
									.getRelativeParticipants(workitem, rRole, variables);
							if (_participants != null) {
								participants.addAll(_participants);
							}
						} else {
							participants.add(new Participant(workitem.getUserId(), workitem.getRoleId(),
									workitem.getDeptId()));
							// 其它已完成任务项
							if (completes == null) {
								completes = wService.getWorkitems(preActivity, EWorkitemStatus.complete);
							}
							for (final WorkitemBean workitem2 : completes) {
								if (!workitem2.getId().equals(workitem.getId())) {
									participants.add(new Participant(workitem2.getUserId(), workitem2
											.getRoleId(), workitem2.getDeptId()));
								}
							}
						}
					}
				}
			}
		}
		return participants;
	}
}