package net.simpleframework.workflow.engine;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = "net.simpleframework.module.log.EntityDeleteLogAdapter")
public class VariableBean extends AbstractIdBean {
	private EVariableSource variableSource;

	private ID sourceId;

	private String variableName;

	private String stringValue;

	/**
	 * vtCollection、vtMap以JSON格式保存
	 */
	private char[] clobValue;

	public EVariableSource getVariableSource() {
		return variableSource;
	}

	public void setVariableSource(final EVariableSource variableSource) {
		this.variableSource = variableSource;
	}

	public ID getSourceId() {
		return sourceId;
	}

	public void setSourceId(final ID sourceId) {
		this.sourceId = sourceId;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(final String variableName) {
		this.variableName = variableName;
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

	private static final long serialVersionUID = -3897086526014759768L;
}
