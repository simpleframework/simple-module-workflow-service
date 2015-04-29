package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkitemListener extends IWorkflowListener {

	/**
	 * 创建时触发
	 * 
	 * @param workitem
	 */
	void onCreated(WorkitemBean workitem);

	/**
	 * 状态变化时触发
	 * 
	 * @param workitem
	 * @param oStatus
	 */
	void onStatusChange(WorkitemBean workitem, EWorkitemStatus oStatus);

	public static abstract class WorkitemAdapter implements IWorkitemListener {
		@Override
		public void onCreated(final WorkitemBean workitem) {
		}

		@Override
		public void onStatusChange(final WorkitemBean workitem, final EWorkitemStatus oStatus) {
		}
	}
}
