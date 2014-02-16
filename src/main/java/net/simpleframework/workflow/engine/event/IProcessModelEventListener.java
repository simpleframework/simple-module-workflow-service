package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ProcessModelBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IProcessModelEventListener extends IWorkflowEventListener {

	/**
	 * 
	 * @param processModel
	 * @param source
	 * @param evn
	 */
	void onStatusChange(ProcessModelBean processModel);

	public static abstract class ProcessModelAdapter implements IProcessModelEventListener {

		@Override
		public void onStatusChange(final ProcessModelBean processModel) {
		}
	}
}