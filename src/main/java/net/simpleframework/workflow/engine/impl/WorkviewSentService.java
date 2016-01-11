package net.simpleframework.workflow.engine.impl;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkviewSentService;
import net.simpleframework.workflow.engine.bean.WorkviewSentBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkviewSentService extends AbstractDbBeanService<WorkviewSentBean> implements
		IWorkviewSentService {

	@Override
	public IDataQuery<WorkviewSentBean> getWorkviewsSentList(final ID userId) {
		return query("userId=? order by createdate desc", userId);
	}
}