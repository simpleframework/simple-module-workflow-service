package net.simpleframework.workflow.engine.comment;

import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;
import net.simpleframework.module.common.content.AbstractComment;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityDeleteLogAdapter" })
public class WfComment extends AbstractComment {
	/* 工作项id，每一个工作项只允许一个意见 */
	private ID workitemId;
	/* 用户所在部门，取自工作项 */
	private ID deptId;
	/* 任务名称 */
	private String taskname;

	public ID getWorkitemId() {
		return workitemId;
	}

	public void setWorkitemId(final ID workitemId) {
		this.workitemId = workitemId;
	}

	public ID getDeptId() {
		return deptId;
	}

	public void setDeptId(final ID deptId) {
		this.deptId = deptId;
	}

	public String getTaskname() {
		return taskname;
	}

	public void setTaskname(final String taskname) {
		this.taskname = taskname;
	}

	private static final long serialVersionUID = -9144204555895365885L;
}
