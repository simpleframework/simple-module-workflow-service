package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.bean.AbstractIdBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@SuppressWarnings("serial")
public abstract class AbstractWorkflowBean extends AbstractIdBean {

	private Date createDate;

	/* 最后一次操作时间 */
	private Date lastUpdate;

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(final Date createDate) {
		this.createDate = createDate;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(final Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
