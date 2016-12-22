package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.ado.trans.TransactionVoidCallback;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.ctx.task.ExecutorRunnableEx;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IProcessService;
import net.simpleframework.workflow.engine.IWorkflowView;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.ProcessLobBean;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.ProcessModelDomainR;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.event.IProcessListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.remote.IProcessRemoteHandler;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.ProcessDocument;
import net.simpleframework.workflow.schema.ProcessNode;
import net.simpleframework.workflow.schema.StartNode;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessService extends AbstractWorkflowService<ProcessBean>
		implements IProcessService {

	@Override
	public ProcessModelBean getProcessModel(final ProcessBean process) {
		return process == null ? null : wfpmService.getBean(process.getModelId());
	}

	@Override
	public ProcessDocument getProcessDocument(final ProcessBean process) {
		if (process == null) {
			return null;
		}
		final ProcessDocument doc = process.getAttrCache(ATTR_PROCESS_DOCUMENT,
				new CacheV<ProcessDocument>() {
					@Override
					public ProcessDocument get() {
						final ProcessLobBean lob = getEntityManager(ProcessLobBean.class)
								.getBean(process.getId());
						return lob != null ? new ProcessDocument(lob.getProcessSchema()) : null;
					}
				});
		return doc != null ? doc : wfpmService.getProcessDocument(getProcessModel(process));
	}

	/**
	 * 获取流程的定义
	 * 
	 * @param process
	 * @return
	 */
	@Override
	public ProcessNode getProcessNode(final ProcessBean process) {
		return getProcessDocument(process).getProcessNode();
	}

	@Override
	public ProcessBean doStartProcess(final InitiateItem initiateItem, final String topic) {
		final ProcessModelBean processModel = initiateItem.model();
		if (processModel == null) {
			throw WorkflowException.of($m("ProcessService.3"));
		}

		final ProcessBean process = _startProcess(processModel, initiateItem.getParticipant(),
				initiateItem.getVariables(), null, topic);
		// 事件
		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onCreated(initiateItem, process);
		}
		_createStartNode(process, initiateItem.getTransitions());
		return process;
	}

	@Override
	public ProcessBean doStartProcess(final InitiateItem initiateItem) {
		return doStartProcess(initiateItem, null);
	}

	@Override
	public ProcessBean doStartProcess(final ProcessModelBean processModel,
			final Participant participant, final KVMap variables, final Properties properties,
			final String topic) {
		final ProcessBean process = _startProcess(processModel, participant, variables, properties,
				topic);
		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onCreated(null, process);
		}
		_createStartNode(process, null);
		return process;
	}

	private void _createStartNode(final ProcessBean process,
			final List<TransitionNode> transitions) {
		// 创建开始任务
		final StartNode startNode = getProcessNode(process).startNode();
		final ActivityService wfaServiceImpl = (ActivityService) wfaService;

		// 如果设置了用户委托，则无法启动流程
		final DelegationBean delegation = wfdService.queryRunningDelegation(process.getUserId());
		if (delegation != null) {
			throw WorkflowException.of($m("ProcessService.2"));
		}

		final ActivityBean sActivity = wfaServiceImpl._create(process, startNode, null, new Date());
		wfaServiceImpl.insert(sActivity);
		if (transitions == null) {
			new ActivityComplete(sActivity).complete();
		} else {
			new ActivityComplete(sActivity, transitions).complete();
		}
	}

	private ProcessBean _startProcess(final ProcessModelBean processModel,
			final Participant participant, final Map<String, Object> variables,
			final Properties properties, final String title) {
		if (processModel.getStatus() != EProcessModelStatus.deploy) {
			throw WorkflowException.of($m("ProcessService.1"));
		}

		final ProcessBean process = _create(processModel, participant, title);
		if (properties != null) {
			process.getProperties().putAll(properties);
		}

		insert(process);

		if (variables != null && variables.size() > 0) {
			// 绑定流程变量
			final Collection<String> names = getVariableNames(process);
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				final String key = e.getKey();
				if (names.contains(key)) {
					setVariable(process, key, e.getValue());
				}
			}
		}

		final ProcessDocument doc = wfpmService.getProcessDocument(processModel);
		if (!doc.getProcessNode().isInstanceShared()) {
			final ProcessLobBean lob = new ProcessLobBean();
			lob.setId(process.getId());
			lob.setProcessSchema(doc.toString().toCharArray());
			getEntityManager(ProcessLobBean.class).insert(lob);
		}
		return process;
	}

	@Override
	public void doBackToRemote(final ProcessBean process) {
		final ITaskExecutor taskExecutor = workflowContext.getTaskExecutor();
		// 如果该流程是一个子流程，将启动该任务，该任务周期运行，以保障主流程的任务环节完成
		taskExecutor.addScheduledTask(new ExecutorRunnableEx("activity_subprocess_back") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				final Properties properties = process.getProperties();
				final KVMap data = new KVMap(); // 提交的参数
				data.add(IProcessRemoteHandler.SUB_ACTIVITYID,
						properties.getProperty(IProcessRemoteHandler.SUB_ACTIVITYID));

				for (final String mapping : StringUtils
						.split(properties.getProperty(IProcessRemoteHandler.VAR_MAPPINGS))) {
					data.add(mapping, wfpService.getVariable(process, mapping));
				}

				final IProcessRemoteHandler rHandler = workflowContext.getProcessRemoteHandler();
				final Map<String, Object> r = rHandler.call(
						properties.getProperty(IProcessRemoteHandler.SERVERURL), "subComplete", data);
				final Boolean success = (Boolean) r.get("success");
				if (success != null && success.booleanValue()) {
					taskExecutor.removeScheduledTask(this);
				}
			}
		});
	}

	@Override
	public IDataQuery<ProcessBean> getProcessList(final ID domainId,
			final ProcessModelBean processModel, final String topic, final EProcessStatus... status) {
		final StringBuilder sql = new StringBuilder("1=1");
		final ArrayList<Object> params = new ArrayList<Object>();
		if (domainId != null) {
			sql.append(" and domainId=?");
			params.add(domainId);
		}
		if (processModel != null) {
			sql.append(" and modelId=?");
			params.add(processModel.getId());
		}
		if (StringUtils.hasText(topic)) {
			sql.append(" and title like '%").append(SqlUtils.sqlEscape(topic)).append("%'");
		}
		buildStatusSQL(sql, params, status);
		sql.append(" order by createdate desc");
		return query(sql, params.toArray());
	}

	@Override
	public IDataQuery<ProcessBean> getProcessWlist(final ID userId,
			final ProcessModelBean processModel, final String topic, final EProcessStatus... status) {
		if (userId == null) {
			return DataQueryUtils.nullQuery();
		}
		final StringBuilder sql = new StringBuilder();
		final List<Object> params = ArrayUtils.toParams(userId);
		sql.append("select p.*, w.c from (");
		sql.append("select processid, count(*) as c from ").append(getTablename(WorkitemBean.class));
		sql.append(" where userid2=? group by processid");
		sql.append(") w left join ").append(getTablename(ProcessBean.class));
		sql.append(" p on p.id=w.processid where 1=1");
		if (processModel != null) {
			sql.append(" and p.modelId=?");
			params.add(processModel.getId());
		}
		if (StringUtils.hasText(topic)) {
			sql.append(" and p.title like '%").append(SqlUtils.sqlEscape(topic)).append("%'");
		}
		buildStatusSQL(sql, params, "p", status);
		sql.append(" order by p.createdate desc");
		return query(new SQLValue(sql, params.toArray()));
	}

	@Override
	public IDataQuery<ProcessBean> getProcessWlistInDept(final ID[] deptIds,
			final ProcessModelBean processModel, final String topic, final EProcessStatus... status) {
		if (deptIds == null || deptIds.length == 0) {
			return DataQueryUtils.nullQuery();
		}
		final StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < deptIds.length; i++) {
			if (i > 0) {
				sb.append(" or ");
			}
			sb.append("deptid=?");
		}
		sb.append(")");
		return query(toProcessListSQLValue(sb.toString(), deptIds, processModel, topic, status));
	}

	@Override
	public IDataQuery<ProcessBean> getProcessWlistInDomain(final ID domainId,
			final ProcessModelBean processModel, final String topic, final EProcessStatus... status) {
		if (domainId == null) {
			return DataQueryUtils.nullQuery();
		}
		return query(toProcessListSQLValue("domainid=?", new Object[] { domainId }, processModel,
				topic, status));
	}

	private SQLValue toProcessListSQLValue(final String expr, final Object[] params,
			final ProcessModelBean processModel, final String topic, final EProcessStatus... status) {
		final StringBuilder sql = new StringBuilder();
		final List<Object> _params = ArrayUtils.toParams(params);
		sql.append("select p.*, w.c from (");
		sql.append("select processid, count(*) as c from ").append(getTablename(WorkitemBean.class));
		sql.append(" where ").append(expr).append(" group by processid");
		sql.append(") w left join ").append(getTablename(ProcessBean.class));
		sql.append(" p on p.id=w.processid where 1=1");
		if (processModel != null) {
			sql.append(" and p.modelId=?");
			_params.add(processModel.getId());
		}
		if (StringUtils.hasText(topic)) {
			sql.append(" and p.title like '%").append(SqlUtils.sqlEscape(topic)).append("%'");
		}
		buildStatusSQL(sql, _params, "p", status);
		sql.append(" order by p.createdate desc");
		return new SQLValue(sql, _params.toArray());
	}

	@Override
	public void doSuspend(final ProcessBean process) {
		_assert(process, EProcessStatus.running);
		_status(process, EProcessStatus.suspended);
	}

	@Override
	public void doResume(final ProcessBean process) {
		_assert(process, EProcessStatus.suspended);
		_status(process, EProcessStatus.running);
	}

	@Override
	public void doAbort(final ProcessBean process, final EProcessAbortPolicy policy) {
		if (policy == EProcessAbortPolicy.allActivities) {
			final ActivityService wfaServiceImpl = (ActivityService) wfaService;
			for (final ActivityBean activity : wfaServiceImpl.getActivities(process)) {
				wfaServiceImpl._abort(activity);
			}
		}

		_status(process, EProcessStatus.abort);
	}

	@Override
	public Map<String, Object> createVariables(final ProcessBean process) {
		final Map<String, Object> variables = wfpmService.createVariables(getProcessModel(process));
		variables.put("process", process);
		for (final String variable : getVariableNames(process)) {
			variables.put(variable, getVariable(process, variable));
		}
		return variables;
	}

	@Override
	public Object getVariable(final ProcessBean process, final String name) {
		final VariableNode variableNode = getProcessNode(process).getVariableNodeByName(name);
		return vServiceImpl.getVariableValue(process, variableNode);
	}

	@Override
	public void setVariable(final ProcessBean process, final String name, final Object value) {
		setVariable(process, new String[] { name }, new Object[] { value });
	}

	@Override
	public void setVariable(final ProcessBean process, final String[] names, final Object[] values) {
		vServiceImpl.setVariableValue(process, names, values);
	}

	@Override
	public Collection<String> getVariableNames(final ProcessBean process) {
		return getProcessNode(process).variables().keySet();
	}

	@Override
	public WorkitemBean getFirstWorkitem(final ProcessBean process) {
		final ActivityBean startActivity = wfaService.getStartActivity(process);
		for (final ActivityBean activity : wfaService.getNextActivities(startActivity)) {
			final List<WorkitemBean> list = wfwService.getWorkitems(activity, EWorkitemStatus.running);
			if (list.size() > 0) {
				return list.get(0);
			}
		}
		throw WorkflowException.of($m("ProcessService.0"));
	}

	@Override
	public boolean isFinalStatus(final ProcessBean t) {
		return _isFinalStatus(t.getStatus());
	}

	private boolean _isFinalStatus(final EProcessStatus status) {
		return status.ordinal() >= EProcessStatus.complete.ordinal();
	}

	@Override
	public void doUpdateKV(final ProcessBean process, final Map<String, Object> kv) {
		if (process == null || kv.size() == 0) {
			return;
		}
		final Set<String> keys = kv.keySet();
		for (final String key : keys) {
			BeanUtils.setProperty(process, key, kv.get(key));
		}
		update(keys.toArray(new String[keys.size()]), process);
	}

	@Override
	public synchronized void doUpdateViews(final ProcessBean process) {
		if (process != null) {
			process.setViews(process.getViews() + 1);
			update(new String[] { "views" }, process);
		}
	}

	@Override
	public void doUpdateTimeoutDate(final ProcessBean process, final Date timeoutDate) {
		process.setTimeoutDate(timeoutDate);
		update(new String[] { "timeoutDate" }, process);
	}

	@Override
	public void doUpdateTimeoutDate(final ProcessBean process, final int hours) {
		doUpdateTimeoutDate(process,
				getWorkCalendarListener(process).getRealDate(process, hours * 60));
	}

	ProcessBean _create(final ProcessModelBean processModel, final Participant participant,
			final String title) {
		final ProcessBean process = createBean();
		process.setModelId(processModel.getId());
		process.setModelName(processModel.getModelName());
		process.setTitle(title);
		process.setCreateDate(new Date());
		final ProcessDocument doc = wfpmService.getProcessDocument(processModel);
		process.setVersion(doc.getProcessNode().getVersion().toString());

		final PermissionUser user = participant.getUser();
		process.setUserId(user.getId());
		process.setUserText(user.getText());
		process.setDeptId(participant.getDeptId());
		process.setDomainId(user.getDomainId());
		process.setRoleId(participant.getRoleId());
		return process;
	}

	@Override
	public void doRunning(final ProcessBean process) {
		final EProcessStatus status = process.getStatus();
		if (status != EProcessStatus.complete) {
			throw WorkflowStatusException.of(process, status, EProcessStatus.complete);
		}
		final ActivityBean end = wfaService.query("processId=? and tasknodeType=? and status=?",
				process.getId(), AbstractTaskNode.TT_END, EActivityStatus.complete).next();
		wfaService.doFallback(end);

		process.setAttr("_status", Boolean.TRUE);
		process.setStatus(EProcessStatus.running);
		update(new String[] { "status" }, process);
	}

	@Override
	public IWorkflowView getWorkflowView(final ProcessBean process) {
		if (process == null) {
			return null;
		}
		final String viewClass = getProcessDocument(process).getProcessNode().getViewClass();
		return (IWorkflowView) (viewClass != null ? singleton(viewClass) : null);
	}

	void _doProcessTimeout() {
		final IDataQuery<ProcessBean> dq = query("timeoutdate is not null and (status=? or status=?)",
				EProcessStatus.running, EProcessStatus.timeout).setFetchSize(0);
		ProcessBean process;
		final Date n = new Date();
		while ((process = dq.next()) != null) {
			final ProcessBean nProcess = process;
			doExecuteTransaction(new TransactionVoidCallback() {
				@Override
				protected void doTransactionVoidCallback() throws Throwable {
					final EProcessStatus status = nProcess.getStatus();
					if (status != EProcessStatus.timeout && n.after(nProcess.getTimeoutDate())) {
						// 设置过期状态
						nProcess.setStatus(EProcessStatus.timeout);
						update(new String[] { "status" }, nProcess);
					}

					// 触发超期检测事件，比如一些通知
					for (final IWorkflowListener listener : getEventListeners(nProcess)) {
						((IProcessListener) listener).onTimeoutCheck(nProcess);
					}
				}
			});
		}
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 启动过期监控
		final ITaskExecutor taskExecutor = workflowContext.getTaskExecutor();
		taskExecutor.addScheduledTask(new ExecutorRunnableEx("process_timeout_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				_doProcessTimeout();
			}
		});

		addListener(new DbEntityAdapterEx<ProcessBean>() {
			@Override
			public void onAfterInsert(final IDbEntityManager<ProcessBean> manager,
					final ProcessBean[] beans) throws Exception {
				super.onAfterInsert(manager, beans);
				for (final ProcessBean bean : beans) {
					// 更新流程实例计数
					updateProcessCount(bean);
				}
			}

			@Override
			public void onAfterDelete(final IDbEntityManager<ProcessBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onAfterDelete(manager, paramsValue);
				for (final ProcessBean process : coll(manager, paramsValue)) {
					updateProcessCount(process);
				}
			}

			private void updateProcessCount(final ProcessBean process) {
				// 更新流程实例计数
				final ProcessModelBean processModel = getProcessModel(process);
				processModel.setProcessCount(getProcessList(null, processModel, "").getCount());
				wfpmService.update(new String[] { "processCount" }, processModel);

				final ID domainId = process.getDomainId();
				final ProcessModelDomainR r = wfpmdService.getProcessModelDomainR(domainId,
						processModel);
				r.setProcessCount(getProcessList(domainId, processModel, "").getCount());
				wfpmdService.update(new String[] { "processCount" }, r);
			}

			@Override
			public void onBeforeDelete(final IDbEntityManager<ProcessBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onBeforeDelete(manager, paramsValue);

				for (final ProcessBean process : coll(manager, paramsValue)) {
					if (!isFinalStatus(process)) {
						throw WorkflowException.of($m("ProcessService.5"));
					}

					final Object id = process.getId();
					// 触发删除事件
					for (final IWorkflowListener listener : getEventListeners(process)) {
						((IProcessListener) listener).onDelete(process);
					}

					// 删除lob
					getEntityManager(ProcessLobBean.class).delete(new ExpressionValue("id=?", id));

					// 删除任务环节
					wfaService.deleteWith("processId=?", id);

					// 删除待阅
					wfvService.deleteWith("processId=?", id);
					wfvsService.deleteWith("processId=?", id);

					// 删除流程变量
					vServiceImpl.deleteVariables(EVariableSource.process, id);
				}
			}

			@Override
			public void onBeforeUpdate(final IDbEntityManager<ProcessBean> manager,
					final String[] columns, final ProcessBean[] beans) throws Exception {
				super.onAfterUpdate(manager, columns, beans);

				if (ArrayUtils.isEmpty(columns) || ArrayUtils.contains(columns, "status", true)) {
					for (final ProcessBean process : beans) {
						// 状态转换事件
						final EProcessStatus _status = Convert.toEnum(EProcessStatus.class,
								queryFor("status", "id=?", process.getId()));
						if (!Convert.toBool(process.getAttr("_status")) && _isFinalStatus(_status)) {
							throw WorkflowException.of($m("ProcessService.4"));
						}

						if (_status != process.getStatus()) {
							for (final IWorkflowListener listener : getEventListeners(process)) {
								((IProcessListener) listener).onStatusChange(process, _status);
							}
						}
					}
				}
			}
		});
	}
}
