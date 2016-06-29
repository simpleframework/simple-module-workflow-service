package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.AbstractADOModuleContext;
import net.simpleframework.ctx.IApplicationContext;
import net.simpleframework.ctx.Module;
import net.simpleframework.ctx.permission.IPermissionHandler;
import net.simpleframework.ctx.settings.ContextSettings;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.IActivityService;
import net.simpleframework.workflow.engine.IDelegationService;
import net.simpleframework.workflow.engine.IProcessModelDomainRService;
import net.simpleframework.workflow.engine.IProcessModelService;
import net.simpleframework.workflow.engine.IProcessService;
import net.simpleframework.workflow.engine.IUserStatService;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.IWorkitemService;
import net.simpleframework.workflow.engine.IWorkviewSentService;
import net.simpleframework.workflow.engine.IWorkviewService;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.ProcessLobBean;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.ProcessModelDomainR;
import net.simpleframework.workflow.engine.bean.ProcessModelLobBean;
import net.simpleframework.workflow.engine.bean.UserStatBean;
import net.simpleframework.workflow.engine.bean.VariableBean;
import net.simpleframework.workflow.engine.bean.VariableLogBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.bean.WorkviewBean;
import net.simpleframework.workflow.engine.bean.WorkviewSentBean;
import net.simpleframework.workflow.engine.comment.IWfCommentLogService;
import net.simpleframework.workflow.engine.comment.IWfCommentService;
import net.simpleframework.workflow.engine.comment.IWfCommentUserService;
import net.simpleframework.workflow.engine.comment.WfComment;
import net.simpleframework.workflow.engine.comment.WfCommentLog;
import net.simpleframework.workflow.engine.comment.WfCommentLogService;
import net.simpleframework.workflow.engine.comment.WfCommentService;
import net.simpleframework.workflow.engine.comment.WfCommentUser;
import net.simpleframework.workflow.engine.comment.WfCommentUserService;
import net.simpleframework.workflow.engine.notice.IWfNoticeService;
import net.simpleframework.workflow.engine.notice.WfNoticeBean;
import net.simpleframework.workflow.engine.notice.WfNoticeService;
import net.simpleframework.workflow.engine.participant.IWorkflowPermissionHandler;
import net.simpleframework.workflow.engine.remote.DefaultProcessRemoteHandler;
import net.simpleframework.workflow.engine.remote.IProcessRemoteHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class WorkflowContext extends AbstractADOModuleContext implements IWorkflowContext {
	/* 机构管理员角色 */
	public static String ROLE_WORKFLOW_MANAGER;

	@Override
	public Package[] getScriptImportPackages() {
		return null;
	}

	@Override
	protected DbEntityTable[] createEntityTables() {
		return new DbEntityTable[] { new DbEntityTable(ProcessModelBean.class, "sf_workflow_model"),
				new DbEntityTable(ProcessModelLobBean.class, "sf_workflow_model_lob").setNoCache(true),
				new DbEntityTable(ProcessModelDomainR.class, "sf_workflow_model_domain"),

				new DbEntityTable(ProcessBean.class, "sf_workflow_process"),
				new DbEntityTable(ProcessLobBean.class, "sf_workflow_process_lob").setNoCache(true),

				new DbEntityTable(DelegationBean.class, "sf_workflow_delegation"),

				new DbEntityTable(ActivityBean.class, "sf_workflow_activity"),

				new DbEntityTable(WorkitemBean.class, "sf_workflow_workitem"),

				new DbEntityTable(WorkviewSentBean.class, "sf_workflow_workview_sent"),
				new DbEntityTable(WorkviewBean.class, "sf_workflow_workview"),

				new DbEntityTable(UserStatBean.class, "sf_workflow_userstat"),

				new DbEntityTable(VariableBean.class, "sf_workflow_variable"),
				new DbEntityTable(VariableLogBean.class, "sf_workflow_variable_log"),

				new DbEntityTable(WfNoticeBean.class, "sf_workflow_notice"),

				new DbEntityTable(WfComment.class, "sf_workflow_comment"),
				new DbEntityTable(WfCommentUser.class, "sf_workflow_comment_user"),
				new DbEntityTable(WfCommentLog.class, "sf_workflow_comment_log") };
	}

	@Override
	public ContextSettings getContextSettings() {
		return singleton(WorkflowSettings.class);
	}

	@Override
	protected Module createModule() {
		return super.createModule().setName(MODULE_NAME).setText($m("WorkflowContext.0"))
				.setOrder(31);
	}

	@Override
	protected String getManagerRole(final KVMap vars) {
		return ROLE_WORKFLOW_MANAGER;
	}

	@Override
	public IProcessModelService getProcessModelService() {
		return singleton(ProcessModelService.class);
	}

	@Override
	public IProcessModelDomainRService getProcessModelDomainRService() {
		return singleton(ProcessModelDomainRService.class);
	}

	@Override
	public IProcessService getProcessService() {
		return singleton(ProcessService.class);
	}

	@Override
	public IActivityService getActivityService() {
		return singleton(ActivityService.class);
	}

	@Override
	public IWorkitemService getWorkitemService() {
		return singleton(WorkitemService.class);
	}

	@Override
	public IDelegationService getDelegationService() {
		return singleton(DelegationService.class);
	}

	@Override
	public IWorkviewService getWorkviewService() {
		return singleton(WorkviewService.class);
	}

	@Override
	public IWorkviewSentService getWorkviewSentService() {
		return singleton(WorkviewSentService.class);
	}

	@Override
	public IUserStatService getUserStatService() {
		return singleton(UserStatService.class);
	}

	@Override
	public IWfCommentService getCommentService() {
		return singleton(WfCommentService.class);
	}

	@Override
	public IWfCommentUserService getCommentUserService() {
		return singleton(WfCommentUserService.class);
	}

	@Override
	public IWfCommentLogService getCommentLogService() {
		return singleton(WfCommentLogService.class);
	}

	@Override
	public IWfNoticeService getNoticeService() {
		return singleton(WfNoticeService.class);
	}

	// ------------------------------ hanlder

	@Override
	public IProcessRemoteHandler getProcessRemoteHandler() {
		return singleton(DefaultProcessRemoteHandler.class);
	}

	@Override
	public IWorkflowPermissionHandler getPermissionHandler() {
		final IPermissionHandler pHandler = getPermission();
		if (!(pHandler instanceof IWorkflowPermissionHandler)) {
			throw WorkflowException.of("must impl: " + IWorkflowPermissionHandler.class.getName());
		}
		return (IWorkflowPermissionHandler) pHandler;
	}

	@Override
	public void onInit(final IApplicationContext application) throws Exception {
		super.onInit(application);

		// 预初始化服务
		oprintln("[Service onInit] " + IWorkflowContextAware.workflowContext);
	}
}
