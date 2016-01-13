package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.ctx.permission.PermissionRole;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.schema.StartNode;
import net.simpleframework.workflow.schema.TransitionNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class InitiateItem extends ObjectEx implements IWorkflowContextAware {

	/* 流程模型id */
	private final ID modelId;

	/* 定义的启动用户 */
	private final ID userId;
	/* 定义的启动角色 */
	private ID roleId;

	/* 实际参与者 */
	private Participant participant;

	/* 传递给流程实例的变量 */
	private final Map<String, Object> variables = new KVMap();

	/* 存放开始节点的手动转移 */
	private final Map<String, TransitionNode> _transitions = new LinkedHashMap<String, TransitionNode>();

	public InitiateItem(final ProcessModelBean processModel, final ID userId, final ID roleId,
			final Map<String, Object> variables) {
		this.modelId = processModel.getId();
		this.userId = userId;
		this.roleId = roleId;

		if (roleId == null) {
			// 如果用户, 则只显示用户角色
			variables.put("userRole", true);
		}
		if (variables != null) {
			this.variables.putAll(variables);
		}
	}

	public ID getModelId() {
		return modelId;
	}

	public ID getUserId() {
		return userId;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	/* 可启动的角色 */
	public List<PermissionRole> roles() {
		final List<PermissionRole> l = permission.getUser(getUserId()).roles(getRoleId(),
				getVariables());
		if (l.size() == 0) {
			throw WorkflowException.of($m("InitiateItem.0"));
		}
		return l;
	}

	public ID getRoleId() {
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
	}

	public Participant getParticipant() {
		if (participant == null) {
			participant = new Participant(getUserId(), getRoleId(), null);
		}
		return participant;
	}

	public boolean isTransitionManual() {
		for (final TransitionNode transition : getTransitions()) {
			if (TransitionUtils.isTransitionManual(transition)) {
				return true;
			}
		}
		return false;
	}

	public List<TransitionNode> getTransitions() {
		return new ArrayList<TransitionNode>(_transitions.values());
	}

	public void resetTransitions(final String[] transitionIds) {
		TransitionUtils.resetTransitions(transitionIds, _transitions);
	}

	public void doTransitions() {
		_transitions.clear();

		final ProcessModelBean processModel = model();
		final IScriptEval script = wfpmService.getScriptEval(processModel, null);
		final StartNode startNode = wfpmService.getProcessDocument(processModel).getProcessNode()
				.startNode();
		for (final Map.Entry<String, Object> e : getVariables().entrySet()) {
			script.putVariable(e.getKey(), e.getValue());
		}
		TransitionUtils.doTransitions(startNode, script, _transitions);
	}

	@Override
	public String toString() {
		return model().toString();
	}

	private transient ProcessModelBean processModel;

	public ProcessModelBean model() {
		if (processModel == null) {
			processModel = wfpmService.getBean(getModelId());
		}
		return processModel;
	}
}
