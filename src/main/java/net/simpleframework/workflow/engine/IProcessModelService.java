package net.simpleframework.workflow.engine;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.schema.ProcessDocument;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IProcessModelService extends IDbBeanService<ProcessModelBean>,
		IListenerAware<ProcessModelBean>, IScriptAware<ProcessModelBean> {

	/**
	 * 获取模型的文档对象
	 * 
	 * @param processModel
	 * @return
	 */
	ProcessDocument getProcessDocument(ProcessModelBean processModel);

	ProcessModelBean addModel(ID userId, ProcessDocument processDocument);

	void updateModel(ProcessModelBean processModel, ID userId, char[] model);

	/**
	 * 根据名称获取ProcessModelBean实例
	 * 
	 * @param name
	 * @return
	 */
	ProcessModelBean getProcessModelByName(String name);

	/**
	 * 根据名字或则id获取模型，如果不存在，则抛异常
	 * 
	 * @param model
	 *           名字或则id
	 * @return
	 */
	ProcessModelBean getProcessModel(String model);

	/**
	 * 
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessModelBean> getModelList(EProcessModelStatus... status);

	/**
	 * 
	 * @param userId
	 * @return
	 */
	InitiateItems getInitiateItems(ID userId);

	/**
	 * 判断用户是否可以启动指定的流程模型
	 * 
	 * @param userId
	 * @param model
	 *           实例或者是id
	 * @return
	 */
	boolean isStartProcess(ID userId, Object model);

	/**
	 * 设置模型的状态
	 * 
	 * @param processModel
	 * @param status
	 */
	void setStatus(ProcessModelBean processModel, EProcessModelStatus status);
}
