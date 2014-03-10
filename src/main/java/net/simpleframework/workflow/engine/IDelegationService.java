package net.simpleframework.workflow.engine;

import net.simpleframework.ado.query.IDataQuery;

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

	/**
	 * 获取指定用户的委托任务
	 * 
	 * @param userId
	 * @return
	 */
	IDataQuery<DelegationBean> queryDelegations(Object userId);

	/**
	 * 放弃当前委托
	 * 
	 * @param delegation
	 */
	void abort(DelegationBean delegation);

	/**
	 * 接受当前委托
	 * 
	 * @param delegation
	 */
	void accept(DelegationBean delegation);
}
