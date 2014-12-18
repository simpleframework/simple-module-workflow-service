package net.simpleframework.workflow.engine;

import java.util.Date;
import java.util.Properties;

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
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "title", "status" })
public class ProcessBean extends AbstractWorkflowBean {
	private ID modelId;

	private String version;

	private Date completeDate;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.1)")
	private String title;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EProcessStatus status;

	/* 流程启动者及启动角色 */
	private ID userId, roleId;
	/* 流程启动者所在的部门id */
	private ID deptId;
	/* 流程启动者所在的机构id */
	private ID orgId;

	/* 用户和角色显示名 */
	private String userText;

	private Properties properties;

	public ID getModelId() {
		return modelId;
	}

	public void setModelId(final ID modelId) {
		this.modelId = modelId;
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

	public ID getOrgId() {
		return orgId;
	}

	public void setOrgId(final ID orgId) {
		this.orgId = orgId;
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

	@Override
	public String toString() {
		return getTitle();
	}

	private static final long serialVersionUID = -4249661933122865392L;
}
