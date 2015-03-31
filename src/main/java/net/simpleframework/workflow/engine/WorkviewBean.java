package net.simpleframework.workflow.engine;

import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkviewBean extends AbstractWorkitemBean {
	/* 父id，转发 */
	private ID parentId;
	/* 待办id */
	private ID workitemId;

	public ID getParentId() {
		return parentId;
	}

	public void setParentId(final ID parentId) {
		this.parentId = parentId;
	}

	public ID getWorkitemId() {
		return workitemId;
	}

	public void setWorkitemId(final ID workitemId) {
		this.workitemId = workitemId;
	}

	private static final long serialVersionUID = 7218560930454033577L;
}
