package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.IModuleContextAware;
import net.simpleframework.ctx.ModuleContextFactory;
import net.simpleframework.workflow.engine.impl.WorkflowSettings;
import net.simpleframework.workflow.engine.participant.IWorkflowPermissionHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowContextAware extends IModuleContextAware {

	static final IWorkflowContext workflowContext = ModuleContextFactory.get(IWorkflowContext.class);

	static final WorkflowSettings settings = (WorkflowSettings) workflowContext.getContextSettings();

	static final IProcessModelService mService = workflowContext.getProcessModelService();
	static final IProcessService pService = workflowContext.getProcessService();
	static final IActivityService aService = workflowContext.getActivityService();
	static final IWorkitemService wService = workflowContext.getWorkitemService();
	static final IDelegationService dService = workflowContext.getDelegationService();
	static final IWorkflowPermissionHandler permission = workflowContext.getParticipantService();
}
