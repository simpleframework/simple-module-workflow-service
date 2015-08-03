package net.simpleframework.workflow.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.common.object.ObjectFactory.IObjectCreatorListener;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkitemComplete extends ObjectEx implements IWorkflowServiceAware {

	private final ID workitemId;

	private boolean allCompleted = true;

	private WorkitemComplete(final WorkitemBean workitem) {
		workitemId = workitem.getId();

		// 判断是否所有的工作项都已完成
		final ActivityBean activity = wfwService.getActivity(workitem);

		if (PropSequential.list(activity).size() > 0) {
			allCompleted = false;
		} else {
			final int allParticipants = wfaService.getParticipants(activity, false).size();
			// 完成的工作项
			final int complete = wfaService.getParticipants2(activity).size();
			if (complete + 1 < TasknodeUtils.getResponseValue(wfaService.getTaskNode(activity),
					allParticipants)) {
				allCompleted = false;
			}
		}
	}

	private ActivityComplete _activityComplete;

	public ActivityComplete getActivityComplete() {
		return getActivityComplete(null);
	}

	public ActivityComplete getActivityComplete(final IObjectCreatorListener l) {
		if (_activityComplete == null) {
			_activityComplete = new ActivityComplete(getWorkitem());
			if (l != null) {
				l.onCreated(_activityComplete);
			}
		}
		return _activityComplete;
	}

	public WorkitemBean getWorkitem() {
		return wfwService.getBean(workitemId);
	}

	public boolean isAllCompleted() {
		return allCompleted;
	}

	public IWorkflowForm getWorkflowForm() {
		return wfaService.getWorkflowForm(wfwService.getActivity(getWorkitem()));
	}

	public void complete(final Map<String, String> parameters) {
		wfwService.doComplete(this);
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
}
