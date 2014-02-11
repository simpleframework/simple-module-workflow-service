package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IActivityEventListener extends IWorkflowEventListener {

	/**
	 * 环节完成时触发
	 * 
	 * @param activityComplete
	 */
	void onCompleted(ActivityComplete activityComplete);

	/**
	 * 环节放弃时触发
	 * 
	 * @param activity
	 * @param policy
	 */
	void onAbort(ActivityBean activity, EActivityAbortPolicy policy);

	/**
	 * 环节挂起时触发
	 * 
	 * @param activity
	 */
	void onSuspend(ActivityBean activity);

	/**
	 * 环节恢复时触发
	 * 
	 * @param activity
	 */
	void onResume(ActivityBean activity);

	/**
	 * 环节回退时触发
	 * 
	 * @param activity
	 * @param tasknode
	 */
	void onFallback(ActivityBean activity, String tasknode);

	public static abstract class ActivityAdapter implements IActivityEventListener {

		@Override
		public void onCompleted(final ActivityComplete activityComplete) {
		}

		@Override
		public void onAbort(final ActivityBean activity, final EActivityAbortPolicy policy) {
		}

		@Override
		public void onSuspend(final ActivityBean activity) {
		}

		@Override
		public void onResume(final ActivityBean activity) {
		}

		@Override
		public void onFallback(final ActivityBean activity, final String tasknode) {
		}
	}
}
