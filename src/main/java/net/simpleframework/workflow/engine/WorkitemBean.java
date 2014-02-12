package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.ColumnMeta;
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
	/* 状态 */
	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EWorkitemStatus status;

	/* 关联的环节id */
	private ID activityId;

	/* 任务分派的用户id */
	private ID userId;

	/* 一务分派的角色id */
	private ID roleId;

	/* 实际执行的用户id，缺省值=userId */
	private ID userId2;

	/* 实际完成时间 */
	private Date completeDate;

	/* 计划完成时间 */
	private Date endDate;

	/* 是否已读 */
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

	public ID getUserId2() {
		return userId2;
	}

	public void setUserId2(final ID userId2) {
		this.userId2 = userId2;
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

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(final Date endDate) {
		this.endDate = endDate;
	}

	public boolean isReadMark() {
		return readMark;
	}

	public void setReadMark(final boolean readMark) {
		this.readMark = readMark;
	}

	private static final long serialVersionUID = 1553478269588195799L;
}
