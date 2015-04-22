package net.simpleframework.workflow.engine;

import net.simpleframework.workflow.engine.comment.IWfCommentService;
import net.simpleframework.workflow.engine.comment.IWfCommentUserService;
import net.simpleframework.workflow.engine.impl.WorkflowSettings;
import net.simpleframework.workflow.engine.participant.IWorkflowPermissionHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowServiceAware extends IWorkflowContextAware {

	static final WorkflowSettings wfSettings = (WorkflowSettings) workflowContext
			.getContextSettings();

	static final IProcessModelService mService = workflowContext.getProcessModelService();
	static final IProcessModelDomainRService drService = workflowContext
			.getProcessModelDomainRService();
	static final IProcessService pService = workflowContext.getProcessService();
	static final IActivityService aService = workflowContext.getActivityService();
	static final IWorkitemService wService = workflowContext.getWorkitemService();
	static final IWorkviewService vService = workflowContext.getWorkviewService();
	static final IDelegationService dService = workflowContext.getDelegationService();
	static final IUserStatService usService = workflowContext.getUserStatService();

	static final IWfCommentService commentService = workflowContext.getCommentService();
	static final IWfCommentUserService commentUserService = workflowContext.getCommentUserService();

	static final IWorkflowPermissionHandler permission = workflowContext.getParticipantService();
}
