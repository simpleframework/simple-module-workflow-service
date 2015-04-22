package net.simpleframework.workflow.engine;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.bean.UserStatBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IUserStatService extends IDbBeanService<UserStatBean>, IWorkflowServiceAware {

	/**
	 * 获取用户统计信息
	 * 
	 * @param userId
	 * @return
	 */
	UserStatBean getUserStat(ID userId);
}
