package net.simpleframework.workflow.engine;

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
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "status",
		"lastUserId", "lastUpdate" })
public class ProcessModelBean extends AbstractWorkflowBean {

	/* 模型名称 */
	private String modelName;

	/* 显示名称 */
	private String modelText;

	/* 状态 */
	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EProcessModelStatus status;

	/* 模型创建者 */
	private ID userId;

	/* 用户显示名 */
	private String userText;

	/* 统计，流程实例数 */
	private int processCount;

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

	public String getUserText() {
		return userText;
	}

	public void setUserText(String userText) {
		this.userText = userText;
	}

	public int getProcessCount() {
		return processCount;
	}

	public void setProcessCount(final int processCount) {
		this.processCount = processCount;
	}

	@Override
	public String toString() {
		final String txt = getModelText();
		return StringUtils.hasText(txt) ? txt : getModelName();
	}

	private static final long serialVersionUID = 6413648325601228584L;
}