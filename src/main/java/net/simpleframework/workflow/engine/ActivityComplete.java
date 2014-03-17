package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.common.object.ObjectFactory;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.participant.IParticipantHandler;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.UserNode.Role;
import net.simpleframework.workflow.schema.UserNode.RuleRole;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ActivityComplete extends ObjectEx implements Serializable, IWorkflowContextAware {

	private WorkitemComplete workitemComplete;

	private ID activityId;

	private final Map<String, TransitionNode> _transitions;

	private final Map<String, List<Participant>> _participants;
	{
		_transitions = new LinkedHashMap<String, TransitionNode>();
		_participants = new LinkedHashMap<String, List<Participant>>();
	}

	public ActivityComplete(final WorkitemBean workitem) {
		this.workitemComplete = WorkitemComplete.get(workitem);
		doInit(wService.getActivity(workitemComplete.getWorkitem()));
	}

	public ActivityComplete(final ActivityBean activity) {
		doInit(activity);
	}

	/**
	 * 指定的转移构造，用在流程启动
	 * 
	 * @param activity
	 *           开始环节
	 * @param transitions
	 */
	public ActivityComplete(final ActivityBean activity, final List<TransitionNode> transitions) {
		activityId = activity.getId();

		final IScriptEval script = aService.getScriptEval(activity);
		for (final TransitionNode transition : transitions) {
			_transitions.put(transition.getId(), transition);
			putParticipant(transition, script);
		}
	}

	private void doInit(final ActivityBean activity) {
		activityId = activity.getId();

		final AbstractTaskNode tasknode = aService.getTaskNode(activity);
		final IScriptEval script;
		if (workitemComplete != null) {
			script = wService.getScriptEval(workitemComplete.getWorkitem());
			final Map<String, Object> variables = workitemComplete.getVariables();
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				script.putVariable(e.getKey(), e.getValue());
			}
		} else {
			script = aService.getScriptEval(activity);
		}

		// 解析条件正确的transition
		TransitionUtils.doTransitions(tasknode, script, _transitions);

		// 解析jobs
		for (final TransitionNode transition : getTransitions()) {
			putParticipant(transition, script);
		}
	}

	private void putParticipant(final TransitionNode transition, final IScriptEval script) {
		final AbstractTaskNode toTask = transition.to();
		if (!(toTask instanceof UserNode)) {
			return;
		}
		final KVMap variables = new KVMap().add("activityComplete", this).add("transition",
				transition);
		final AbstractParticipantType pt = ((UserNode) toTask).getParticipantType();
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		if (pt instanceof RuleRole) {
			final IParticipantHandler hdl = (IParticipantHandler) ObjectFactory
					.singleton(((UserNode) toTask).getParticipantType().getParticipant());
			final Collection<Participant> _participants = hdl.getParticipants(script, variables);
			if (_participants != null) {
				participants.addAll(_participants);
			}
		} else {
			final IParticipantHandler hdl = ParticipantUtils.getParticipantHandler(pt.getClass());
			Collection<Participant> _participants;
			if (hdl != null && (_participants = hdl.getParticipants(script, variables)) != null) {
				participants.addAll(_participants);
			}
		}

		if (participants.size() > 0) {
			_participants.put(transition.getId(), participants);
		} else {
			throw WorkflowException.of($m("ActivityComplete.0"));
		}
	}

	public WorkitemComplete getWorkitemComplete() {
		return workitemComplete;
	}

	public ActivityBean getActivity() {
		return aService.getBean(activityId);
	}

	public void complete() {
		aService.doComplete(this);
	}

	public boolean isTransitionManual() {
		for (final TransitionNode transition : getTransitions()) {
			if (TransitionUtils.isTransitionManual(transition)) {
				return true;
			}
		}
		return false;
	}

	public boolean isParticipantManual(final String[] transitionIds) {
		Collection<TransitionNode> values;
		if (transitionIds != null) {
			values = new ArrayList<TransitionNode>();
			for (final String id : transitionIds) {
				final TransitionNode transition = getTransitionById(id);
				if (transition != null) {
					values.add(transition);
				}
			}
		} else {
			values = _transitions.values();
		}
		for (final TransitionNode transition : values) {
			if (isParticipantManual(transition.to())) {
				return true;
			}
		}
		return false;
	}

	public boolean isParticipantManual() {
		return isParticipantManual((String[]) null);
	}

	public boolean isParticipantManual(final AbstractTaskNode taskNode) {
		final AbstractParticipantType pt = taskNode instanceof UserNode ? ((UserNode) taskNode)
				.getParticipantType() : null;
		return pt instanceof Role && ((Role) pt).isManual();
	}

	public boolean isParticipantMultiSelected(final AbstractTaskNode taskNode) {
		final AbstractParticipantType pt = taskNode instanceof UserNode ? ((UserNode) taskNode)
				.getParticipantType() : null;
		return pt instanceof Role && ((Role) pt).isMultiSelected();
	}

	public List<TransitionNode> getTransitions() {
		return new ArrayList<TransitionNode>(_transitions.values());
	}

	public List<Participant> getParticipants(final TransitionNode transition) {
		return _participants.get(transition.getId());
	}

	public TransitionNode getTransitionById(final String id) {
		return _transitions.get(id);
	}

	/**
	 * 根据客户端选择重置路由
	 * 
	 * @param transitionIds
	 */
	public void resetTransitions(final String[] transitionIds) {
		TransitionUtils.resetTransitions(transitionIds, _transitions);
	}

	/**
	 * 根据客户端选择重置参与者
	 * 
	 * @param participantIds
	 */
	public void resetParticipants(final Map<String, String[]> participantIds) {
		if (participantIds == null) {
			return;
		}
		final Set<String> keys = participantIds.keySet();
		resetTransitions(keys.toArray(new String[keys.size()]));

		final Map<String, List<Participant>> participants = new LinkedHashMap<String, List<Participant>>();
		for (final TransitionNode transition : getTransitions()) {
			final ArrayList<Participant> al = new ArrayList<Participant>();
			for (final String id : participantIds.get(transition.getId())) {
				for (final Participant participant : getParticipants(transition)) {
					if (id.equals(participant.toString())) {
						al.add(participant);
						break;
					}
				}
			}
			participants.put(transition.getId(), al);
		}
		_participants.clear();
		_participants.putAll(participants);
	}

	private static final long serialVersionUID = -3129141441786781334L;
}
