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

	private Date completeDate;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.2)")
	private String title;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EProcessStatus status;

	/* 流程启动者及启动角色 */
	private ID userId, roleId;

	private Properties properties;

	public ID getModelId() {
		return modelId;
	}

	public void setModelId(final ID modelId) {
		this.modelId = modelId;
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

	public ID getRoleId() {
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
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
