package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter",
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "status" })
public class WorkitemBean extends AbstractWorkflowBean {
	private EWorkitemStatus status;

	private ID activityId;

	// private ID previousId;

	private ID userId, roleId;

	private Date completeDate;

	private boolean readMark;

	public EWorkitemStatus getStatus() {
		return status != null ? status : EWorkitemStatus.running;
	}

	public void setStatus(final EWorkitemStatus status) {
		this.status = status;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public ID getRoleId() {
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
	}

	public ID getActivityId() {
		return activityId;
	}

	public void setActivityId(final ID activityId) {
		this.activityId = activityId;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(final Date completeDate) {
		this.completeDate = completeDate;
	}

	public boolean isReadMark() {
		return readMark;
	}

	public void setReadMark(final boolean readMark) {
		this.readMark = readMark;
	}

	private static final long serialVersionUID = 1553478269588195799L;
}
