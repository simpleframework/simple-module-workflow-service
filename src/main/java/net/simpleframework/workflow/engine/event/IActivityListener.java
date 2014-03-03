package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ActivityBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IActivityListener extends IWorkflowListener {

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
	void onStatusChange(ActivityBean activity);

	public static abstract class ActivityAdapter implements IActivityListener {

		@Override
		public void onStatusChange(final ActivityBean activity) {
		}
	}
}
