package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.bean.ProcessBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IProcessListener extends IWorkflowListener {

	/**
	 * 流程创建时触发
	 * 
	 * @param initiateItem
	 * @param process
	 */
	void onCreated(InitiateItem initiateItem, ProcessBean process);

	/**
	 * 流程删除时触发
	 * 
	 * @param process
	 */
	void onDelete(ProcessBean process);

	/**
	 * 当流程状态改变时触发
	 * 
	 * @param process
	 * @param oStatus
	 */
	void onStatusChange(ProcessBean process, EProcessStatus oStatus);

	public static abstract class ProcessAdapter implements IProcessListener {

		@Override
		public void onCreated(final InitiateItem initiateItem, final ProcessBean process) {
		}

		@Override
		public void onDelete(final ProcessBean process) {
		}

		@Override
		public void onStatusChange(final ProcessBean process, final EProcessStatus oStatus) {
		}
	}
}
