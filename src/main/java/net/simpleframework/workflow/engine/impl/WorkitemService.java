package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.FilterItems;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.ctx.permission.PermissionDept;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IWorkitemService;
import net.simpleframework.workflow.engine.PropSequential;
import net.simpleframework.workflow.engine.TasknodeUtils;
import net.simpleframework.workflow.engine.WorkitemComplete;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
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
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkitemService extends AbstractWorkflowService<WorkitemBean> implements
		IWorkitemService {

	@Override
	public ActivityBean getActivity(final WorkitemBean workitem) {
		return workitem == null ? null : aService.getBean(workitem.getActivityId());
	}

	@Override
	public ProcessBean getProcessBean(final WorkitemBean workitem) {
		return workitem == null ? null : pService.getBean(workitem.getProcessId());
	}

	@Override
	public void doComplete(final WorkitemComplete workitemComplete) {
		final WorkitemBean workitem = workitemComplete.getWorkitem();
		_assert(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);
		DelegationBean delegation = null;
		if (workitem.getStatus() == EWorkitemStatus.delegate
				&& (delegation = dService.queryRunningDelegation(workitem)) != null) {
			dService._assert(delegation, EDelegationStatus.running);
		}

		try {
			final ActivityBean activity = getActivity(workitem);
			aService._assert(activity, EActivityStatus.running, EActivityStatus.timeout);
			final ProcessBean process = aService.getProcessBean(activity);

			// 更新流程变量
			final Map<String, Object> variables = workitemComplete.getVariables();
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				final String key = e.getKey();
				if (aService.getVariableNames(activity).contains(key)) {
					aService.setVariable(activity, key, e.getValue());
				} else {
					pService.setVariable(process, key, e.getValue());
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
					dService.update(new String[] { "completeDate", "status" }, delegation);
				}
			}

			if (workitemComplete.isAllCompleted()) {
				aService.doComplete(activityComplete);
			} else {
				if (!bcomplete) {
					// 如果环节不完成，则必须执行环节完成动作，否则抛出异常
					throw WorkflowException.of($m("WorkitemService.6"));
				}

				final List<?> list = PropSequential.list(activity);
				if (list.size() > 0) { // 获取顺序执行的参与者
					final AbstractTaskNode tasknode = aService.getTaskNode(activity);
					final Object o = list.remove(0);
					if (TasknodeUtils.isInstanceShared(tasknode)) {
						// 单实例：创建工作项，同时设置后续参与者属性
						PropSequential.set(activity, list);
						aService.update(new String[] { "properties" }, activity);
						_createSequentialWorkitem(activity, o);
					} else {
						// 多实例，创建环节实例，同时完成当前环节
						final ActivityBean nActivity = aService._create(process, tasknode, activity,
								new Date());
						PropSequential.set(nActivity, list);
						aService.insert(nActivity);
						_createSequentialWorkitem(nActivity, o);

						activity.setStatus(EActivityStatus.complete);
						activity.setCompleteDate(new Date());
						aService.update(new String[] { "completeDate", "status" }, activity);
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
		final ProcessBean process = aService.getProcessBean(activity);
		pService._assert(process, EProcessStatus.running);

		final AbstractTaskNode tn = aService.getTaskNode(activity);
		ActivityBean nActivity = null;
		final Date createDate = new Date();
		final EActivityStatus aStatus = activity.getStatus();
		if (aStatus == EActivityStatus.complete) {
			// 检测后续环节
			for (final ActivityBean nextActivity : aService.getNextActivities(activity)) {
				final AbstractTaskNode tasknode = aService.getTaskNode(nextActivity);
				if (tasknode instanceof UserNode) {
					// 如果用户环节，则不能出现已读和完成
					assertRetakeWorkitems(nextActivity);
					// 放弃
					aService._abort(nextActivity);
				} else if (tasknode instanceof MergeNode) {
					final EActivityStatus status2 = nextActivity.getStatus();
					if (status2 == EActivityStatus.complete) {
						for (final ActivityBean nextActivity2 : aService.getNextActivities(nextActivity)) {
							if (aService.getTaskNode(nextActivity2) instanceof UserNode) {
								assertRetakeWorkitems(nextActivity2);
							} else {
								throw WorkflowException.of($m("WorkitemService.0"));
							}
						}
					}

					final List<String> preActivities = aService._getMergePreActivities(nextActivity);
					final int size = preActivities.size();
					if (size > 0) {
						if (activity.getId().toString().equals(preActivities.remove(size - 1))) {
							// 新建merge环节
							if (status2 == EActivityStatus.running) {
								aService._setMergePreActivities(nextActivity, preActivities.toArray());
								aService.update(new String[] { "properties" }, nextActivity);
							} else if (status2 == EActivityStatus.complete) {
								final ActivityBean mActivity = aService._create(tasknode,
										aService.getBean(nextActivity.getPreviousId()), createDate);
								aService._setMergePreActivities(mActivity, preActivities.toArray());
								aService.insert(mActivity);
								// 放弃
								aService._abort(nextActivity, EActivityAbortPolicy.nextActivities);
							}
						} else {
							throw WorkflowException.of($m("WorkitemService.1"));
						}
					} else {
						// 放弃
						aService._abort(nextActivity, EActivityAbortPolicy.nextActivities);
					}
				} else {
					// 其他环节，不允许取回
					throw WorkflowException.of($m("WorkitemService.0"));
				}
			}
			// 创建新的环节
			nActivity = aService._create(tn, aService.getBean(activity.getPreviousId()), createDate);
			aService.insert(nActivity);
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
				aService.update(new String[] { "properties" }, nActivity);
			}
		} else {
			throw WorkflowStatusException.of(aStatus, EActivityStatus.running,
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
		_status(workitem, EWorkitemStatus.abort);

		if (!workitem.isReadMark()) { // 未读
			workitem.setReadMark(true);
			update(new String[] { "readMark" }, workitem);
		}

		// 如果含有委托
		final DelegationBean delegation = _getDelegation(workitem);
		if (delegation != null) {
			dService._abort(delegation);
		}
	}

	WorkitemBean _clone(final ActivityBean nActivity, final WorkitemBean workitem) {
		final WorkitemBean nWorkitem = _create(nActivity, new Participant(workitem.getUserId(),
				workitem.getRoleId(), workitem.getDeptId()), new Date());

		// 设置退回节点的引用
		final ActivityBean preActivity = aService.getPreActivity(nActivity);
		if (preActivity != null && preActivity.getStatus() == EActivityStatus.fallback) {
			nWorkitem.setFallbackId(preActivity.getId());
		}

		insert(nWorkitem);

		// 如果含有委托
		final DelegationBean delegation = _getDelegation(workitem);
		if (delegation != null) {
			final DelegationBean nDelegation = dService._create(EDelegationSource.workitem,
					nWorkitem.getId(), delegation.getUserId(), delegation.getDstartDate(),
					delegation.getDcompleteDate(), delegation.getDescription());
			dService.insert(nDelegation);
			dService._doDelegateTask(nDelegation, false);
		}
		return nWorkitem;
	}

	DelegationBean _getDelegation(final WorkitemBean workitem) {
		return workitem.getUserId().equals(workitem.getUserId2()) ? null : dService
				.queryRunningDelegation(workitem);
	}

	protected void assertRetakeWorkitems(final ActivityBean nextActivity) {
		for (final WorkitemBean workitem : getWorkitems(nextActivity)) {
			_assert(workitem, EWorkitemStatus.running);
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
	public void doWorkitemDelegation(final WorkitemBean workitem, final ID userId,
			final Date dStartDate, final Date dCompleteDate, final String description) {
		if (workitem.getUserId().equals(userId)) {
			throw WorkflowException.of($m("WorkitemService.4"));
		}
		_assert(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);
		if (workitem.getStatus() == EWorkitemStatus.delegate) {
			// 如果已经委托了，则放弃
			final DelegationBean delegation = dService.queryRunningDelegation(workitem);
			if (delegation != null) {
				dService._abort(delegation);
			}
		}

		final DelegationBean delegation = dService._create(EDelegationSource.workitem,
				workitem.getId(), userId, dStartDate, dCompleteDate, description);
		dService.insert(delegation);
		// 执行...
		dService._doDelegateTask(delegation, true);
	}

	@Override
	public List<WorkitemBean> getWorkitems(final ActivityBean activity,
			final EWorkitemStatus... status) {
		final List<WorkitemBean> workitems = new ArrayList<WorkitemBean>();
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
		return DataQueryUtils.toList(_getWorklist(process, userId, (FilterItems) null, status));
	}

	@Override
	public IDataQuery<WorkitemBean> getWorklist(final ID userId, final EWorkitemStatus... status) {
		return getWorklist(userId, null, status);
	}

	@Override
	public IDataQuery<WorkitemBean> getWorklist(final ID userId, final FilterItems items,
			final EWorkitemStatus... status) {
		return _getWorklist(null, userId, items, status);
	}

	protected String getDefaultOrderby() {
		return " order by topmark desc, createdate desc";
	}

	private IDataQuery<WorkitemBean> _getWorklist(final ProcessBean process, final ID userId,
			final FilterItems items, final EWorkitemStatus... status) {
		final StringBuilder sql = new StringBuilder("1=1");
		final ArrayList<Object> params = new ArrayList<Object>();
		if (process != null) {
			sql.append(" and processid=?");
			params.add(process.getId());
		}
		if (userId != null) {
			sql.append(" and userid2=?");
			params.add(userId);
		}
		buildStatusSQL(sql, params, null, status);
		if (items != null && items.size() > 0) {
			final ExpressionValue eVal = toExpressionValue(items);
			sql.append(" and (").append(eVal.getExpression()).append(")");
			params.addAll(ArrayUtils.asList(eVal.getValues()));
		}
		sql.append(getDefaultOrderby());
		return query(sql, params.toArray());
	}

	@Override
	public IDataQuery<WorkitemBean> getRunningWorklist(final ID userId) {
		return getWorklist(userId, EWorkitemStatus.running, EWorkitemStatus.suspended,
				EWorkitemStatus.delegate);
	}

	@Override
	public IDataQuery<WorkitemBean> getUnreadWorklist(final ID userId) {
		final StringBuilder sql = new StringBuilder(
				"userId2=? and readMark=? and (status=? or status=? or status=?)")
				.append(getDefaultOrderby());
		return query(sql, userId, Boolean.FALSE, EWorkitemStatus.running, EWorkitemStatus.suspended,
				EWorkitemStatus.delegate);
	}

	@Override
	public Map<String, Object> createVariables(final WorkitemBean workitem) {
		final Map<String, Object> variables = aService.createVariables(getActivity(workitem));
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
		final IDataQuery<ActivityBean> qs = aService.query("processId=?", processId);
		ActivityBean activity;
		while ((activity = qs.next()) != null) {
			if (aService.getTaskNode(activity) instanceof StartNode) {
				continue;
			}
			final EActivityStatus status = activity.getStatus();
			if (status != EActivityStatus.running) {
				throw WorkflowException.of($m("WorkitemService.3"));
			}
		}
		pService.delete(processId);
	}

	WorkitemBean _create(final ActivityBean activity, final Participant participant,
			final Date createDate) {
		final WorkitemBean workitem = createBean();
		workitem.setProcessId(activity.getProcessId());
		workitem.setActivityId(activity.getId());
		workitem.setCreateDate(createDate);

		final PermissionUser user = permission.getUser(participant.userId);
		workitem.setUserId(user.getId());
		workitem.setUserText(user.getText());
		final PermissionDept dept = user.getDept();
		workitem.setDeptId(dept.getId());
		workitem.setDomainId(dept.getDomainId());

		workitem.setRoleId(participant.roleId);
		workitem.setUserId2(participant.userId);
		return workitem;
	}

	void doUserStat_status(final ID userId) {
		final UserStatBean stat = usService.getUserStat(userId);
		final IDataQuery<Map<String, Object>> dq = getEntityManager().queryMapSet(
				new SQLValue("select status, count(status) as cc from "
						+ getTablename(WorkitemBean.class) + " where userid=? group by status", userId));
		Map<String, Object> map;
		usService.reset(stat);
		while ((map = dq.next()) != null) {
			final EWorkitemStatus status = Convert.toEnum(EWorkitemStatus.class, map.get("status"));
			if (status != null) {
				BeanUtils.setProperty(stat, "workitem_" + status.name(), Convert.toInt(map.get("cc")));
			}
		}
		usService.update(stat);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx<WorkitemBean>() {
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
					final ID userId = workitem.getUserId();
					if (ArrayUtils.contains(columns, "readMark", true)) {
						doUserStat_readMark(userId);
					}
					if (ArrayUtils.contains(columns, "status", true)) {
						doUserStat_status(userId);
					}
				}
			}

			@Override
			public void onAfterInsert(final IDbEntityManager<WorkitemBean> manager,
					final WorkitemBean[] beans) throws Exception {
				super.onAfterInsert(manager, beans);

				for (final WorkitemBean workitem : beans) {
					// 如果存在用户委托，则创建
					final DelegationBean delegation = dService.queryRunningDelegation(workitem
							.getUserId());
					if (delegation != null) {
						doWorkitemDelegation(workitem, delegation.getUserId(), null, null,
								$m("WorkitemService.5", workitem.getUserText()));
					}
					// 设置用户统计
					final ID userId = workitem.getUserId();
					doUserStat_readMark(userId);
					doUserStat_status(userId);

					// 触发创建事件
					for (final IWorkflowListener listener : getEventListeners(workitem)) {
						((IWorkitemListener) listener).onCreated(workitem);
					}
				}
			}

			private void doUserStat_readMark(final ID userId) {
				final UserStatBean stat = usService.getUserStat(userId);
				stat.setWorkitem_unread(getUnreadWorklist(userId).getCount());
				usService.update(new String[] { "workitem_unread" }, stat);
			}
		});
	}
}
