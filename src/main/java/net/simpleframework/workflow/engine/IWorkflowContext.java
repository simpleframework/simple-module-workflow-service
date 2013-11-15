package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.service.ado.db.IDbModuleContext;
import net.simpleframework.workflow.engine.participant.IParticipantModel;
import net.simpleframework.workflow.engine.remote.IProcessRemote;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowContext extends IDbModuleContext {

	static final String MODULE_NAME = "simple-workflow";

	/**
	 * 模型管理器
	 * 
	 * @return
	 */
	IProcessModelService getModelService();

	/**
	 * 流程实例管理器
	 * 
	 * @return
	 */
	IProcessService getProcessService();

	/**
	 * 任务环节管理器
	 * 
	 * @return
	 */
	IActivityService getActivityService();

	/**
	 * 工作列表管理器
	 * 
	 * @return
	 */
	IWorkitemService getWorkitemService();

	/**
	 * 参与者模型接口
	 * 
	 * @return
	 */
	IParticipantModel getParticipantService();

	IProcessRemote getRemoteService();
}
