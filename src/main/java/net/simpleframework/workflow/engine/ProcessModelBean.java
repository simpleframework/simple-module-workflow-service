package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelBean extends AbstractWorkflowBean {
	private String modelName, modelText;

	private EProcessModelStatus status;

	/**
	 * 模型创建者
	 */
	private ID userId;

	private ID lastUserId;

	private Date lastUpdate;

	public String getModelName() {
		return modelName;
	}

	public void setModelName(final String modelName) {
		this.modelName = modelName;
	}

	public String getModelText() {
		return modelText;
	}

	public void setModelText(final String modelText) {
		this.modelText = modelText;
	}

	public EProcessModelStatus getStatus() {
		return status != null ? status : EProcessModelStatus.edit;
	}

	public void setStatus(final EProcessModelStatus status) {
		this.status = status;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public ID getLastUserId() {
		return lastUserId;
	}

	public void setLastUserId(final ID lastUserId) {
		this.lastUserId = lastUserId;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(final Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	@Override
	public String toString() {
		return StringUtils.text(getModelText(), getModelName());
	}

	public static final String modelId = "__model_Id";

	private static final long serialVersionUID = 6413648325601228584L;
}