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

	/* 流程实例id */
	private ID processId;

	/* 关联的环节id */
	private ID activityId;

	/* 任务分派的用户id */
	private ID userId;
	/* 用户所在的部门id */
	private ID deptId;
	/* 用户所在的域，等于机构id */
	private ID domainId;

	/* 任务分派的角色id */
	private ID roleId;

	/* 用户显示名 */
	private String userText;

	/* 实际执行的用户id，缺省值=userId */
	private ID userId2;
	/* 实际执行用户显示名 */
	private String userText2;
	/* 实际执行的用户所在的部门 */
	private ID deptId2;

	/* 实际完成时间 */
	private Date completeDate;

	/* 是否已读 */
	@ColumnMeta(columnText = "#(WorkitemBean.0)")
	private boolean readMark;
	/* 第一次阅读时间 */
	private Date readDate;

	/* 是否置顶 */
	@ColumnMeta(columnText = "#(WorkitemBean.1)")
	private boolean topMark;

	/* 取回工作项的引用 */
	private ID retakeRef;

	/* 扩展字段1 */
	private String ext1;
	/* 扩展字段2 */
	private int ext2;

	public EWorkitemStatus getStatus() {
		return status != null ? status : EWorkitemStatus.running;
	}

	public void setStatus(final EWorkitemStatus status) {
		this.status = status;
	}

	public ID getProcessId() {
		return processId;
	}

	public void setProcessId(final ID processId) {
		this.processId = processId;
	}

	public ID getActivityId() {
		return activityId;
	}

	public void setActivityId(final ID activityId) {
		this.activityId = activityId;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public ID getDeptId() {
		return deptId;
	}

	public void setDeptId(final ID deptId) {
		this.deptId = deptId;
	}

	public ID getDomainId() {
		return domainId;
	}

	public void setDomainId(final ID domainId) {
		this.domainId = domainId;
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

	public ID getDeptId2() {
		return deptId2;
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

	public boolean isReadMark() {
		return readMark;
	}

	public void setReadMark(final boolean readMark) {
		this.readMark = readMark;
	}

	public Date getReadDate() {
		return readDate;
	}

	public void setReadDate(final Date readDate) {
		this.readDate = readDate;
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

	public String getExt1() {
		return ext1;
	}

	public void setExt1(final String ext1) {
		this.ext1 = ext1;
	}

	public int getExt2() {
		return ext2;
	}

	public void setExt2(final int ext2) {
		this.ext2 = ext2;
	}

	@Override
	public String toString() {
		return "Workitem [" + getUserText() + ", " + status + "]";
	}

	private static final long serialVersionUID = 1553478269588195799L;
}
