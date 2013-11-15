package net.simpleframework.workflow.engine.impl;

import net.simpleframework.workflow.engine.DelegationBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DelegationService extends AbstractWorkflowService<DelegationBean> {
	private static DelegationService dm = new DelegationService();

	static DelegationService get() {
		return dm;
	}
}
