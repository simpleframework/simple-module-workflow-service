package net.simpleframework.workflow.engine;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.ProcessModelDomainR;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IProcessModelDomainRService extends IDbBeanService<ProcessModelDomainR> {

	/**
	 * 获取流程模型与域(机构)的关系
	 * 
	 * @param processModel
	 * @param domainId
	 * @return
	 */
	ProcessModelDomainR getProcessModelDomainR(ID domainId, ProcessModelBean processModel);
}