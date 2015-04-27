package net.simpleframework.workflow.engine.bean;

import java.util.Date;

import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.workflow.engine.EWorkitemStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter",
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "status", "readMark",
		"topMark" })
public class WorkitemBean extends AbstractWorkitemBean {
	/* 状态 */
	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EWorkitemStatus status;

	/* 关联的环节id */
	private ID activityId;

	/* 任务分派的角色id */
	private ID roleId;

	/* 实际执行的用户id，缺省值=userId */
	private ID userId2;
	/* 实际执行用户显示名 */
	private String userText2;
	/* 实际执行的用户所在的部门 */
	private ID deptId2;

	/* 实际完成时间 */
	private Date completeDate;

	/* 取回工作项的引用 */
	private ID retakeRef;

	public EWorkitemStatus getStatus() {
		return status != null ? status : EWorkitemStatus.running;
	}

	public void setStatus(final EWorkitemStatus status) {
		this.status = status;
	}

	public ID getActivityId() {
		return activityId;
	}

	public void setActivityId(final ID activityId) {
		this.activityId = activityId;
	}

	public ID getRoleId() {
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
	}

	public ID getUserId2() {
		return userId2 != null ? userId2 : getUserId();
	}

	public void setUserId2(final ID userId2) {
		this.userId2 = userId2;
	}

	public String getUserText2() {
		return StringUtils.hasText(userText2) ? userText2 : getUserText();
	}

	public void setUserText2(final String userText2) {
		this.userText2 = userText2;
	}

	public ID getDeptId2() {
		return deptId2 != null ? deptId2 : getDeptId();
	}

	public void setDeptId2(final ID deptId2) {
		this.deptId2 = deptId2;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(final Date completeDate) {
		this.completeDate = completeDate;
	}

	public ID getRetakeRef() {
		return retakeRef;
	}

	public void setRetakeRef(final ID retakeRef) {
		this.retakeRef = retakeRef;
	}

	@Override
	public String toString() {
		return "Workitem [" + getUserText() + ", " + status + "]";
	}

	private static final long serialVersionUID = 1553478269588195799L;
}
