package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessModelBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IProcessEventListener extends IWorkflowEventListener {

	/**
	 * 模型部署时触发
	 * 
	 * @param processModel
	 */
	void onModelDeploy(ProcessModelBean processModel);

	void onModelResume(ProcessModelBean processModel);

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
	void onProcessAbort(ProcessBean process, EProcessAbortPolicy policy);

	void onProcessDelete(ProcessBean process);

	/**
	 * 流程挂起或恢复时触发
	 * 
	 * @param process
	 */
	void onProcessSuspend(ProcessBean process);

	void onProcessResume(ProcessBean process);
}
