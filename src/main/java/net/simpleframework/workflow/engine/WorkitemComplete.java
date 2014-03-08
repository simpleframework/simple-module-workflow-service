package net.simpleframework.workflow.engine;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkitemComplete extends ObjectEx implements Serializable, IWorkflowContextAware {

	private final ID workitemId;

	private boolean allCompleted = true;

	private WorkitemComplete(final WorkitemBean workitem) {
		workitemId = workitem.getId();

		// 判断是否所有的工作项都已完成
		final ActivityBean activity = wService.getActivity(workitem);

		if (PropSequential.list(activity).size() > 0) {
			allCompleted = false;
		} else {
			final int allParticipants = aService.getParticipants(activity, false).size();
			// 完成的工作项
			final int complete = aService.getParticipants2(activity).size();
			if (complete + 1 < TasknodeUtils.getResponseValue(aService.getTaskNode(activity),
					allParticipants)) {
				allCompleted = false;
			}
		}
	}

	public WorkitemBean getWorkitem() {
		return wService.getBean(workitemId);
	}

	public boolean isAllCompleted() {
		return allCompleted;
	}

	public IWorkflowForm getWorkflowForm() {
		return aService.getWorkflowForm(wService.getActivity(getWorkitem()));
	}

	public void complete(final Map<String, String> parameters) {
		wService.complete(this);
	}

	private Map<String, Object> variables;

	public Map<String, Object> getVariables() {
		if (variables == null) {
			variables = new KVMap();
		}
		return variables;
	}

	public void done() {
		WORKITEMCOMPLETE_CACHE.remove(workitemId);
	}

	private static Map<ID, WorkitemComplete> WORKITEMCOMPLETE_CACHE = new ConcurrentHashMap<ID, WorkitemComplete>();

	public static WorkitemComplete get(final WorkitemBean workitem) {
		final ID key = workitem.getId();
		WorkitemComplete workitemComplete = WORKITEMCOMPLETE_CACHE.get(key);
		if (workitemComplete == null) {
			WORKITEMCOMPLETE_CACHE.put(key, workitemComplete = new WorkitemComplete(workitem));
		}
		return workitemComplete;
	}

	private static final long serialVersionUID = 5112409107824255728L;
}
