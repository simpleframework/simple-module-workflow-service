package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.bean.AbstractIdBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelLobBean extends AbstractIdBean {

	private char[] processSchema;

	public char[] getProcessSchema() {
		return processSchema;
	}

	public void setProcessSchema(final char[] processSchema) {
		this.processSchema = processSchema;
	}

	private static final long serialVersionUID = -8281273400041652815L;
}
