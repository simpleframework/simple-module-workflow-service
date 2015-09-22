package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDelegationService extends IWorkflowService<DelegationBean> {

	/**
	 * 获取正在运行的工作项委托
	 * 
	 * @param workitem
	 * @return
	 */
	DelegationBean queryRunningDelegation(WorkitemBean workitem);

	DelegationBean queryRunningDelegation(ID userId);

	/**
	 * 获取指定用户的委托任务
	 * 
	 * @param userId
	 * @param source
	 * @return
	 */
	IDataQuery<DelegationBean> queryDelegations(ID userId, EDelegationSource source);

	/**
	 * 放弃当前委托
	 * 
	 * @param delegation
	 */
	void doAbort(DelegationBean delegation);

	/**
	 * 接受当前委托
	 * 
	 * @param delegation
	 * @param description2
	 */
	void doAccept(DelegationBean delegation, String description2);

	/**
	 * 拒绝当前委托
	 * 
	 * @param delegation
	 * @param description2
	 */
	void doRefuse(DelegationBean delegation, String description2);

	/**
	 * 设置用户委托
	 * 
	 * @param sourceId
	 * @param userId
	 * @param dStartDate
	 * @param dCompleteDate
	 * @param description
	 */
	void doUserDelegation(ID sourceId, ID userId, Date dStartDate, Date dCompleteDate,
			String description);
}
