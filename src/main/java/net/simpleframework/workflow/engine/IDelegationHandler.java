package net.simpleframework.workflow.engine;

import net.simpleframework.workflow.IWorkflowHandler;
import net.simpleframework.workflow.engine.bean.DelegationBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDelegationHandler extends IWorkflowHandler, IWorkflowContextAware {

	/**
	 * 定义委托是否开始执行
	 * 
	 * @param delegation
	 * @return
	 */
	boolean isStart(DelegationBean delegation);

	/**
	 * 定义委托是否结束
	 * 
	 * @param delegation
	 * @return
	 */
	boolean isEnd(DelegationBean delegation);
}
