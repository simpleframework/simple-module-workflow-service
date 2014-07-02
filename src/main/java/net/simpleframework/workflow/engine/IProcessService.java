package net.simpleframework.workflow.engine;

import java.util.Properties;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.schema.ProcessDocument;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IProcessService extends IWorkflowVariableAware<ProcessBean>,
		IEventListenerAware<ProcessBean>, IWorkflowScriptAware<ProcessBean>,
		IWorkflowService<ProcessBean> {

	ProcessDocument getProcessDocument(ProcessBean process);

	/**
	 * 获取流程模型
	 * 
	 * @param process
	 * @return
	 */
	ProcessModelBean getProcessModel(ProcessBean process);

	/**
	 * 创建流程实例
	 * 
	 * @param initiateItem
	 *        启动项
	 * @param topic
	 *        主题
	 * @return
	 */
	ProcessBean doStartProcess(InitiateItem initiateItem, String topic);

	ProcessBean doStartProcess(InitiateItem initiateItem);

	/**
	 * 非人工方式启动流程，一般用在子流程及接口方式
	 * 
	 * @param processModel
	 * @param variables
	 * @param properties
	 * @param topic
	 * @return
	 */
	ProcessBean doStartProcess(ProcessModelBean processModel, KVMap variables,
			Properties properties, String topic);

	/**
	 * 获取流程实例列表
	 * 
	 * @param processModel
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessBean> getProcessList(ProcessModelBean processModel, EProcessStatus... status);

	/**
	 * 挂起流程
	 * 
	 * @param process
	 * @param resume
	 */
	void doSuspend(ProcessBean process);

	/**
	 * 恢复流程
	 * 
	 * @param process
	 */
	void doResume(ProcessBean process);

	/**
	 * 放弃流程实例
	 * 
	 * @param process
	 * @param policy
	 */
	void doAbort(ProcessBean process, EProcessAbortPolicy policy);

	/**
	 * 子流程返回到主流程
	 * 
	 * @param process
	 */
	void doBackToRemote(ProcessBean process);

	/**
	 * 获取创建流程后，当前流程启动者的第一个工作项
	 * 
	 * @param process
	 * @return
	 */
	WorkitemBean getFirstWorkitem(ProcessBean process);

	/**
	 * 设置流程标题
	 * 
	 * @param process
	 * @param title
	 */
	void doUpdateTitle(ProcessBean process, String title);
}
