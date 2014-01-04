package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.ado.db.DbManagerFactory;
import net.simpleframework.ado.db.IDbEntityTableRegistry;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.ctx.AbstractADOModuleContext;
import net.simpleframework.ctx.IApplicationContext;
import net.simpleframework.ctx.Module;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityLobBean;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.IActivityService;
import net.simpleframework.workflow.engine.IProcessModelService;
import net.simpleframework.workflow.engine.IProcessService;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.IWorkitemService;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessLobBean;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelLobBean;
import net.simpleframework.workflow.engine.VariableBean;
import net.simpleframework.workflow.engine.VariableLogBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.participant.IParticipantModel;
import net.simpleframework.workflow.engine.remote.DefaultProcessRemote;
import net.simpleframework.workflow.engine.remote.IProcessRemote;
import net.simpleframework.workflow.schema.AbstractTaskNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class WorkflowContext extends AbstractADOModuleContext implements IWorkflowContext,
		IDbEntityTableRegistry {

	@Override
	public void onInit(final IApplicationContext application) throws Exception {
		super.onInit(application);

		// 引擎的初始化
		getTaskExecutor().execute(new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				// 启动子流程监控
				final IDataQuery<?> qs = ((DbManagerFactory) getADOManagerFactory()).getEntityManager(
						ActivityBean.class).queryBeans(
						new ExpressionValue("tasknodeType=? and (status=? or status=?)",
								AbstractTaskNode.SUBNODE_TYPE, EActivityStatus.running,
								EActivityStatus.waiting));
				final IActivityService service = getActivityService();
				ActivityBean activity;
				while ((activity = (ActivityBean) qs.next()) != null) {
					service.doRemoteSubTask(activity);
				}
			}
		});
	}

	@Override
	public DbEntityTable[] createEntityTables() {
		return new DbEntityTable[] { new DbEntityTable(ProcessModelBean.class, "sf_workflow_model"),
				new DbEntityTable(ProcessModelLobBean.class, "sf_workflow_model_lob").setNoCache(true),
				new DbEntityTable(ProcessBean.class, "sf_workflow_process"),
				new DbEntityTable(ProcessLobBean.class, "sf_workflow_process_lob").setNoCache(true),
				new DbEntityTable(DelegationBean.class, "sf_workflow_delegation"),
				new DbEntityTable(ActivityBean.class, "sf_workflow_activity"),
				new DbEntityTable(ActivityLobBean.class, "sf_workflow_activity_lob").setNoCache(true),
				new DbEntityTable(WorkitemBean.class, "sf_workflow_workitem"),
				new DbEntityTable(VariableBean.class, "sf_workflow_variable"),
				new DbEntityTable(VariableLogBean.class, "sf_workflow_variable_log") };
	}

	@Override
	protected Module createModule() {
		return new Module().setName(MODULE_NAME).setText($m("WorkflowContext.0")).setOrder(31);
	}

	@Override
	public IProcessModelService getProcessModelService() {
		return singleton(ProcessModelService.class);
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
	public IProcessRemote getRemoteService() {
		return singleton(DefaultProcessRemote.class);
	}

	@Override
	public IParticipantModel getParticipantService() {
		return (IParticipantModel) singleton("net.simpleframework.workflow.web.DefaultParticipantModel");
	}
}
