package net.simpleframework.workflow.engine.impl;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkviewService;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkviewBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkviewService extends AbstractDbBeanService<WorkviewBean> implements
		IWorkviewService {

	@Override
	public WorkviewBean[] createWorkviews(final WorkitemBean workitem, final ID... userIds) {
		return null;
	}
}