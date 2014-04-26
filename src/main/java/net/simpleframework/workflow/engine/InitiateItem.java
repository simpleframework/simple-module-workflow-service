package net.simpleframework.workflow.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.CollectionUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.schema.StartNode;
import net.simpleframework.workflow.schema.TransitionNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class InitiateItem extends ObjectEx implements IWorkflowServiceAware {

	/* 流程模型id */
	private final ID modelId;

	private final ID userId;

	/* 启动角色 */
	private ID roleId;

	/* 实际启动角色 */
	private ID selectedRoleId;

	/* 传递给流程实例的变量 */
	private final Map<String, Object> variables = new KVMap().add("userRole", true);

	/* 存放开始节点的手动转移 */
	private final Map<String, TransitionNode> _transitions = new LinkedHashMap<String, TransitionNode>();

	public InitiateItem(final ProcessModelBean processModel, final ID userId, final ID roleId,
			final Map<String, Object> variables) {
		this.modelId = processModel.getId();
		this.userId = userId;
		this.roleId = roleId;
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

	/* 其它可启动的角色 */
	public Collection<ID> roles() {
		return CollectionUtils.toList(permission.roles(getUserId(), getVariables()));
	}

	private transient ProcessModelBean processModel;

	public ProcessModelBean model() {
		if (processModel == null) {
			processModel = mService.getBean(getModelId());
		}
		return processModel;
	}

	public ID getRoleId() {
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
	}

	public ID getSelectedRoleId() {
		return selectedRoleId == null ? getRoleId() : selectedRoleId;
	}

	public void setSelectedRoleId(final ID selectedRoleId) {
		this.selectedRoleId = selectedRoleId;
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
		final IScriptEval script = mService.getScriptEval(processModel);
		final StartNode startNode = mService.getProcessDocument(processModel).getProcessNode()
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
}
