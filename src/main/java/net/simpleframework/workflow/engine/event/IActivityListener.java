package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IActivityListener extends IWorkflowListener {

	/**
	 * 环节创建前触发
	 * 
	 * @param activityComplete
	 */
	void onBeforeComplete(ActivityComplete activityComplete);

	/**
	 * 环节创建后触发
	 * 
	 * @param activityComplete
	 */
	void onCompleted(ActivityComplete activityComplete);

	/**
	 * 过期检查
	 * 
	 * @param activity
	 */
	void onTimeoutCheck(ActivityBean activity);

	/**
	 * 状态变化时触发
	 * 
	 * @param activity
	 */
	void onStatusChange(ActivityBean activity, EActivityStatus oStatus);

	public static abstract class ActivityAdapter implements IActivityListener {

		@Override
		public void onBeforeComplete(final ActivityComplete activityComplete) {
		}

		@Override
		public void onCompleted(final ActivityComplete activityComplete) {
		}

		@Override
		public void onTimeoutCheck(final ActivityBean activity) {
		}

		@Override
		public void onStatusChange(final ActivityBean activity, final EActivityStatus oStatus) {
		}
	}
}
