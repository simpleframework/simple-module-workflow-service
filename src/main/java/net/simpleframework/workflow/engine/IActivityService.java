package net.simpleframework.workflow.engine;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.workflow.schema.AbstractTaskNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IActivityService extends IWorkflowVariableAware<ActivityBean>,
		IWorkflowService<ActivityBean>, IEventListenerAware<ActivityBean>,
		IWorkflowScriptAware<ActivityBean> {

	/**
	 * 获取流程实例对象
	 * 
	 * @param activity
	 * @return
	 */
	ProcessBean getProcessBean(ActivityBean activity);

	/**
	 * 获取当前环节的节点描述
	 * 
	 * @param activity
	 * @return
	 */
	AbstractTaskNode getTaskNode(ActivityBean activity);

	/**
	 * 获取指定流程实例下的所有环节实例，默认按创建日期排序
	 * 
	 * @param processBean
	 * @return
	 */
	IDataQuery<ActivityBean> getActivities(ProcessBean processBean, EActivityStatus... status);

	/**
	 * 获取指定环节的后续环节实例
	 * 
	 * @param preActivity
	 * @return
	 */
	IDataQuery<ActivityBean> getNextActivities(ActivityBean preActivity);

	/**
	 * 
	 * @param activity
	 * @return
	 */
	ActivityBean getPreActivity(ActivityBean activity);

	/**
	 * 获取前一指定环节，获取的环节必须在创建链路上
	 * 
	 * @param activity
	 * @param tasknode
	 *           环节id或名称
	 * @return
	 */
	ActivityBean getPreActivity(ActivityBean activity, String tasknode);

	/**
	 * 
	 * @param processBean
	 * @return
	 */
	ActivityBean getStartActivity(ProcessBean processBean);

	/**
	 * 
	 * @param activityCallback
	 */
	void complete(ActivityComplete activityCallback);

	/**
	 * 挂起当前环节
	 * 
	 * @param activity
	 * @param resume
	 */
	void suspend(ActivityBean activity);

	/**
	 * 恢复当前环节
	 * 
	 * @param activity
	 */
	void resume(ActivityBean activity);

	/**
	 * 
	 * @param activity
	 * @param policy
	 *           放弃策略
	 */
	void abort(ActivityBean activity, EActivityAbortPolicy policy);

	/**
	 * 跳转到指定的任务环节
	 * 
	 * 跳转和回退的区别：跳转可以到任何一个有效的任务环节，按照模型创建参与者；回退只能是已运行过的任务，按运行历史创建参与者
	 * 
	 * @param activity
	 * @param tasknode
	 *           环节id或名称
	 */
	void jump(ActivityBean activity, String tasknode);

	/**
	 * 回退到指定的任务环节
	 * 
	 * @param activity
	 * @param tasknode
	 *           环节id或名称
	 */
	void fallback(ActivityBean activity, String tasknode);

	void fallback(ActivityBean activity);

	/**
	 * 运行远程子流程
	 * 
	 * @param activity
	 */
	void doRemoteSubTask(ActivityBean activity);

	/**
	 * 完成子流程环节
	 * 
	 * @param activity
	 * @param mappingVal
	 */
	void subComplete(ActivityBean activity, IMappingVal mappingVal);

	/**
	 * 获取表单实例
	 * 
	 * @param activity
	 * @return
	 */
	IWorkflowForm getWorkflowForm(ActivityBean activity);
}
