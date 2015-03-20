package net.simpleframework.workflow.engine.impl;

import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IProcessModelDomainRService;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelDomainR;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelDomainRService extends AbstractDbBeanService<ProcessModelDomainR>
		implements IProcessModelDomainRService {

	public ProcessModelDomainR getProcessModelDomainR(final ProcessModelBean processModel,
			final Object domainId) {
		return null;
	}

}
