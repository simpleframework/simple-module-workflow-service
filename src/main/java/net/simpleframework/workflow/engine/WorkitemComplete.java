package net.simpleframework.workflow.engine;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.workflow.engine.IActivityService.PropSequential;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;

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
		final IWorkitemService service = context.getWorkitemService();
		final ActivityBean activity = service.getActivity(workitem);

		if (PropSequential.list(activity).size() > 0) {
			allCompleted = false;
		} else {
			final int allWorkitems = service.getWorkitemList(activity).getCount();
			// 完成的工作项
			final int complete = service.getWorkitemList(activity, EWorkitemStatus.complete)
					.getCount();
			if (complete + 1 < ParticipantUtils.getResponseValue(context.getActivityService()
					.getTaskNode(activity), allWorkitems)) {
				allCompleted = false;
			}
		}
	}

	public WorkitemBean getWorkitem() {
		return context.getWorkitemService().getBean(workitemId);
	}

	public boolean isAllCompleted() {
		return allCompleted;
	}

	private ActivityComplete activityComplete;

	public ActivityComplete getActivityComplete() {
		if (activityComplete == null) {
			activityComplete = new ActivityComplete(this);
		}
		return activityComplete;
	}

	public Object getWorkflowForm() {
		return context.getActivityService().getWorkflowForm(
				context.getWorkitemService().getActivity(getWorkitem()));
	}

	public void complete(final Map<String, String> parameters) {
		context.getWorkitemService().complete(parameters, this);
	}

	private static Map<ID, KVMap> variablesCache = new ConcurrentHashMap<ID, KVMap>();

	public KVMap getVariables() {
		KVMap kv = variablesCache.get(workitemId);
		if (kv == null) {
			variablesCache.put(workitemId, kv = new KVMap());
		}
		return kv;
	}

	public void done() {
		variablesCache.remove(workitemId);
	}

	public void reset() {
		completeCache.remove(workitemId);
	}

	private static Map<ID, WorkitemComplete> completeCache = new ConcurrentHashMap<ID, WorkitemComplete>();

	public static WorkitemComplete get(final WorkitemBean workitem) {
		final ID key = workitem.getId();
		WorkitemComplete workitemComplete = completeCache.get(key);
		if (workitemComplete == null) {
			completeCache.put(key, workitemComplete = new WorkitemComplete(workitem));
		}
		return workitemComplete;
	}

	private static final long serialVersionUID = 5112409107824255728L;
}
