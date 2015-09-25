package net.simpleframework.workflow.engine.bean;

import java.util.Date;
import java.util.Properties;

import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.workflow.engine.EProcessStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter",
		"net.simpleframework.module.log.EntityDeleteLogAdapter",
		"net.simpleframework.module.log.EntityInsertLogAdapter" }, columns = { "title", "status" })
public class ProcessBean extends AbstractWorkflowBean {
	/* 模型id */
	private ID modelId;
	/* 模型名称 */
	private String modelName;

	private String version;

	private Date completeDate;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.1)")
	private String title;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EProcessStatus status;

	/* 流程编号 */
	private String pno;

	/* 流程启动者及启动角色 */
	private ID userId, roleId;
	/* 流程启动者所在的部门id */
	private ID deptId;
	/* 流程启动者所在的域，等于机构id */
	private ID domainId;

	/* 用户和角色显示名 */
	private String userText;

	private Properties properties;

	/* 意见数 */
	private int comments;
	/* 浏览数 */
	private int views;

	public ID getModelId() {
		return modelId;
	}

	public void setModelId(final ID modelId) {
		this.modelId = modelId;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(final String modelName) {
		this.modelName = modelName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(final Date completeDate) {
		this.completeDate = completeDate;
	}

	public EProcessStatus getStatus() {
		return status != null ? status : EProcessStatus.running;
	}

	public void setStatus(final EProcessStatus status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getPno() {
		return pno;
	}

	public void setPno(final String pno) {
		this.pno = pno;
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

	public Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
		}
		return properties;
	}

	public void setProperties(final Properties properties) {
		this.properties = properties;
	}

	public int getComments() {
		return comments;
	}

	public void setComments(final int comments) {
		this.comments = comments;
	}

	public int getViews() {
		return views;
	}

	public void setViews(final int views) {
		this.views = views;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(wfpmService.getBean(getModelId()));
		String str = getTitle();
		if (StringUtils.hasText(str)) {
			sb.append(" / ").append(str);
		}
		return sb.toString();
	}

	private static final long serialVersionUID = -4249661933122865392L;
}
