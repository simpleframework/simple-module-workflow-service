package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter",
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "status", "readMark",
		"topMark" })
public class WorkitemBean extends AbstractWorkflowBean {
	/* 状态 */
	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EWorkitemStatus status;

	/* 关联的环节id */
	private ID activityId;

	/* 任务分派的用户id */
	private ID userId;

	/* 任务分派的角色id */
	private ID roleId;

	/* 用户和角色显示名 */
	private String userText, roleText;

	/* 实际执行的用户id，缺省值=userId */
	private ID userId2;

	/* 实际执行用户显示名 */
	private String userText2;

	/* 实际完成时间 */
	private Date completeDate;

	/* 是否已读 */
	@ColumnMeta(columnText = "#(WorkitemBean.0)")
	private boolean readMark;

	/* 是否置顶 */
	@ColumnMeta(columnText = "#(WorkitemBean.1)")
	private boolean topMark;

	/* 取回工作项的引用 */
	private ID retakeRef;

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

	public String getUserText() {
		return userText;
	}

	public void setUserText(final String userText) {
		this.userText = userText;
	}

	public String getRoleText() {
		return roleText;
	}

	public void setRoleText(final String roleText) {
		this.roleText = roleText;
	}

	public ID getUserId2() {
		return userId2;
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

	public boolean isTopMark() {
		return topMark;
	}

	public void setTopMark(final boolean topMark) {
		this.topMark = topMark;
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
