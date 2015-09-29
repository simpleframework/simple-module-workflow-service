package net.simpleframework.workflow.engine.impl;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IProcessModelDomainRService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.ProcessModelDomainR;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelDomainRService extends AbstractDbBeanService<ProcessModelDomainR>
		implements IProcessModelDomainRService, IWorkflowContextAware {

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
