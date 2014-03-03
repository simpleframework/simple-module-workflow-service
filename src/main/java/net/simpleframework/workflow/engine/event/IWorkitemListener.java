package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkitemListener extends IWorkflowListener {

	void onStatusChange(WorkitemBean workitem);

	public static abstract class WorkitemAdapter implements IWorkitemListener {

		@Override
		public void onStatusChange(final WorkitemBean workitem) {
		}
	}
}
