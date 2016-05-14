package net.simpleframework.workflow.engine;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.schema.ProcessDocument;
import net.simpleframework.workflow.schema.ProcessNode;

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

	ProcessNode getProcessNode(ProcessBean process);

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
	 * @param participant
	 * @param variables
	 * @param properties
	 * @param topic
	 * @return
	 */
	ProcessBean doStartProcess(ProcessModelBean processModel, Participant participant,
			KVMap variables, Properties properties, String topic);

	/**
	 * 获取流程实例列表
	 * 
	 * @param domainId
	 * @param processModel
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessBean> getProcessList(ID domainId, ProcessModelBean processModel,
			EProcessStatus... status);

	/**
	 * 获取经办的流程实例
	 * 
	 * @param userId
	 * @param processModel
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessBean> getProcessWlist(ID userId, ProcessModelBean processModel,
			EProcessStatus... status);

	/**
	 * 获取指定部门下经办的流程实例
	 * 
	 * @param deptIds
	 * @param processModel
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessBean> getProcessWlistInDept(ID[] deptIds, ProcessModelBean processModel,
			EProcessStatus... status);

	/**
	 * 获取某一域下经办的流程实例
	 * 
	 * @param domainId
	 * @param processModel
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessBean> getProcessWlistInDomain(ID domainId, ProcessModelBean processModel,
			EProcessStatus... status);

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
	void doUpdateKV(ProcessBean process, Map<String, Object> kv);

	/**
	 * 更新流程的过期时间
	 * 
	 * @param process
	 * @param timeoutDate
	 */
	void doUpdateTimeoutDate(ProcessBean process, Date timeoutDate);

	void doUpdateTimeoutDate(ProcessBean process, int hours);

	void doUpdateViews(ProcessBean process);

	/**
	 * 获取待阅表单
	 * 
	 * @param process
	 * @return
	 */
	IWorkflowView getWorkflowView(ProcessBean process);
}
