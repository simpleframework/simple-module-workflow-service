package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter" }, columns = {
		"readMark", "topMark" })
public class WorkviewBean extends AbstractWorkitemBean {
	/* 父id，转发 */
	private ID parentId;

	/* 待办id */
	private ID workitemId;

	/* 发送id */
	private ID sentId;

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

	public ID getSentId() {
		return sentId;
	}

	public void setSentId(ID sentId) {
		this.sentId = sentId;
	}

	private static final long serialVersionUID = 7218560930454033577L;
}
