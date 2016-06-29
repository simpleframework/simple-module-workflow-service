package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelDomainR extends AbstractIdBean {
	/* 模型id */
	private ID modelId;
	/* 机构id */
	private ID domainId;
	/* 统计，流程实例数 */
	private int processCount;

	public ID getDomainId() {
		return domainId;
	}

	public void setDomainId(final ID domainId) {
		this.domainId = domainId;
	}

	public ID getModelId() {
		return modelId;
	}

	public void setModelId(final ID modelId) {
		this.modelId = modelId;
	}

	public int getProcessCount() {
		return processCount;
	}

	public void setProcessCount(final int processCount) {
		this.processCount = processCount;
	}

	private static final long serialVersionUID = 9183200639048673963L;
}
