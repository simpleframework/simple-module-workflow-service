package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.FilterItems;
import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.DbDataQuery;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.ctx.permission.PermissionDept;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IWorkitemService;
import net.simpleframework.workflow.engine.PropSequential;
import net.simpleframework.workflow.engine.TasknodeUtils;
import net.simpleframework.workflow.engine.WorkitemComplete;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.UserStatBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.event.IWorkitemListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.MergeNode;
import net.simpleframework.workflow.schema.StartNode;
import net.simpleframework.workflow.schema.UserNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkitemService extends AbstractWorkflowService<WorkitemBean>
		implements IWorkitemService {

	@Override
	public ActivityBean getActivity(final WorkitemBean workitem) {
		return workitem == null ? null : wfaService.getBean(workitem.getActivityId());
	}

	@Override
	public ProcessBean getProcessBean(final WorkitemBean workitem) {
		return workitem == null ? null : wfpService.getBean(workitem.getProcessId());
	}

	@Override
	public void doComplete(final WorkitemComplete workitemComplete) {
		final WorkitemBean workitem = workitemComplete.getWorkitem();
		_assert(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);
		DelegationBean delegation = null;
		if (workitem.getStatus() == EWorkitemStatus.delegate
				&& (delegation = wfdService.queryRunningDelegation(workitem)) != null) {
			_assert(delegation, EDelegationStatus.running);
		}

		try {
			final ActivityBean activity = getActivity(workitem);
			_assert(activity, EActivityStatus.running, EActivityStatus.timeout);

			final ActivityService wfaServiceImpl = (ActivityService) wfaService;
			final ProcessBean process = wfaServiceImpl.getProcessBean(activity);

			// 更新流程变量
			final Map<String, Object> variables = workitemComplete.getVariables();
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				final String key = e.getKey();
				if (wfaServiceImpl.getVariableNames(activity).contains(key)) {
					wfaServiceImpl.setVariable(activity, key, e.getValue());
				} else {
					wfpService.setVariable(process, key, e.getValue());
				}
			}

			final ActivityComplete activityComplete = workitemComplete.getActivityComplete();
			// 如果环节设置为不完成，则也不完成任务项
			final boolean bcomplete = activityComplete.isBcomplete();
			if (bcomplete) {
				workitem.setStatus(EWorkitemStatus.complete);
				workitem.setCompleteDate(new Date());
				update(new String[] { "completeDate", "status" }, workitem);

				// 设置委托完成
				if (delegation != null) {
					delegation.setCompleteDate(new Date());
					delegation.setStatus(EDelegationStatus.complete);
					wfdService.update(new String[] { "completeDate", "status" }, delegation);
				}
			}

			if (workitemComplete.isAllCompleted()) {
				wfaServiceImpl.doComplete(activityComplete);
			} else {
				if (!bcomplete) {
					// 如果环节不完成，则必须执行环节完成动作，否则抛出异常
					throw WorkflowException.of($m("WorkitemService.6"));
				}

				final List<?> list = PropSequential.list(activity);
				if (list.size() > 0) { // 获取顺序执行的参与者
					final AbstractTaskNode tasknode = wfaServiceImpl.getTaskNode(activity);
					final Object o = list.remove(0);
					if (TasknodeUtils.isInstanceShared(tasknode)) {
						// 单实例：创建工作项，同时设置后续参与者属性
						PropSequential.set(activity, list);
						wfaServiceImpl.update(new String[] { "properties" }, activity);
						_createSequentialWorkitem(activity, o);
					} else {
						// 多实例，创建环节实例，同时完成当前环节
						final ActivityBean nActivity = wfaServiceImpl._create(process, tasknode, activity,
								new Date());
						PropSequential.set(nActivity, list);
						wfaServiceImpl.insert(nActivity);
						_createSequentialWorkitem(nActivity, o);

						activity.setStatus(EActivityStatus.complete);
						activity.setCompleteDate(new Date());
						wfaServiceImpl.update(new String[] { "completeDate", "status" }, activity);
					}
				}
			}
		} finally {
			workitemComplete.done();
		}
	}

	void _createSequentialWorkitem(final ActivityBean activity, final Object o) {
		if (o instanceof Participant) {
			insert(_create(activity, (Participant) o, new Date()));
		} else if (o instanceof WorkitemBean) {
			_clone(activity, (WorkitemBean) o);
		}
	}

	@Override
	public void doRetake(final WorkitemBean workitem) {
		_assert(workitem, EWorkitemStatus.complete);
		final ActivityBean activity = getActivity(workitem);
		final ActivityService wfaServiceImpl = (ActivityService) wfaService;
		final ProcessBean process = wfaServiceImpl.getProcessBean(activity);
		_assert(process, EProcessStatus.running);

		final AbstractTaskNode tn = wfaServiceImpl.getTaskNode(activity);
		ActivityBean nActivity = null;
		final Date createDate = new Date();
		final EActivityStatus aStatus = activity.getStatus();
		if (aStatus == EActivityStatus.complete) {
			// 检测后续环节
			for (final ActivityBean nextActivity : wfaServiceImpl.getNextActivities(activity)) {
				final AbstractTaskNode tasknode = wfaServiceImpl.getTaskNode(nextActivity);
				if (tasknode instanceof UserNode) {
					// 仅判断完成后的后续环节
					if (nextActivity.getCreateDate().after(activity.getCompleteDate())) {
						// nextActivity
						// 如果用户环节，则不能出现已读和完成
						assertRetakeWorkitems(nextActivity);
					}
					// 放弃
					wfaServiceImpl._abort(nextActivity);
				} else if (tasknode instanceof MergeNode) {
					final EActivityStatus status2 = nextActivity.getStatus();
					if (status2 == EActivityStatus.complete) {
						for (final ActivityBean nextActivity2 : wfaServiceImpl
								.getNextActivities(nextActivity)) {
							if (wfaServiceImpl.getTaskNode(nextActivity2) instanceof UserNode) {
								assertRetakeWorkitems(nextActivity2);
							} else {
								throw WorkflowException.of($m("WorkitemService.0"));
							}
						}
					}

					final List<String> preActivities = wfaServiceImpl
							._getMergePreActivities(nextActivity);
					final int size = preActivities.size();
					if (size > 0) {
						if (activity.getId().toString().equals(preActivities.remove(size - 1))) {
							// 新建merge环节
							if (status2 == EActivityStatus.running) {
								wfaServiceImpl._setMergePreActivities(nextActivity,
										preActivities.toArray());
								wfaServiceImpl.update(new String[] { "properties" }, nextActivity);
							} else if (status2 == EActivityStatus.complete) {
								final ActivityBean mActivity = wfaServiceImpl._create(tasknode,
										wfaServiceImpl.getBean(nextActivity.getPreviousId()), createDate);
								wfaServiceImpl._setMergePreActivities(mActivity, preActivities.toArray());
								wfaServiceImpl.insert(mActivity);
								// 放弃
								wfaServiceImpl._abort(nextActivity, EActivityAbortPolicy.nextActivities);
							}
						} else {
							throw WorkflowException.of($m("WorkitemService.1"));
						}
					} else {
						// 放弃
						wfaServiceImpl._abort(nextActivity, EActivityAbortPolicy.nextActivities);
					}
				} else {
					// 其他环节，不允许取回
					throw WorkflowException.of($m("WorkitemService.0"));
				}
			}
			// 创建新的环节
			nActivity = wfaServiceImpl._create(tn, wfaServiceImpl.getBean(activity.getPreviousId()),
					createDate);
			wfaServiceImpl.insert(nActivity);
		} else if (aStatus == EActivityStatus.running) {
			nActivity = activity;
			// 处理顺序情况
			if (TasknodeUtils.isSequential(tn)) {
				final List<WorkitemBean> list = getWorkitems(activity);
				WorkitemBean workitem2 = null;
				for (final WorkitemBean _workitem : list) {
					if (_workitem.getId().equals(workitem.getId())) {
						continue;
					}
					if (isFinalStatus(_workitem)) {
						if (_workitem.getCreateDate().after(workitem.getCreateDate())) {
							throw WorkflowException.of($m("WorkitemService.1"));
						}
					} else { // 只有一个
						if (_workitem.isReadMark()) {
							throw WorkflowException.of($m("WorkitemService.1"));
						}
						workitem2 = _workitem;
					}
				}
				_abort(workitem2);

				PropSequential.push(nActivity, workitem2);
				wfaServiceImpl.update(new String[] { "properties" }, nActivity);
			}
		} else {
			throw WorkflowStatusException.of(activity, aStatus, EActivityStatus.running,
					EActivityStatus.complete);
		}

		if (nActivity != null) {
			// 复制新的工作项
			final WorkitemBean nWorkitem = _clone(nActivity, workitem);
			nWorkitem.setRetakeRef(workitem.getId());
			update(new String[] { "retakeRef" }, nWorkitem);

			workitem.setRetakeId(nWorkitem.getId());
			update(new String[] { "retakeId" }, workitem);
		}
	}

	void _abort(final WorkitemBean workitem) {
		workitem.setStatus(EWorkitemStatus.abort);
		workitem.setCompleteDate(new Date());
		update(new String[] { "status", "completeDate" }, workitem);

		if (!workitem.isReadMark()) { // 未读
			workitem.setReadMark(true);
			update(new String[] { "readMark" }, workitem);
		}

		// 如果含有委托
		final DelegationBean delegation = _getDelegation(workitem);
		if (delegation != null) {
			((DelegationService) wfdService)._abort(delegation);
		}
	}

	WorkitemBean _clone(final ActivityBean nActivity, final WorkitemBean workitem) {
		final WorkitemBean nWorkitem = _create(nActivity, new Participant(workitem), new Date());

		// 设置退回节点的引用
		final ActivityBean preActivity = wfaService.getPreActivity(nActivity);
		if (preActivity != null) {
			final EActivityStatus pStatus = preActivity.getStatus();
			if (pStatus == EActivityStatus.fallback || pStatus == EActivityStatus.fallback2) {
				nWorkitem.setFallbackId(preActivity.getId());
			}
		}

		insert(nWorkitem);

		// 如果含有委托
		final DelegationBean delegation = _getDelegation(workitem);
		if (delegation != null) {
			final DelegationService wfdServiceImpl = (DelegationService) wfdService;
			final DelegationBean nDelegation = wfdServiceImpl._create(EDelegationSource.workitem,
					nWorkitem.getId(), delegation.getOuserId(), delegation.getUserId(),
					delegation.getDstartDate(), delegation.getDcompleteDate(),
					delegation.getDescription());
			wfdServiceImpl.insert(nDelegation);
			wfdServiceImpl._doDelegateTask(nDelegation, false);
		}
		return nWorkitem;
	}

	DelegationBean _getDelegation(final WorkitemBean workitem) {
		return workitem.getUserId().equals(workitem.getUserId2()) ? null
				: wfdService.queryRunningDelegation(workitem);
	}

	protected void assertRetakeWorkitems(final ActivityBean nextActivity) {
		for (final WorkitemBean workitem : getWorkitems(nextActivity)) {
			if (workitem.getStatus() == EWorkitemStatus.delegate) {
				final DelegationBean delegation = wfdService.queryRunningDelegation(workitem);
				if (delegation != null && delegation.getStatus() == EDelegationStatus.running) {
					throw WorkflowException.of($m("WorkitemService.8"));
				}
			} else {
				_assert(workitem, EWorkitemStatus.running);
			}

			if (workitem.isReadMark()) {
				throw WorkflowException.of($m("WorkitemService.1"));
			}
		}
	}

	@Override
	public void doUnReadMark(final WorkitemBean workitem) {
		_doReadMark(workitem, true);
	}

	@Override
	public void doReadMark(final WorkitemBean workitem) {
		_doReadMark(workitem, false);
	}

	private void _doReadMark(final WorkitemBean workitem, final boolean unread) {
		workitem.setReadMark(!unread);
		if (workitem.getReadDate() == null) {
			workitem.setReadDate(new Date());
			update(new String[] { "readMark", "readDate" }, workitem);
		} else {
			update(new String[] { "readMark" }, workitem);
		}
	}

	@Override
	public void doUnTopMark(final WorkitemBean workitem) {
		_doTopMark(workitem, true);
	}

	@Override
	public void doTopMark(final WorkitemBean workitem) {
		_doTopMark(workitem, false);
	}

	private void _doTopMark(final WorkitemBean workitem, final boolean untop) {
		workitem.setTopMark(!untop);
		update(new String[] { "topMark" }, workitem);
	}

	@Override
	public void doLastUpdate(final WorkitemBean workitem, final Date lastUpdate, final ID lastUser) {
		workitem.setLastUpdate(lastUpdate);
		workitem.setLastUser(lastUser);
		update(new String[] { "lastUpdate", "lastuser" }, workitem);
	}

	@Override
	public void doWorkitemDelegation(final WorkitemBean workitem, final ID ouserId, final ID userId,
			final Date dStartDate, final Date dCompleteDate, final String description) {
		if (workitem.getUserId().equals(userId)) {
			throw WorkflowException.of($m("WorkitemService.4"));
		}
		_assert(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);

		// if (!workitem.isReadMark()) {
		// // 设置已读
		// doReadMark(workitem);
		// }

		final DelegationService wfdServiceImpl = (DelegationService) wfdService;
		if (workitem.getStatus() == EWorkitemStatus.delegate) {
			// 如果已经委托了，则放弃
			final DelegationBean delegation = wfdServiceImpl.queryRunningDelegation(workitem);
			if (delegation != null) {
				wfdServiceImpl._abort(delegation);
			}
		}

		final DelegationBean delegation = wfdServiceImpl._create(EDelegationSource.workitem,
				workitem.getId(), ouserId, userId, dStartDate, dCompleteDate, description);
		wfdServiceImpl.insert(delegation);
		// 执行...
		wfdServiceImpl._doDelegateTask(delegation, true);
	}

	@Override
	public List<WorkitemBean> getNextWorkitems(final ActivityBean preActivity,
			final EWorkitemStatus... status) {
		final List<WorkitemBean> workitems = new ArrayList<>();
		for (final ActivityBean activity : wfaService.getNextActivities(preActivity)) {
			workitems.addAll(getWorkitems(activity, status));
		}
		return workitems;
	}

	@Override
	public List<WorkitemBean> getWorkitems(final ActivityBean activity,
			final EWorkitemStatus... status) {
		final List<WorkitemBean> workitems = new ArrayList<>();
		if (activity != null) {
			WorkitemBean workitem;
			final IDataQuery<WorkitemBean> dq = query("activityId=?", activity.getId());
			while ((workitem = dq.next()) != null) {
				if (ArrayUtils.isEmpty(status) || ArrayUtils.contains(status, workitem.getStatus())) {
					workitems.add(workitem);
				}
			}
		}
		return workitems;
	}

	@Override
	public List<WorkitemBean> getWorkitems(final ProcessBean process, final ID userId,
			final EWorkitemStatus... status) {
		return DataQueryUtils.toList(_getWorklist(process, userId, null, (FilterItems) null, status));
	}

	protected EWorkitemStatus[] STATUS_RUNNINGs = new EWorkitemStatus[] { EWorkitemStatus.running,
			EWorkitemStatus.suspended, EWorkitemStatus.delegate };

	@Override
	public IDataQuery<WorkitemBean> getWorklist(final ID userId, final List<ProcessModelBean> models,
			final FilterItems items, final EWorkitemStatus... status) {
		return _getWorklist(null, userId, models, items, status);
	}

	@Override
	public IDataQuery<WorkitemBean> getWorklist(final ID userId, final List<ProcessModelBean> models,
			final EWorkitemStatus... status) {
		return getWorklist(userId, models, null, status);
	}

	@Override
	public IDataQuery<WorkitemBean> getRunningWorklist(final ID userId,
			final List<ProcessModelBean> models) {
		return getWorklist(userId, models, STATUS_RUNNINGs);
	}

	@Override
	public IDataQuery<WorkitemBean> getRunningWorklist_Unread(final ID userId,
			final List<ProcessModelBean> models) {
		return getWorklist(userId, models, FilterItems.of("readMark", Boolean.FALSE),
				STATUS_RUNNINGs);
	}

	protected String getDefaultOrderby(final String dateColumn) {
		return " order by topmark desc, "
				+ (StringUtils.hasText(dateColumn) ? dateColumn : "lastupdate") + " desc";
	}

	private IDataQuery<WorkitemBean> _getWorklist(final ProcessBean process, final ID userId,
			final List<ProcessModelBean> models, final FilterItems items,
			final EWorkitemStatus... status) {
		final StringBuilder sql = new StringBuilder("1=1");
		final ArrayList<Object> params = new ArrayList<>();
		if (process != null) {
			sql.append(" and processid=?");
			params.add(process.getId());
		} else if (models != null && models.size() > 0) {
			sql.append(" and (");
			int i = 0;
			for (final ProcessModelBean pm : models) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				sql.append("modelid=?");
				params.add(pm.getId());
			}
			sql.append(")");
		}
		if (userId != null) {
			sql.append(" and userid2=?");
			params.add(userId);
		}
		buildStatusSQL(sql, params, status);
		if (items != null && items.size() > 0) {
			final ExpressionValue eVal = toExpressionValue(items);
			sql.append(" and (").append(eVal.getExpression()).append(")");
			params.addAll(ArrayUtils.asList(eVal.getValues()));
		}
		if (ArrayUtils.contains(status, EWorkitemStatus.complete)) {
			sql.append(getDefaultOrderby("completedate"));
		} else {
			sql.append(getDefaultOrderby(null));
		}
		return query(sql, params.toArray());
	}

	@Override
	public void addQueryFilters(final DbDataQuery<WorkitemBean> dq, final String topic,
			final String pno) {
		final SQLValue sv = dq.getSqlValue();
		final StringBuilder sb = new StringBuilder();
		sb.append("select t.* from (").append(sv.getSql()).append(") t left join ")
				.append(wfpService.getTablename(ProcessBean.class))
				.append(" p on t.processid=p.id where 1=1");
		if (StringUtils.hasText(topic)) {
			sb.append(" and p.title like '%").append(SqlUtils.sqlEscape(topic)).append("%'");
		}
		if (StringUtils.hasText(pno)) {
			sb.append(" and p.pno like '%").append(SqlUtils.sqlEscape(pno)).append("%'");
		}
		sv.setSql(sb.toString());
	}

	@Override
	public Map<String, Object> createVariables(final WorkitemBean workitem) {
		final Map<String, Object> variables = wfaService.createVariables(getActivity(workitem));
		variables.put("workitem", workitem);
		return variables;
	}

	@Override
	public boolean isFinalStatus(final WorkitemBean t) {
		return _isFinalStatus(t.getStatus());
	}

	private boolean _isFinalStatus(final EWorkitemStatus status) {
		return status.ordinal() >= EWorkitemStatus.complete.ordinal();
	}

	@Override
	public void doDeleteProcess(final WorkitemBean workitem) {
		// 检测是否含有完成状态，否则不能删除
		final Object processId = getActivity(workitem).getProcessId();
		final IDataQuery<ActivityBean> qs = wfaService.query("processId=?", processId);
		ActivityBean activity;
		while ((activity = qs.next()) != null) {
			if (wfaService.getTaskNode(activity) instanceof StartNode) {
				continue;
			}
			final EActivityStatus status = activity.getStatus();
			if (status != EActivityStatus.running) {
				throw WorkflowException.of($m("WorkitemService.3"));
			}
		}
		final ProcessBean process = wfpService.getBean(processId);
		if (process != null) {
			wfpService.doAbort(process, EProcessAbortPolicy.normal);
			wfpService.delete(process);
		}
	}

	WorkitemBean _create(final ActivityBean activity, final Participant participant,
			final Date createDate) {
		final WorkitemBean workitem = createBean();
		final ProcessBean process = wfpService.getBean(activity.getProcessId());
		workitem.setModelId(process.getModelId());
		workitem.setProcessId(process.getId());
		workitem.setActivityId(activity.getId());
		workitem.setCreateDate(createDate);

		final PermissionUser user = participant.getUser();
		workitem.setUserId(user.getId());
		workitem.setUserText(user.getText());

		final PermissionDept dept = user.getDept();
		final ID deptId = participant.getDeptId();
		workitem.setDeptId(deptId != null ? deptId : dept.getId());
		workitem.setDomainId(dept.getDomainId());

		workitem.setRoleId(participant.getRoleId());
		workitem.setUserId2(participant.getUserId());
		return workitem;
	}

	void doUserStat_status(final ID userId) {
		final UserStatBean stat = wfusService.getUserStat(userId);
		// 初始化状态
		for (final EWorkitemStatus status : EWorkitemStatus.values()) {
			BeanUtils.setProperty(stat, "workitem_" + status.name(), 0);
		}
		final IDataQuery<Map<String, Object>> dq = getEntityManager()
				.queryMapSet(new SQLValue("select status, count(status) as cc from "
						+ getTablename(WorkitemBean.class) + " where userid=? group by status", userId));
		Map<String, Object> map;
		while ((map = dq.next()) != null) {
			final EWorkitemStatus status = Convert.toEnum(EWorkitemStatus.class, map.get("status"));
			if (status != null) {
				BeanUtils.setProperty(stat, "workitem_" + status.name(), Convert.toInt(map.get("cc")));
			}
		}
		wfusService.update(stat);
	}

	protected void _onAfterInsert(final WorkitemBean workitem) {
		// 用于子类继承处理
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx<WorkitemBean>() {
			@Override
			public void onBeforeDelete(final IDbEntityManager<WorkitemBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onBeforeDelete(manager, paramsValue);
				for (final WorkitemBean workitem : coll(manager, paramsValue)) {
					if (!workitem.isReadMark()) {
						doReadMark(workitem);
					}

					// 放弃并删除
					final DelegationBean delegation = wfdService.queryRunningDelegation(workitem);
					if (delegation != null) {
						wfdService.doAbort(delegation);
					}
					wfdService.deleteWith("delegationSource=? and sourceId=?",
							EDelegationSource.workitem, workitem.getId());
				}
			}

			@Override
			public void onBeforeUpdate(final IDbEntityManager<WorkitemBean> manager,
					final String[] columns, final WorkitemBean[] beans) throws Exception {
				super.onAfterUpdate(manager, columns, beans);

				for (final WorkitemBean workitem : beans) {
					// 状态转换事件
					if (ArrayUtils.isEmpty(columns) || ArrayUtils.contains(columns, "status", true)) {
						final EWorkitemStatus _status = Convert.toEnum(EWorkitemStatus.class,
								queryFor("status", "id=?", workitem.getId()));
						if (_isFinalStatus(_status)) {
							throw WorkflowException.of($m("WorkitemService.7"));
						}

						if (_status != workitem.getStatus()) {
							for (final IWorkflowListener listener : getEventListeners(workitem)) {
								((IWorkitemListener) listener).onStatusChange(workitem, _status);
							}
						}
					}
				}
			}

			@Override
			public void onAfterUpdate(final IDbEntityManager<WorkitemBean> manager,
					final String[] columns, final WorkitemBean[] beans) throws Exception {
				super.onAfterUpdate(manager, columns, beans);
				for (final WorkitemBean workitem : beans) {
					if (ArrayUtils.contains(columns, "readMark", true)) {
						// 按实际执行者
						doUserStat_readMark(workitem);
					}
					if (ArrayUtils.contains(columns, "status", true)) {
						doUserStat_status(workitem.getUserId());
					}
				}
			}

			@Override
			public void onAfterInsert(final IDbEntityManager<WorkitemBean> manager,
					final WorkitemBean[] beans) throws Exception {
				super.onAfterInsert(manager, beans);

				for (final WorkitemBean workitem : beans) {
					// 如果存在用户委托，则创建
					final DelegationBean delegation = wfdService
							.queryRunningDelegation(workitem.getUserId());
					if (delegation != null && delegation.getStatus() == EDelegationStatus.running) {
						doWorkitemDelegation(workitem, delegation.getSourceId(), delegation.getUserId(),
								null, null, $m("WorkitemService.5", workitem.getUserText()));
					}
					_onAfterInsert(workitem);
					// 设置用户统计
					doUserStat_readMark(workitem);
					doUserStat_status(workitem.getUserId());

					// 触发创建事件
					for (final IWorkflowListener listener : getEventListeners(workitem)) {
						((IWorkitemListener) listener).onCreated(workitem);
					}
				}
			}

			private void doUserStat_readMark(final WorkitemBean workitem) {
				final ID userId = workitem.getUserId2();
				final UserStatBean stat = wfusService.getUserStat(userId);
				stat.setWorkitem_unread(getRunningWorklist_Unread(userId, null).getCount());
				wfusService.update(new String[] { "workitem_unread" }, stat);
			}
		});
	}
}
