package net.simpleframework.workflow.engine;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowService<T extends AbstractIdBean> extends IDbBeanService<T>,
		IWorkflowContextAware {

	/**
	 * 是否最终状态，不能再发生状态转换
	 * 
	 * @param t
	 * @return
	 */
	boolean isFinalStatus(T t);
}
