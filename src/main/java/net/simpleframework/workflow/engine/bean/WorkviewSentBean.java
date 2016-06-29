package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.bean.AbstractUserAwareBean;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkviewSentBean extends AbstractUserAwareBean {
	/* 待办id */
	private ID workitemId;
	/* 流程实例id */
	private ID processId;

	/* 待阅id，转发 */
	private ID workviewId;

	/* 发送者显示名 */
	private String userText;
	/* 发送者所在的域，等于机构id */
	private ID domainId;

	public ID getWorkitemId() {
		return workitemId;
	}

	public void setWorkitemId(final ID workitemId) {
		this.workitemId = workitemId;
	}

	public ID getProcessId() {
		return processId;
	}

	public void setProcessId(final ID processId) {
		this.processId = processId;
	}

	public ID getWorkviewId() {
		return workviewId;
	}

	public void setWorkviewId(final ID workviewId) {
		this.workviewId = workviewId;
	}

	public String getUserText() {
		return userText;
	}

	public void setUserText(final String userText) {
		this.userText = userText;
	}

	public ID getDomainId() {
		return domainId;
	}

	public void setDomainId(final ID domainId) {
		this.domainId = domainId;
	}

	private static final long serialVersionUID = 6095931804414330559L;
}