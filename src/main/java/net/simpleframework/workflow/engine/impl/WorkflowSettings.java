package net.simpleframework.workflow.engine.impl;

import net.simpleframework.ctx.settings.PropertiesContextSettings;
import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkflowSettings extends PropertiesContextSettings implements IWorkflowContextAware {

	public WorkflowSettings() {
		setApplicationSettings(workflowContext.getApplicationContext().getContextSettings());
	}

	/**
	 * 获取当前服务器的地址
	 * 
	 * @return
	 */
	public String getServerUrl() {
		return "http://127.0.0.1:8080/";
	}

	public String getSubtaskUrl() {
		return "/wf-subtask-remote";
	}

	/**
	 * 获取将要过期的警告时间，单位小时
	 * 
	 * @return
	 */
	public int getHoursToTimeoutWarning() {
		return 8;
	}
}
