package net.simpleframework.workflow.engine;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.bean.WorkviewSentBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkviewSentService
		extends IDbBeanService<WorkviewSentBean>, IWorkflowContextAware {

	IDataQuery<WorkviewSentBean> getWorkviewsSentList(ID userId);
}
