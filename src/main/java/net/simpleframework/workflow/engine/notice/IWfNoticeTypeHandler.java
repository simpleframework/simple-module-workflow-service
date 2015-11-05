package net.simpleframework.workflow.engine.notice;

import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfNoticeTypeHandler extends IWorkflowContextAware {

	/**
	 * 发消息
	 * 
	 * @param notice
	 */
	void doSent(WfNoticeBean notice) throws Exception;

	/**
	 * 获取handler的唯一编号
	 * 
	 * @return
	 */
	int getNo();
}