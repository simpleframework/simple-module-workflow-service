package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkitemService extends IDbBeanService<WorkitemBean>,
		IEventListenerAware<WorkitemBean>, IWorkflowScriptAware<WorkitemBean> {

	/**
	 * 获取环节实例
	 * 
	 * @param workitem
	 * @return
	 */
	ActivityBean getActivity(WorkitemBean workitem);

	/**
	 * 获取某一环节的所有工作列表
	 * 
	 * @param activity
	 * @return
	 */
	IDataQuery<WorkitemBean> getWorkitemList(ActivityBean activity, EWorkitemStatus... status);

	/**
	 * 获取指定用户的工作列表
	 * 
	 * @param userId
	 * @param status
	 * @return
	 */
	IDataQuery<WorkitemBean> getWorkitemList(ID userId, EWorkitemStatus... status);

	/**
	 * 完成当前的工作项
	 * 
	 * @param workitemComplete
	 */
	void complete(WorkitemComplete workitemComplete);

	/**
	 * 取回当前的工作项
	 * 
	 * @param workitem
	 */
	void retake(WorkitemBean workitem);

	/**
	 * 设置已读或标记未读
	 * 
	 * @param workitem
	 * @param unread
	 *           true标记为未读
	 */
	void readMark(WorkitemBean workitem, boolean unread);

	/**
	 * 设置工作项委托
	 * 
	 * @param workitem
	 * @param userId
	 * @param startDate
	 * @param endDate
	 * @param description
	 */
	void setWorkitemDelegation(WorkitemBean workitem, ID userId, Date startDate, Date endDate,
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
	void deleteProcess(WorkitemBean workitem);

	/**
	 * 是否最终状态，不可状态转换
	 * 
	 * @param workitem
	 * @return
	 */
	boolean isFinalStatus(WorkitemBean workitem);
}
