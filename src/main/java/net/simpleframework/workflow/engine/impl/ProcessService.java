package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.lucene.AbstractLuceneManager;
import net.simpleframework.ado.lucene.ILuceneManager;
import net.simpleframework.ado.lucene.LuceneDocument;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IProcessService;
import net.simpleframework.workflow.engine.IWorkflowView;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.ProcessLobBean;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.ProcessModelDomainR;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.event.IProcessListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.remote.IProcessRemote;
import net.simpleframework.workflow.schema.ProcessDocument;
import net.simpleframework.workflow.schema.ProcessNode;
import net.simpleframework.workflow.schema.StartNode;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessService extends AbstractWorkflowService<ProcessBean> implements IProcessService {

	@Override
	public ProcessModelBean getProcessModel(final ProcessBean process) {
		return process == null ? null : mService.getBean(process.getModelId());
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
						final ProcessLobBean lob = getEntityManager(ProcessLobBean.class).getBean(
								process.getId());
						return lob != null ? new ProcessDocument(lob.getProcessSchema()) : null;
					}
				});
		return doc != null ? doc : mService.getProcessDocument(getProcessModel(process));
	}

	/**
	 * 获取流程的定义
	 * 
	 * @param process
	 * @return
	 */
	ProcessNode _getProcessNode(final ProcessBean process) {
		return getProcessDocument(process).getProcessNode();
	}

	@Override
	public ProcessBean doStartProcess(final InitiateItem initiateItem, final String topic) {
		final ID roleId = initiateItem.getRoleId();
		if (roleId == null) {
			throw WorkflowException.of($m("ProcessService.2"));
		}
		final ProcessModelBean processModel = initiateItem.model();
		if (processModel == null) {
			throw WorkflowException.of($m("ProcessService.3"));
		}

		final ProcessBean process = _startProcess(processModel, initiateItem.getUserId(), roleId,
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
	public ProcessBean doStartProcess(final ProcessModelBean processModel, final KVMap variables,
			final Properties properties, final String topic) {
		final ProcessBean process = _startProcess(processModel, null, null, variables, properties,
				topic);
		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onCreated(null, process);
		}
		_createStartNode(process, null);
		return process;
	}

	private void _createStartNode(final ProcessBean process, final List<TransitionNode> transitions) {
		// 创建开始任务
		final StartNode startNode = _getProcessNode(process).startNode();
		final ActivityBean sActivity = aService._create(process, startNode, null, new Date());
		aService.insert(sActivity);
		if (transitions == null) {
			new ActivityComplete(sActivity).complete();
		} else {
			new ActivityComplete(sActivity, transitions).complete();
		}
	}

	private ProcessBean _startProcess(final ProcessModelBean processModel, final ID userId,
			final ID roleId, final Map<String, Object> variables, final Properties properties,
			final String title) {
		if (processModel.getStatus() != EProcessModelStatus.deploy) {
			throw WorkflowException.of($m("ProcessService.1"));
		}

		final ProcessBean process = _create(processModel, userId, roleId, title);
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

		final ProcessDocument doc = mService.getProcessDocument(processModel);
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
		taskExecutor.addScheduledTask(wfSettings.getSubActivityPeriod(), new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				final Properties properties = process.getProperties();
				final KVMap data = new KVMap(); // 提交的参数
				data.add(IProcessRemote.SUB_ACTIVITYID,
						properties.getProperty(IProcessRemote.SUB_ACTIVITYID));

				for (final String mapping : StringUtils.split(properties
						.getProperty(IProcessRemote.VAR_MAPPINGS))) {
					data.add(mapping, pService.getVariable(process, mapping));
				}

				final Map<String, Object> r = workflowContext.getRemoteService().call(
						properties.getProperty(IProcessRemote.SERVERURL), "subComplete", data);
				final Boolean success = (Boolean) r.get("success");
				if (success != null && success.booleanValue()) {
					taskExecutor.removeScheduledTask(this);
				}
			}
		});
	}

	@Override
	public IDataQuery<ProcessBean> getProcessList(final ID domainId,
			final ProcessModelBean processModel, final EProcessStatus... status) {
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
		buildStatusSQL(sql, params, null, status);
		sql.append(" order by createdate desc");
		return query(sql.toString(), params.toArray());
	}

	@Override
	public IDataQuery<ProcessBean> getProcessList(final ID userId, final EProcessStatus... status) {
		if (userId == null) {
			return DataQueryUtils.nullQuery();
		}
		final StringBuilder sql = new StringBuilder();
		final List<Object> params = ArrayUtils.toParams(userId);
		sql.append("select p.*, w.c from (");
		sql.append("select processid, count(*) as c from ").append(getTablename(WorkitemBean.class));
		sql.append(" where userid=? group by processid");
		sql.append(") w left join ").append(getTablename(ProcessBean.class));
		sql.append(" p on p.id=w.processid where 1=1");
		buildStatusSQL(sql, params, "p", status);
		sql.append(" order by p.createdate desc");
		return query(new SQLValue(sql.toString(), params.toArray()));
	}

	@Override
	public IDataQuery<ProcessBean> getProcessListInDept(final ID deptId,
			final EProcessStatus... status) {
		if (deptId == null) {
			return DataQueryUtils.nullQuery();
		}
		final StringBuilder sql = new StringBuilder();
		final List<Object> params = ArrayUtils.toParams(deptId);
		sql.append("select p.*, w.c from (");
		sql.append("select processid, count(*) as c from ").append(getTablename(WorkitemBean.class));
		sql.append(" where deptid=? group by processid");
		sql.append(") w left join ").append(getTablename(ProcessBean.class));
		sql.append(" p on p.id=w.processid where 1=1");
		buildStatusSQL(sql, params, "p", status);
		sql.append(" order by p.createdate desc");
		return query(new SQLValue(sql.toString(), params.toArray()));
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
			for (final ActivityBean activity : aService.getActivities(process)) {
				aService._abort(activity);
			}
		}

		_status(process, EProcessStatus.abort);
	}

	@Override
	public Map<String, Object> createVariables(final ProcessBean process) {
		final Map<String, Object> variables = mService.createVariables(getProcessModel(process));
		variables.put("process", process);
		for (final String variable : getVariableNames(process)) {
			variables.put(variable, getVariable(process, variable));
		}
		return variables;
	}

	@Override
	public Object getVariable(final ProcessBean process, final String name) {
		final VariableNode variableNode = _getProcessNode(process).getVariableNodeByName(name);
		return varService.getVariableValue(process, variableNode);
	}

	@Override
	public void setVariable(final ProcessBean process, final String name, final Object value) {
		setVariable(process, new String[] { name }, new Object[] { value });
	}

	@Override
	public void setVariable(final ProcessBean process, final String[] names, final Object[] values) {
		varService.setVariableValue(process, names, values);
	}

	@Override
	public Collection<String> getVariableNames(final ProcessBean process) {
		return _getProcessNode(process).variables().keySet();
	}

	@Override
	public WorkitemBean getFirstWorkitem(final ProcessBean process) {
		final ActivityBean startActivity = aService.getStartActivity(process);
		for (final ActivityBean activity : aService.getNextActivities(startActivity)) {
			final List<WorkitemBean> list = wService.getWorkitems(activity, EWorkitemStatus.running);
			if (list.size() > 0) {
				return list.get(0);
			}
		}
		throw WorkflowException.of($m("ProcessService.0"));
	}

	@Override
	public boolean isFinalStatus(final ProcessBean t) {
		return t.getStatus().ordinal() >= EProcessStatus.complete.ordinal();
	}

	@Override
	public void doUpdateTitle(final ProcessBean process, final String title) {
		if (process != null) {
			process.setTitle(title);
			update(new String[] { "title" }, process);
		}
	}

	@Override
	public synchronized void doUpdateViews(final ProcessBean process) {
		if (process != null) {
			process.setViews(process.getViews() + 1);
			update(new String[] { "views" }, process);
		}
	}

	ProcessBean _create(final ProcessModelBean processModel, final ID userId, final ID roleId,
			final String title) {
		final ProcessBean process = createBean();
		process.setModelId(processModel.getId());
		process.setModelName(processModel.getModelName());
		process.setTitle(title);
		process.setCreateDate(new Date());
		final ProcessDocument doc = mService.getProcessDocument(processModel);
		process.setVersion(doc.getProcessNode().getVersion().toString());

		final PermissionUser user = permission.getUser(userId);
		process.setUserId(user.getId());
		process.setUserText(user.getText());
		process.setDeptId(user.getDept().getId());
		final ID domainId = user.getDept().getDomainId();
		if (domainId != null) {
			process.setDomainId(domainId);
		}
		process.setRoleId(roleId);
		return process;
	}

	@Override
	public IWorkflowView getWorkflowView(final ProcessBean process) {
		if (process == null) {
			return null;
		}
		final String viewClass = getProcessDocument(process).getProcessNode().getViewClass();
		return (IWorkflowView) (viewClass != null ? singleton(viewClass) : null);
	}

	@Override
	public ILuceneManager getLuceneService() {
		return luceneService;
	}

	private WfLuceneManager luceneService;

	protected WfLuceneManager createLuceneManager() {
		return new WfLuceneManager();
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		luceneService = createLuceneManager();
		if (!luceneService.indexExists()) {
			getModuleContext().getTaskExecutor().execute(new ExecutorRunnable() {
				@Override
				protected void task() throws Exception {
					getLog().info($m("ProcessService.4"));
					luceneService.rebuildIndex();
					getLog().info($m("ProcessService.5"));
				}
			});
		}

		addListener(new DbEntityAdapterEx() {
			@Override
			public void onAfterInsert(final IDbEntityManager<?> manager, final Object[] beans) {
				super.onAfterInsert(manager, beans);
				for (final Object bean : beans) {
					// 更新流程实例计数
					updateProcessCount((ProcessBean) bean);
				}
			}

			@Override
			public void onAfterDelete(final IDbEntityManager<?> manager, final IParamsValue paramsValue) {
				super.onAfterDelete(manager, paramsValue);
				for (final ProcessBean process : coll(paramsValue)) {
					updateProcessCount(process);
				}
			}

			private void updateProcessCount(final ProcessBean process) {
				// 更新流程实例计数
				final ProcessModelBean processModel = getProcessModel(process);
				processModel.setProcessCount(getProcessList(null, processModel).getCount());
				mService.update(new String[] { "processCount" }, processModel);

				final ID domainId = process.getDomainId();
				final ProcessModelDomainR r = drService.getProcessModelDomainR(domainId, processModel);
				r.setProcessCount(getProcessList(domainId, processModel).getCount());
				drService.update(new String[] { "processCount" }, r);
			}

			@Override
			public void onBeforeDelete(final IDbEntityManager<?> manager,
					final IParamsValue paramsValue) {
				super.onBeforeDelete(manager, paramsValue);

				for (final ProcessBean process : coll(paramsValue)) {
					final Object id = process.getId();
					// 触发删除事件
					for (final IWorkflowListener listener : getEventListeners(process)) {
						((IProcessListener) listener).onDelete(process);
					}

					// 删除lob
					getEntityManager(ProcessLobBean.class).delete(new ExpressionValue("id=?", id));

					// 删除任务环节
					aService.deleteWith("processId=?", id);
					// 删除待阅
					vService.deleteWith("processId=?", id);

					// 删除流程变量
					varService.deleteVariables(EVariableSource.process, id);

					// 删除意见
					commentService.deleteWith("contentId=?", id);
					commentUserService.deleteWith("contentId=?", id);
				}
			}

			@Override
			public void onBeforeUpdate(final IDbEntityManager<?> manager, final String[] columns,
					final Object[] beans) {
				super.onAfterUpdate(manager, columns, beans);

				if (ArrayUtils.contains(columns, "status", true)) {
					for (final Object bean : beans) {
						final ProcessBean process = (ProcessBean) bean;
						for (final IWorkflowListener listener : getEventListeners(process)) {
							((IProcessListener) listener).onStatusChange(
									process,
									Convert.toEnum(EProcessStatus.class,
											queryFor("status", "id=?", process.getId())));
						}
					}
				}
			}
		});
	}

	protected class WfLuceneManager extends AbstractLuceneManager {

		public WfLuceneManager() {
			super(new File(workflowContext.getTmpdir() + "index"));
		}

		@Override
		protected String[] getQueryFields() {
			return new String[] { "id", "topic" };
		}

		@Override
		protected IDataQuery<?> queryAll() {
			return workflowContext.getProcessService().queryAll();
		}

		@Override
		protected Object documentToObject(final LuceneDocument doc, final Class<?> beanClass) {
			Object obj;
			if (beanClass == null) {
				obj = super.documentToObject(doc, beanClass);
			} else {
				obj = getBean(doc.get("id"));
			}
			return obj;
		}

		@Override
		protected void objectToDocument(final Object object, final LuceneDocument doc)
				throws IOException {
			super.objectToDocument(object, doc);
			final ProcessBean process = (ProcessBean) object;
			doc.addTextField("topic", process.getTitle(), false);
		}
	}
}
