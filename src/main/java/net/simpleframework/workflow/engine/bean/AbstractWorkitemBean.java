package net.simpleframework.workflow.engine.bean;

import java.util.Date;

import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@SuppressWarnings("serial")
public abstract class AbstractWorkitemBean extends AbstractWorkflowBean {
	/* 流程实例id */
	private ID processId;

	/* 任务分派的用户id */
	private ID userId;
	/* 用户显示名 */
	private String userText;
	/* 用户所在的部门id */
	private ID deptId;
	/* 用户所在的域，等于机构id */
	private ID domainId;

	/* 是否已读 */
	@ColumnMeta(columnText = "#(WorkitemBean.0)")
	private boolean readMark;
	/* 第一次阅读时间 */
	private Date readDate;

	/* 是否置顶 */
	@ColumnMeta(columnText = "#(WorkitemBean.1)")
	private boolean topMark;

	/* 扩展字段1 */
	private String ext1;
	/* 扩展字段2 */
	private int ext2;

	public ID getProcessId() {
		return processId;
	}

	public void setProcessId(final ID processId) {
		this.processId = processId;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public String getUserText() {
		return userText;
	}

	public void setUserText(final String userText) {
		this.userText = userText;
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
}
