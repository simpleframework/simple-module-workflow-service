package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityBean;
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
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.event.IWorkitemListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;
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
		return pService.getBean(getActivity(workitem).getProcessId());
	}

	@Override
	public void complete(final WorkitemComplete workitemComplete) {
		final WorkitemBean workitem = workitemComplete.getWorkitem();
		assertStatus(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);
		try {
			final ActivityBean activity = getActivity(workitem);
			assertStatus(activity, EActivityStatus.running);
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

			// 设置委托
			final DelegationBean delegation = dService.queryWorkitem(workitem);
			if (delegation != null) {
				delegation.setCompleteDate(new Date());
				delegation.setStatus(EDelegationStatus.complete);
				dService.update(new String[] { "completeDate", "status" }, delegation);
			}

			if (workitemComplete.isAllCompleted()) {
				aService.complete(workitemComplete.getActivityComplete());
			} else {
				final Collection<Participant> participants = PropSequential.list(activity);
				if (participants.size() > 0) { // 获取顺序执行的参与者
					final AbstractTaskNode tasknode = aService.getTaskNode(activity);
					final Iterator<Participant> it = participants.iterator();
					if (ParticipantUtils.isInstanceShared(tasknode)) {
						insert(createWorkitem(activity, it.next()));
						PropSequential.set(activity, it);
						aService.update(new String[] { "properties" }, activity);
					} else {
						final ActivityBean nActivity = aService.createActivity(process, tasknode,
								activity);
						final Participant participant = it.next();
						PropSequential.set(nActivity, it);
						aService.insert(nActivity);
						insert(createWorkitem(nActivity, participant));

						activity.setStatus(EActivityStatus.complete);
						activity.setCompleteDate(new Date());
						aService.update(new String[] { "completeDate", "status" }, activity);
					}
				}
			}

			workitemComplete.done();
		} finally {
			workitemComplete.reset();
		}
	}

	@Override
	public void retake(final WorkitemBean workitem) {
		assertStatus(workitem, EWorkitemStatus.complete);
		final ActivityBean activity = getActivity(workitem);
		final ProcessBean process = aService.getProcessBean(activity);
		assertStatus(process, EProcessStatus.running);

		ActivityBean nActivity = null;
		final EActivityStatus status = activity.getStatus();
		if (status == EActivityStatus.complete) {
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

					final List<String> preActivities = aService.getMergePreActivities(nextActivity);
					final int size = preActivities.size();
					if (size > 0) {
						if (activity.getId().toString().equals(preActivities.remove(size - 1))) {
							// 新建merge环节
							if (status2 == EActivityStatus.running) {
								aService.setMergePreActivities(nextActivity, preActivities.toArray());
								aService.update(new String[] { "properties" }, nextActivity);
							} else if (status2 == EActivityStatus.complete) {
								final ActivityBean mActivity = aService.createActivity(tasknode,
										aService.getBean(nextActivity.getPreviousId()));
								aService.setMergePreActivities(mActivity, preActivities.toArray());
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
			nActivity = aService.createActivity(aService.getTaskNode(activity),
					aService.getBean(activity.getPreviousId()));
			aService.insert(nActivity);
		} else if (status == EActivityStatus.running) {
			nActivity = activity;
			// 顺序，单实例
			if (ParticipantUtils.isSequential(aService.getTaskNode(activity))) {
				final List<WorkitemBean> list = getWorkitemList(activity, EWorkitemStatus.running);
				if (list.size() > 0) {
					final WorkitemBean workitem2 = list.get(0);
					if (workitem2.isReadMark()) {
						throw WorkflowException.of($m("WorkitemService.1"));
					}
					workitem2.setStatus(EWorkitemStatus.abort);
					update(new String[] { "status" }, workitem2);

					PropSequential.push(activity,
							new Participant(workitem2.getUserId(), workitem2.getRoleId()));
					aService.update(new String[] { "properties" }, activity);
				}
			}
		} else {
			throw WorkflowStatusException
					.of(status, EActivityStatus.running, EActivityStatus.complete);
		}

		if (nActivity != null) {
			workitem.setStatus(EWorkitemStatus.retake);
			update(new String[] { "status" }, workitem);

			insert(createWorkitem(nActivity,
					new Participant(workitem.getUserId(), workitem.getRoleId())));
		}
	}

	private void assertRetakeWorkitems(final ActivityBean activity) {
		for (final WorkitemBean workitem : getWorkitemList(activity)) {
			if (workitem.isReadMark() || workitem.getStatus() != EWorkitemStatus.running) {
				throw WorkflowException.of($m("WorkitemService.1"));
			}
		}
	}

	@Override
	public void readMark(final WorkitemBean workitem, final boolean unread) {
		assertStatus(workitem, EWorkitemStatus.running, EWorkitemStatus.delegate);
		workitem.setReadMark(!unread);
		update(new String[] { "readMark" }, workitem);
	}

	@Override
	public void setWorkitemDelegation(final WorkitemBean workitem, final ID userId,
			final Date startDate, final Date endDate, final String description) {
		assertStatus(workitem, EWorkitemStatus.running);
		final DelegationBean delegation = dService.createBean();
		delegation.setDelegationSource(EDelegationSource.workitem);
		delegation.setSourceId(workitem.getId());
		delegation.setUserId(userId);
		delegation.setUserText(permission.getUser(userId).toString());
		delegation.setStartDate(startDate);
		delegation.setEndDate(endDate);
		delegation.setDescription(description);
		dService.insert(delegation);

		// 执行...
		dService.doDelegateTask(delegation);
	}

	@Override
	public List<WorkitemBean> getWorkitemList(final ActivityBean activity,
			final EWorkitemStatus... status) {
		final List<WorkitemBean> list = new ArrayList<WorkitemBean>();
		if (activity != null) {
			_setWorkitemList(list, query("activityId=?", activity.getId()), status);
		}
		return list;
	}

	@Override
	public List<WorkitemBean> getWorkitemList(final ID userId, final EWorkitemStatus... status) {
		final List<WorkitemBean> list = new ArrayList<WorkitemBean>();
		if (userId != null) {
			_setWorkitemList(list, query("userId2=?", userId), status);
		}
		return list;
	}

	private void _setWorkitemList(final List<WorkitemBean> list, final IDataQuery<WorkitemBean> dq,
			final EWorkitemStatus... status) {
		WorkitemBean workitem;
		while ((workitem = dq.next()) != null) {
			if (ArrayUtils.isEmpty(status) || ArrayUtils.contains(status, workitem.getStatus())) {
				list.add(workitem);
			}
		}
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
	public void deleteProcess(final WorkitemBean workitem) {
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

	WorkitemBean createWorkitem(final ActivityBean activity, final Participant participant) {
		final WorkitemBean workitem = createBean();
		workitem.setActivityId(activity.getId());
		workitem.setUserId(participant.userId);
		workitem.setUserText(permission.getUser(participant.userId).toString());
		workitem.setCreateDate(activity.getCreateDate());
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
