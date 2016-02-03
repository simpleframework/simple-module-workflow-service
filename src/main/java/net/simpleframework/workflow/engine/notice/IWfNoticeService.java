package net.simpleframework.workflow.engine.notice;

import java.util.Date;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfNoticeService extends IDbBeanService<WfNoticeBean>, IWorkflowContextAware {

	/**
	 * 按流程添加通知消息
	 * 
	 * @param sentId
	 * @param process
	 * @param userId
	 * @param dsentDate
	 * @param smessage
	 * @param typeno
	 * @return
	 */
	WfNoticeBean addWfNotice(String sentId, ProcessBean process, ID userId, Date dsentDate,
			String smessage, int typeno);

	/**
	 * 按任务项添加通知消息
	 * 
	 * @param sentId
	 * @param workitem
	 * @param dsentDate
	 * @param smessage
	 * @param typeno
	 * @return
	 */
	WfNoticeBean addWfNotice(String sentId, WorkitemBean workitem, Date dsentDate, String smessage,
			int typeno);

	/**
	 * 
	 * @param sentId
	 * @return
	 */
	WfNoticeBean getNoticeBeanBySentId(String sentId);

	/**
	 * 根据no号获取
	 * 
	 * @param no
	 * @return
	 */
	IWfNoticeTypeHandler getWfNoticeTypeHandler(int no);
}
