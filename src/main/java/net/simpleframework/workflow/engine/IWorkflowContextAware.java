package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.IModuleContextAware;
import net.simpleframework.ctx.ModuleContextFactory;
import net.simpleframework.workflow.engine.comment.IWfCommentLogService;
import net.simpleframework.workflow.engine.comment.IWfCommentService;
import net.simpleframework.workflow.engine.comment.IWfCommentUserService;
import net.simpleframework.workflow.engine.impl.WorkflowSettings;
import net.simpleframework.workflow.engine.notice.IWfNoticeService;
import net.simpleframework.workflow.engine.participant.IWorkflowPermissionHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowContextAware extends IModuleContextAware {

	static final IWorkflowContext workflowContext = ModuleContextFactory.get(IWorkflowContext.class);

	static final WorkflowSettings wfSettings = (WorkflowSettings) workflowContext
			.getContextSettings();

	/* service */
	static final IProcessModelService wfpmService = workflowContext.getProcessModelService();
	static final IProcessModelDomainRService wfpmdService = workflowContext
			.getProcessModelDomainRService();

	static final IProcessService wfpService = workflowContext.getProcessService();
	static final IActivityService wfaService = workflowContext.getActivityService();
	static final IWorkitemService wfwService = workflowContext.getWorkitemService();

	static final IWorkviewService wfvService = workflowContext.getWorkviewService();
	static final IDelegationService wfdService = workflowContext.getDelegationService();

	static final IUserStatService wfusService = workflowContext.getUserStatService();

	static final IWfCommentService wfcService = workflowContext.getCommentService();
	static final IWfCommentLogService wfclService = workflowContext.getCommentLogService();
	static final IWfCommentUserService wfcuService = workflowContext.getCommentUserService();

	static final IWfNoticeService wfnService = workflowContext.getNoticeService();

	static final IWorkflowPermissionHandler permission = workflowContext.getParticipantService();
}
