package net.simpleframework.workflow.engine;

import java.util.Date;
import java.util.List;

import net.simpleframework.ado.FilterItems;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkitemService extends IWorkflowService<WorkitemBean>,
		IEventListenerAware<WorkitemBean>, IWorkflowScriptAware<WorkitemBean> {

	/**
	 * 获取环节实例
	 * 
	 * @param workitem
	 * @return
	 */
	ActivityBean getActivity(WorkitemBean workitem);

	/**
	 * 获取某一环节的工作列表
	 * 
	 * @param activity
	 * @param status
	 * @return
	 */
	List<WorkitemBean> getWorkitems(ActivityBean activity, EWorkitemStatus... status);

	/**
	 * 获取运行态的工作列表
	 * 
	 * @param user
	 * @return
	 */
	IDataQuery<WorkitemBean> getRunningWorklist(ID userId);

	/**
	 * 获取指定用户的工作列表
	 * 
	 * @param userId
	 * @param status
	 * @return
	 */
	IDataQuery<WorkitemBean> getWorklist(ID userId, EWorkitemStatus... status);

	/**
	 * 获取未读的工作列表
	 * 
	 * @param user
	 * @return
	 */
	IDataQuery<WorkitemBean> getUnreadWorklist(ID userId);

	IDataQuery<WorkitemBean> getWorklist(ID userId, FilterItems params);

	/**
	 * 完成当前的工作项
	 * 
	 * @param workitemComplete
	 */
	void doComplete(WorkitemComplete workitemComplete);

	/**
	 * 取回当前的工作项
	 * 
	 * @param workitem
	 */
	void doRetake(WorkitemBean workitem);

	/**
	 * 设置未读
	 * 
	 * @param workitem
	 */
	void doUnReadMark(WorkitemBean workitem);

	/**
	 * 设置已读
	 * 
	 * @param workitem
	 */
	void doReadMark(WorkitemBean workitem);

	void doUnTopMark(WorkitemBean workitem);

	void doTopMark(WorkitemBean workitem);

	/**
	 * 设置工作项委托
	 * 
	 * @param workitem
	 * @param userId
	 * @param dStartDate
	 * @param dCompleteDate
	 * @param description
	 */
	void doWorkitemDelegation(WorkitemBean workitem, ID userId, Date dStartDate, Date dCompleteDate,
			String description);

	/**
	 * 获取流程实例
	 * 
	 * @param workitem
	 * @return
	 */
	ProcessBean getProcessBean(WorkitemBean workitem);

	/**
	 * 删除当前的流程实例，删除条件是没有完成的工作
	 * 
	 * @param workitem
	 */
	void doDeleteProcess(WorkitemBean workitem);
}
