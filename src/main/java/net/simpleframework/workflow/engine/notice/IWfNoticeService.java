package net.simpleframework.workflow.engine.notice;

import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfNoticeService extends IDbBeanService<WfNoticeBean>, IWorkflowContextAware {

	/**
	 * 根据no号获取
	 * 
	 * @param no
	 * @return
	 */
	IWfNoticeTypeHandler getWfNoticeTypeHandler(int no);
}
