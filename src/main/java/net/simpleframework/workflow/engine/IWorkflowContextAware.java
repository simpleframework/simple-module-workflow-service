package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.IModuleContextAware;
import net.simpleframework.ctx.ModuleContextFactory;
import net.simpleframework.workflow.engine.participant.IParticipantModel;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowContextAware extends IModuleContextAware {

	static final IWorkflowContext context = ModuleContextFactory.get(IWorkflowContext.class);

	static final IProcessModelService mService = context.getProcessModelService();
	static final IProcessService pService = context.getProcessService();
	static final IActivityService aService = context.getActivityService();
	static final IWorkitemService wService = context.getWorkitemService();
	static final IParticipantModel permission = context.getParticipantService();
}
