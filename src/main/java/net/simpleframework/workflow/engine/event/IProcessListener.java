package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.ProcessBean;

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
	void onProcessCreated(InitiateItem initiateItem, ProcessBean process);

	/**
	 * 流程被放弃时触发
	 * 
	 * @param process
	 * @param policy
	 */
	void onAbort(ProcessBean process, EProcessAbortPolicy policy);

	/**
	 * 流程挂起或恢复时触发
	 * 
	 * @param process
	 */
	void onSuspend(ProcessBean process);
}
