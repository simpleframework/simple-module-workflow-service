package net.simpleframework.workflow.engine.impl;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IProcessModelDomainRService;
import net.simpleframework.workflow.engine.IWorkflowServiceAware;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelDomainR;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelDomainRService extends AbstractDbBeanService<ProcessModelDomainR>
		implements IProcessModelDomainRService, IWorkflowServiceAware {

	@Override
	public ProcessModelDomainR getProcessModelDomainR(final ID domainId,
			final ProcessModelBean processModel) {
		final ID modelId = processModel.getId();
		ProcessModelDomainR r = getBean("modelId=? and domainId=?", modelId, domainId);
		if (r == null) {
			r = createBean();
			r.setModelId(modelId);
			r.setDomainId(domainId);
			insert(r);
		}
		return r;
	}
}
