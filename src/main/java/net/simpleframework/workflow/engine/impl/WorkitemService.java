package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IWorkitemService;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.PropSequential;
import net.simpleframework.workflow.engine.TasknodeUtils;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;
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
		return aService.getBean(workitem.getActivityId());
	}

	@Override
	public ProcessBean getProcessBean(final WorkitemBean workitem) {
		return pService.getBean(workitem.getProcessId());
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
			if (process.getStatus() == EProcessStatus.suspended) {
				throw WorkflowStatusException.of($m("WorkitemService.2"));
			}

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

			workitem.setStatus(EWorkitemStatus.complete);
			workitem.setCompleteDate(new Date());
			update(new String[] { "completeDate", "status" }, workitem);

			// 设置委托完成
			if (delegation != null) {
				delegation.setCompleteDate(new Date());
				delegation.setStatus(EDelegationStatus.complete);
				dService.update(new String[] { "completeDate", "status" }, delegation);
			}

			if (workitemComplete.isAllCompleted()) {
				aService.doComplete(new ActivityComplete(workitem));
			} else {
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
						final ActivityBean nActivity = aService._create(process, tasknode, activity);
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
			insert(_create(activity, (Participant) o));
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
										aService.getBean(nextActivity.getPreviousId()));
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
			nActivity = aService._create(tn, aService.getBean(activity.getPreviousId()));
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
			_status(workitem, EWorkitemStatus.retake);

			// 复制新的工作项
			final WorkitemBean nWorkitem = _clone(nActivity, workitem);
			nWorkitem.setRetakeRef(workitem.getId());
			update(new String[] { "retakeRef" }, nWorkitem);
		}
	}

	void _abort(final WorkitemBean workitem) {
		_status(workitem, EWorkitemStatus.abort);

		if (!workitem.isReadMark()) { // 未读
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
				workitem.getRoleId()));
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
		_assert(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);
		workitem.setReadMark(!unread);
		update(new String[] { "readMark" }, workitem);
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
		WorkitemBean workitem;
		final IDataQuery<WorkitemBean> dq = query("activityId=?", activity.getId());
		while ((workitem = dq.next()) != null) {
			if (ArrayUtils.isEmpty(status) || ArrayUtils.contains(status, workitem.getStatus())) {
				workitems.add(workitem);
			}
		}
		return workitems;
	}

	private static final String DEFAULT_ORDERBY = " order by topmark desc, createdate desc";

	@Override
	public IDataQuery<WorkitemBean> getWorklist(final ID userId, final EWorkitemStatus... status) {
		final StringBuilder sql = new StringBuilder("userId2=?");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(userId);
		if (status != null && status.length > 0) {
			sql.append(" and (");
			for (final EWorkitemStatus s : status) {
				if (params.size() > 1) {
					sql.append(" or ");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
		sql.append(DEFAULT_ORDERBY);
		return query(sql.toString(), params.toArray());
	}

	@Override
	public IDataQuery<WorkitemBean> getRunningWorklist(final ID userId) {
		return getWorklist(userId, EWorkitemStatus.running, EWorkitemStatus.suspended,
				EWorkitemStatus.delegate);
	}

	@Override
	public IDataQuery<WorkitemBean> getUnreadWorklist(final ID userId) {
		final StringBuilder sql = new StringBuilder("userId2=? and readMark=?")
				.append(DEFAULT_ORDERBY);
		return query(sql.toString(), userId, Boolean.FALSE);
	}

	@Override
	public Map<String, Object> createVariables(final WorkitemBean workitem) {
		final Map<String, Object> variables = aService.createVariables(getActivity(workitem));
		variables.put("workitem", workitem);
		return variables;
	}

	@Override
	public boolean isFinalStatus(final WorkitemBean t) {
		return t.getStatus().ordinal() >= EWorkitemStatus.complete.ordinal();
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

	WorkitemBean _create(final ActivityBean activity, final Participant participant) {
		final WorkitemBean workitem = createBean();
		workitem.setProcessId(activity.getProcessId());
		workitem.setActivityId(activity.getId());
		workitem.setUserId(participant.userId);
		workitem.setUserText(permission.getUser(participant.userId).toString());
		workitem.setCreateDate(new Date());
		workitem.setRoleId(participant.roleId);
		workitem.setRoleText(permission.getRole(participant.roleId).toString());
		workitem.setUserId2(participant.userId);
		return workitem;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx() {

			@Override
			public void onAfterUpdate(final IDbEntityManager<?> manager, final String[] columns,
					final Object[] beans) {
				super.onAfterUpdate(manager, columns, beans);

				if (ArrayUtils.contains(columns, "status")) {
					for (final Object bean : beans) {
						final WorkitemBean workitem = (WorkitemBean) bean;
						for (final IWorkflowListener listener : getEventListeners(workitem)) {
							((IWorkitemListener) listener).onStatusChange(workitem);
						}
					}
				}
			}
		});
	}
}
