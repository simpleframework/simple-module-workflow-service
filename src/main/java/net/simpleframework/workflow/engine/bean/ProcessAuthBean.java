package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessAuthBean extends AbstractWorkflowBean {
	/* 流程实例id */
	private ID processId;
	/* 授权人 */
	private ID fromId;
	/* 被授权人 */
	private ID toId;

	@ColumnMeta(columnText = "#(Description)")
	private String description;

	public ID getProcessId() {
		return processId;
	}

	public void setProcessId(ID processId) {
		this.processId = processId;
	}

	public ID getFromId() {
		return fromId;
	}

	public void setFromId(ID fromId) {
		this.fromId = fromId;
	}

	public ID getToId() {
		return toId;
	}

	public void setToId(ID toId) {
		this.toId = toId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private static final long serialVersionUID = 6163208692153357882L;
}
