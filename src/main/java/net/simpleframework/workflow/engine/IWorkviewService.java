package net.simpleframework.workflow.engine;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkviewService extends IDbBeanService<WorkviewBean>, IWorkflowServiceAware {

	/**
	 * 创建待阅项
	 * 
	 * @param workitem
	 * @param userIds
	 * @return
	 */
	WorkviewBean[] createWorkviews(WorkitemBean workitem, ID... userIds);
}
