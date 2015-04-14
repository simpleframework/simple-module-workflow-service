package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.ado.db.IDbEntityTableRegistry;
import net.simpleframework.ctx.AbstractADOModuleContext;
import net.simpleframework.ctx.IApplicationContext;
import net.simpleframework.ctx.Module;
import net.simpleframework.ctx.permission.IPermissionHandler;
import net.simpleframework.ctx.settings.ContextSettings;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityLobBean;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.IActivityService;
import net.simpleframework.workflow.engine.IDelegationService;
import net.simpleframework.workflow.engine.IProcessModelDomainRService;
import net.simpleframework.workflow.engine.IProcessModelService;
import net.simpleframework.workflow.engine.IProcessService;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.IWorkitemService;
import net.simpleframework.workflow.engine.IWorkviewService;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessLobBean;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelDomainR;
import net.simpleframework.workflow.engine.ProcessModelLobBean;
import net.simpleframework.workflow.engine.VariableBean;
import net.simpleframework.workflow.engine.VariableLogBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkviewBean;
import net.simpleframework.workflow.engine.ext.IWfCommentLogService;
import net.simpleframework.workflow.engine.ext.IWfCommentService;
import net.simpleframework.workflow.engine.ext.IWfUserCommentService;
import net.simpleframework.workflow.engine.ext.WfComment;
import net.simpleframework.workflow.engine.ext.WfCommentLog;
import net.simpleframework.workflow.engine.ext.WfCommentLogService;
import net.simpleframework.workflow.engine.ext.WfCommentService;
import net.simpleframework.workflow.engine.ext.WfUserCommentService;
import net.simpleframework.workflow.engine.participant.IWorkflowPermissionHandler;
import net.simpleframework.workflow.engine.remote.DefaultProcessRemote;
import net.simpleframework.workflow.engine.remote.IProcessRemote;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class WorkflowContext extends AbstractADOModuleContext implements IWorkflowContext,
		IDbEntityTableRegistry {
	/* 机构管理员角色 */
	public static String ROLE_WORKFLOW_MANAGER;

	@Override
	public void onInit(final IApplicationContext application) throws Exception {
		super.onInit(application);

		getTaskExecutor().execute(new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				// 初始化所有服务
				AbstractWorkflowService.doStartup();
			}
		});
	}

	@Override
	public Package[] getImportPackages() {
		return null;
	}

	@Override
	public DbEntityTable[] createEntityTables() {
		return new DbEntityTable[] { new DbEntityTable(ProcessModelBean.class, "sf_workflow_model"),
				new DbEntityTable(ProcessModelLobBean.class, "sf_workflow_model_lob").setNoCache(true),
				new DbEntityTable(ProcessModelDomainR.class, "sf_workflow_model_domain"),
				new DbEntityTable(ProcessBean.class, "sf_workflow_process"),
				new DbEntityTable(ProcessLobBean.class, "sf_workflow_process_lob").setNoCache(true),
				new DbEntityTable(DelegationBean.class, "sf_workflow_delegation"),
				new DbEntityTable(ActivityBean.class, "sf_workflow_activity"),
				new DbEntityTable(ActivityLobBean.class, "sf_workflow_activity_lob").setNoCache(true),
				new DbEntityTable(WorkitemBean.class, "sf_workflow_workitem"),
				new DbEntityTable(WorkviewBean.class, "sf_workflow_workview"),
				new DbEntityTable(VariableBean.class, "sf_workflow_variable"),
				new DbEntityTable(VariableLogBean.class, "sf_workflow_variable_log"),
				new DbEntityTable(WfComment.class, "sf_workflow_comment"),
				new DbEntityTable(WfCommentLog.class, "sf_workflow_comment_log") };
	}

	@Override
	protected Module createModule() {
		return new Module() {
			@Override
			public String getManagerRole() {
				return ROLE_WORKFLOW_MANAGER;
			}
		}.setName(MODULE_NAME).setText($m("WorkflowContext.0")).setOrder(31);
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
	public IWfCommentService getCommentService() {
		return singleton(WfCommentService.class);
	}

	@Override
	public IWfUserCommentService getWfUserCommentService() {
		return singleton(WfUserCommentService.class);
	}

	@Override
	public IWfCommentLogService getCommentLogService() {
		return singleton(WfCommentLogService.class);
	}

	@Override
	public IProcessRemote getRemoteService() {
		return singleton(DefaultProcessRemote.class);
	}

	@Override
	public IWorkflowPermissionHandler getParticipantService() {
		IPermissionHandler pHandler;
		return ((pHandler = getPermission()) instanceof IWorkflowPermissionHandler ? (IWorkflowPermissionHandler) pHandler
				: null);
	}

	@Override
	public ContextSettings getContextSettings() {
		return singleton(WorkflowSettings.class);
	}
}
