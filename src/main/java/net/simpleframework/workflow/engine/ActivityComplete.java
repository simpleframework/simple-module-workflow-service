package net.simpleframework.workflow.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.common.object.ObjectFactory;
import net.simpleframework.ctx.permission.PermissionConst;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
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
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ActivityComplete extends ObjectEx implements IWorkflowContextAware {

	private WorkitemBean workitem;

	private ID activityId;

	private final Map<String, TransitionNode> _transitions;

	private final Map<String, List<Participant>> _participants;
	{
		_transitions = new LinkedHashMap<>();
		_participants = new LinkedHashMap<>();
	}

	/* 是否完成当前环节，当前环节可以不完成，而直接创建后续环节 */
	private boolean bcomplete = true;

	ActivityComplete(final WorkitemBean workitem) {
		this.workitem = workitem;
		doInit(wfwService.getActivity(workitem));
	}

	public ActivityComplete(final ActivityBean activity) {
		doInit(activity);
	}

	/**
	 * 指定的转移构造，用在流程启动
	 * 
	 * @param activity
	 *        开始环节
	 * @param transitions
	 */
	public ActivityComplete(final ActivityBean activity, final List<TransitionNode> transitions) {
		activityId = activity.getId();

		final IScriptEval script = wfaService.getScriptEval(activity,
				new KVMap().add("activityComplete", this));
		for (final TransitionNode transition : transitions) {
			_transitions.put(transition.getId(), transition);
			putParticipant(transition, script);
		}
	}

	private void doInit(final ActivityBean activity) {
		activityId = activity.getId();
		reset(activity);
	}

	private void reset(ActivityBean activity) {
		if (activity == null) {
			activity = wfaService.getBean(activityId);
			_transitions.clear();
			_participants.clear();
		}

		if (wfaService.getFallbackNextActivity(activity) != null) {
			return;
		}

		final AbstractTaskNode tasknode = wfaService.getTaskNode(activity);
		final IScriptEval script;
		if (workitem != null) {
			script = wfwService.getScriptEval(workitem,
					new KVMap().add("activityComplete", this).add("activity", activity));
			final Map<String, Object> variables = WorkitemComplete.get(workitem).getVariables();
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				script.putVariable(e.getKey(), e.getValue());
			}
		} else {
			script = wfaService.getScriptEval(activity, new KVMap().add("activityComplete", this));
		}

		// 解析条件正确的transition
		TransitionUtils.doTransitions(tasknode, script, _transitions);
		// 解析jobs
		for (final TransitionNode transition : getTransitions()) {
			putParticipant(transition, script);
		}
	}

	public void reset() {
		reset(null);
	}

	public WorkitemBean getWorkitem() {
		return workitem;
	}

	private void putParticipant(final TransitionNode transition, final IScriptEval script) {
		final AbstractTaskNode toTask = transition.to();
		if (!(toTask instanceof UserNode)) {
			return;
		}

		final KVMap variables = new KVMap().add("transition", transition).add(
				PermissionConst.VAR_USERID,
				wfpService.getBean(getActivity().getProcessId()).getUserId());
		final AbstractParticipantType pt = ((UserNode) toTask).getParticipantType();
		final ArrayList<Participant> participants = new ArrayList<>();
		if (pt instanceof RuleRole) {
			final IParticipantHandler hdl = (IParticipantHandler) ObjectFactory
					.singleton(((UserNode) toTask).getParticipantType().getParticipant());
			final Collection<Participant> _participants = hdl.getParticipants(script, this, variables);
			if (_participants != null) {
				participants.addAll(_participants);
			}
		} else {
			final IParticipantHandler hdl = ParticipantUtils.getParticipantHandler(pt.getClass());
			Collection<Participant> _participants;
			if (hdl != null
					&& (_participants = hdl.getParticipants(script, this, variables)) != null) {
				participants.addAll(_participants);
			}
		}

		if (participants.size() > 0) {
			_participants.put(transition.getId(), participants);
		}
	}

	public ActivityBean getActivity() {
		return wfaService.getBean(activityId);
	}

	public boolean isBcomplete() {
		return bcomplete;
	}

	public ActivityComplete setBcomplete(final boolean bcomplete) {
		this.bcomplete = bcomplete;
		return this;
	}

	public void complete() {
		wfaService.doComplete(this);
	}

	public boolean isTransitionManual() {
		final List<TransitionNode> transitions = getTransitions();
		if (transitions.size() > 1) {
			for (final TransitionNode transition : transitions) {
				if (TransitionUtils.isTransitionManual(transition)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isParticipantManual(final String[] transitionIds) {
		Collection<TransitionNode> values;
		if (transitionIds != null) {
			values = new ArrayList<>();
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
			final List<Participant> l = getParticipants(transition);
			final boolean isParticipantManual = isParticipantManual(transition.to());
			if (!this.bcomplete && isParticipantManual) {
				return true;// hwz070210如果是发送时不需要判断执行者数量
			} else if (isParticipantManual && (l != null && l.size() > 1)) {
				return true;
			}
		}
		return false;
	}

	public boolean isParticipantManual() {
		return isParticipantManual((String[]) null);
	}

	public boolean isParticipantManual(final AbstractTaskNode taskNode) {
		final AbstractParticipantType pt = taskNode instanceof UserNode
				? ((UserNode) taskNode).getParticipantType() : null;
		return pt instanceof Role && ((Role) pt).isManual();
	}

	public boolean isParticipantMultiSelected(final AbstractTaskNode taskNode) {
		final AbstractParticipantType pt = taskNode instanceof UserNode
				? ((UserNode) taskNode).getParticipantType() : null;
		return pt instanceof Role && ((Role) pt).isMultiSelected();
	}

	public List<TransitionNode> getTransitions() {
		final ArrayList<TransitionNode> l = new ArrayList<>(_transitions.values());
		Collections.sort(l, new Comparator<TransitionNode>() {
			@Override
			public int compare(final TransitionNode o1, final TransitionNode o2) {
				final int order1 = o1.getOrder();
				final int order2 = o2.getOrder();
				if (order1 == order2) {
					return 0;
				} else {
					return order1 > order2 ? 1 : -1;
				}
			}
		});
		return l;
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

		final Map<String, List<Participant>> participants = new LinkedHashMap<>();
		for (final TransitionNode transition : getTransitions()) {
			final ArrayList<Participant> al = new ArrayList<>();
			final String[] parr = participantIds.get(transition.getId());
			if (parr != null) {
				for (final String id : parr) {
					for (final Participant participant : getParticipants(transition)) {
						if (id.equals(participant.toString())) {
							al.add(participant);
							break;
						}
					}
				}
			}
			if (al.size() > 0) {
				participants.put(transition.getId(), al);
			}
		}
		_participants.clear();
		_participants.putAll(participants);

		final Set<String> keys = _participants.keySet();
		resetTransitions(keys.toArray(new String[keys.size()]));
	}
}
