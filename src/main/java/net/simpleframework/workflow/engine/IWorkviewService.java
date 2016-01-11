package net.simpleframework.workflow.engine;

import java.util.List;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.bean.WorkviewBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkviewService extends IDbBeanService<WorkviewBean>, IWorkflowContextAware {

	/**
	 * 创建待阅项
	 * 
	 * @param workitem
	 * @param userIds
	 * @return
	 */
	List<WorkviewBean> createWorkviews(WorkitemBean workitem, ID... userIds);

	List<WorkviewBean> createForwardWorkviews(WorkviewBean workview, ID... userIds);

	/**
	 * 获取待阅bean对象
	 * 
	 * @param processId
	 * @param userId
	 * @return
	 */
	WorkviewBean getWorkviewBean(Object processId, Object userId);

	void doReadMark(WorkviewBean workview);

	IDataQuery<WorkviewBean> getUnreadWorkviewsList(ID userId);

	/**
	 * 获取指定用户的待阅
	 * 
	 * @param userId
	 * @return
	 */
	IDataQuery<WorkviewBean> getWorkviewsList(ID userId);

	IDataQuery<WorkviewBean> getWorkviewsListBySent(ID sentId);
}
