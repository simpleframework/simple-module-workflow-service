package net.simpleframework.workflow.engine;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.ado.db.common.EntityInterceptor;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = "net.simpleframework.module.log.EntityDeleteLogAdapter")
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
