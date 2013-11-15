package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class VariableLogBean extends AbstractIdBean {
	private String variableId;

	private Date createDate;

	private ID userId;

	private String stringValue;

	private char[] clobValue;

	public String getVariableId() {
		return variableId;
	}

	public void setVariableId(final String variableId) {
		this.variableId = variableId;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(final Date createDate) {
		this.createDate = createDate;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(final String stringValue) {
		this.stringValue = stringValue;
	}

	public char[] getClobValue() {
		return clobValue;
	}

	public void setClobValue(final char[] clobValue) {
		this.clobValue = clobValue;
	}

	private static final long serialVersionUID = 610238022846863774L;
}
