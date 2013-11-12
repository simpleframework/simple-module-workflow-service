package net.simpleframework.workflow.engine;

import net.simpleframework.ado.bean.AbstractIdBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessLobBean extends AbstractIdBean {
	private char[] processModel;

	public char[] getProcessModel() {
		return processModel;
	}

	public void setProcessModel(final char[] processModel) {
		this.processModel = processModel;
	}

	private static final long serialVersionUID = 219135045162448573L;
}
