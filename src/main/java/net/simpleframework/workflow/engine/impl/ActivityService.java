package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.CollectionUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IActivityService;
import net.simpleframework.workflow.engine.IMappingVal;
import net.simpleframework.workflow.engine.IWorkflowForm;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.PropSequential;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.event.IActivityEventListener;
import net.simpleframework.workflow.engine.event.IWorkflowEventListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;
import net.simpleframework.workflow.engine.remote.IProcessRemote;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.EVariableMode;
import net.simpleframework.workflow.schema.EndNode;
import net.simpleframework.workflow.schema.MergeNode;
import net.simpleframework.workflow.schema.ProcessNode;
import net.simpleframework.workflow.schema.SubNode;
import net.simpleframework.workflow.schema.SubNode.VariableMapping;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ActivityService extends AbstractWorkflowService<ActivityBean> implements
		IActivityService {

	@Override
	public void complete(final ActivityComplete activityComplete) {
		final ActivityBean activity = activityComplete.getActivity();
		if (isFinalStatus(activity)) {
			throw WorkflowStatusException.of($m("ActivityService.2"));
		}

		final ProcessBean process = getProcessBean(activity);
		ActivityBean endActivity = null;
		// 如果流程处在最终状态，则不创建后续环节
		if (!pService.isFinalStatus(process)) {
			for (final TransitionNode transition : activityComplete.getTransitions()) {
				final AbstractTaskNode to = transition.to();
				if (to instanceof UserNode) {
					doUserNode(activity, (UserNode) to, activityComplete.getParticipants(transition));
				} else if (to instanceof MergeNode) {
					doMergeNode(activity, (MergeNode) to);
				} else if (to instanceof SubNode) {
					doSubNode(activity, (SubNode) to);
				} else if (to instanceof EndNode) {
					endActivity = createActivity(process, to, activity);
					endActivity.setStatus(EActivityStatus.complete);
					endActivity.setCompleteDate(new Date());
					insert(endActivity);
				}
			}
		}

		activity.setStatus(EActivityStatus.complete);
		activity.setCompleteDate(new Date());
		update(new String[] { "completeDate", "status" }, activity);

		final AbstractTaskNode tasknode = getTaskNode(activity);
		if (tasknode instanceof UserNode) {
			// 放弃未完成的工作项
			for (final WorkitemBean workitem : wService.getWorkitemList(activity)) {
				final EWorkitemStatus status = workitem.getStatus();
				if (status == EWorkitemStatus.running || status == EWorkitemStatus.suspended) {
					workitem.setStatus(EWorkitemStatus.abort);
					wService.update(new String[] { "status" }, workitem);
				}
			}

			if (!ParticipantUtils.isInstanceShared(tasknode)
					&& !ParticipantUtils.isSequential(tasknode)) {
				// 多实例，响应数
				final ArrayList<ActivityBean> al = new ArrayList<ActivityBean>();
				int complete = 0;
				for (final ActivityBean activity2 : getNextActivities(getPreActivity(activity))) {
					if (activity2.getTasknodeId().equals(tasknode.getId())) {
						al.add(activity2);
						if (activity2.getStatus() == EActivityStatus.complete) {
							complete++;
						}
					}
				}
				if (complete >= ParticipantUtils.getResponseValue(tasknode, al.size())) {
					for (int i = 0; i < al.size(); i++) {
						_abort(al.get(i));
					}
				}
			}
		}

		if (endActivity != null) {
			process.setStatus(EProcessStatus.complete);
			process.setCompleteDate(endActivity.getCompleteDate());
			pService.update(new String[] { "completeDate", "status" }, process);

			backToProcess(process);
		}
	}

	private void doUserNode(final ActivityBean activity, final UserNode to,
			final Collection<Participant> _participants) {
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		Iterator<Participant> it = null;
		if (_participants != null && _participants.size() > 0) {
			if (ParticipantUtils.isSequential(to)) {
				it = _participants.iterator();
				participants.add(it.next());
			} else {
				participants.addAll(_participants);
			}
		}

		final boolean instanceShared = ParticipantUtils.isInstanceShared(to);
		final ProcessBean process = getProcessBean(activity);
		ActivityBean nActivity = null;
		for (final Participant participant : participants) {
			if (!instanceShared || nActivity == null) {
				nActivity = createActivity(process, to, activity);
				PropSequential.set(nActivity, it);
				insert(nActivity);
			}
			wService.insert(wService.createWorkitem(nActivity, participant));
		}
	}

	private static final String MERGE_PRE_ACTIVITIES = "merge_pre_activities";

	private void doMergeNode(final ActivityBean preActivity, final MergeNode to) {
		// 当前的合并环节，单实例
		ActivityBean nActivity = null;
		IDataQuery<ActivityBean> dq = query("processId=? and tasknodeId=?",
				preActivity.getProcessId(), to.getId());
		if (dq.getCount() == 0) {
			insert(nActivity = createActivity(to, preActivity));
		} else {
			// 查找非最终态的实例
			ActivityBean activity;
			while ((activity = dq.next()) != null) {
				if (!isFinalStatus(activity)) {
					nActivity = activity;
					break;
				}
			}
			// 如果全部是最终态，则创建，可能由于回退造成
			if (nActivity == null) {
				insert(nActivity = createActivity(to, preActivity));
			}
		}

		// 更新每次的preActivity
		final ID preId = preActivity.getId();
		if (!preId.equals(nActivity.getPreviousId())) {
			setMergePreActivities(
					nActivity,
					new String[] { nActivity.getProperties().getProperty(MERGE_PRE_ACTIVITIES),
							preId.toString() });
			update(new String[] { "properties" }, nActivity);
		}

		// 判断合并环节之前是否还有活动的
		ExpressionValue ev = null;
		for (final TransitionNode t : to.fromTransitions()) {
			if (ev == null) {
				ev = new ExpressionValue("processId=? and (");
				ev.addValues(nActivity.getProcessId());
			} else {
				ev.addExpression(" or ");
			}
			ev.addExpression("tasknodeId=?");
			ev.addValues(t.from().getId());
		}
		dq = getEntityManager().queryBeans(ev.addExpression(")"));
		ActivityBean pre;
		final int count = to.getCount();
		final List<ActivityBean> aborts = new ArrayList<ActivityBean>();
		boolean complete;
		if (count <= 0 && count >= dq.getCount()) {
			complete = true;
			// 查找是否存在有未完成的任务环节
			while ((pre = dq.next()) != null) {
				if (!isFinalStatus(pre) && !pre.equals(preActivity)) {
					complete = false;
					break;
				}
			}
		} else {
			complete = false;
			int completes = 0;
			while ((pre = dq.next()) != null) {
				// 如果合并环节含有前一环节的记录，则认为是完成的
				if (isPreviousOfMergeActivity(nActivity, pre)) {
					completes++;
				} else {
					aborts.add(pre);
				}
				if (completes >= count) {
					complete = true;
				}
			}
		}
		if (complete) {
			new ActivityComplete(nActivity).complete();
			if (aborts.size() > 0) {
				// 放弃未完成的
				for (final ActivityBean activity : aborts) {
					_abort(activity);
				}
			}
		}
	}

	void setMergePreActivities(final ActivityBean mActivity, final Object[] preActivities) {
		mActivity.getProperties().setProperty(MERGE_PRE_ACTIVITIES,
				StringUtils.join(preActivities, ";"));
	}

	List<String> getMergePreActivities(final ActivityBean mActivity) {
		return ArrayUtils.asList(StringUtils.split(
				mActivity.getProperties().getProperty(MERGE_PRE_ACTIVITIES), ";"));
	}

	boolean isPreviousOfMergeActivity(final ActivityBean mActivity, final ActivityBean preActivity) {
		return preActivity.getId().equals(mActivity.getPreviousId())
				|| getMergePreActivities(mActivity).contains(preActivity.getId().toString());
	}

	private void backToProcess(final ProcessBean sProcess) {
		final Properties properties = sProcess.getProperties();
		final String serverUrl = properties.getProperty(IProcessRemote.SERVERURL);
		if (StringUtils.hasText(serverUrl)) {
			pService.backToRemote(sProcess);
		} else {
			final ActivityBean nActivity = getBean(properties
					.getProperty(IProcessRemote.SUB_ACTIVITYID));
			subComplete(nActivity, new IMappingVal() {
				@Override
				public Object val(final String mapping) {
					return pService.getVariable(sProcess, mapping);
				}
			});
		}
	}

	@Override
	public void subComplete(final ActivityBean activity, final IMappingVal mappingVal) {
		if (activity == null) {
			return;
		}
		final SubNode sub = (SubNode) getTaskNode(activity);
		if (sub.isSync()) {
			// 设置返回的变量，仅在同步方式
			final ProcessBean mProcess = getProcessBean(activity);
			final ProcessNode processNode = pService.getProcessNode(mProcess);
			for (final VariableMapping vm : sub.getMappingSet()) {
				final VariableNode vNode = processNode.getVariableNodeByName(vm.variable);
				if (vNode == null) {
					continue;
				}
				if (vNode.getMode() == EVariableMode.inout) {
					pService.setVariable(mProcess, vm.variable, mappingVal.val(vm.mapping));
				}
			}
		}
		new ActivityComplete(activity).complete();
	}

	private void doSubNode(final ActivityBean preActivity, final SubNode to) {
		final ActivityBean nActivity = createActivity(to, preActivity);
		insert(nActivity);

		final String url = to.getUrl();
		// 如果是远程，则通过循环任务方式，如果本地，则进行强验证，即抛出异常
		if (!StringUtils.hasText(url)) {
			final ProcessBean process = getProcessBean(preActivity);
			final KVMap variables = new KVMap();
			for (final VariableMapping vMapping : to.getMappingSet()) {
				final Object value = pService.getVariable(process, vMapping.variable);
				variables.add(vMapping.mapping, value);
			}
			final boolean sync = to.isSync();
			Properties properties = null;
			if (sync) {
				properties = new Properties();
				properties
						.setProperty(IProcessRemote.SUB_ACTIVITYID, String.valueOf(nActivity.getId()));
			}
			pService
					.startProcess(mService.getProcessModel(to.getModel()), variables, properties, null);
			if (sync) {
				nActivity.setStatus(EActivityStatus.waiting);
				update(new String[] { "status" }, nActivity);
			} else {
				new ActivityComplete(nActivity).complete();
			}
		} else {
			doRemoteSubTask(nActivity);
		}
	}

	@Override
	public void doRemoteSubTask(final ActivityBean activity) {
		final ID activityId = activity.getId();
		final ITaskExecutor taskExecutor = context.getTaskExecutor();
		taskExecutor.addScheduledTask(settings.getSubTaskPeriod(), new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				final ActivityBean nActivity = getBean(activityId);
				if (nActivity == null) {
					taskExecutor.removeScheduledTask(this);
					return;
				}

				final ProcessBean mProcess = getProcessBean(nActivity);
				final SubNode sub = (SubNode) getTaskNode(nActivity);
				final KVMap data = new KVMap(); // 提交的参数

				final EActivityStatus status = nActivity.getStatus();
				if (status == EActivityStatus.running) {
					// 模型名称、主流程的地址及实例id
					data.add(IProcessRemote.SERVERURL, settings.getServerUrl());
					data.add(IProcessRemote.SUB_ACTIVITYID, nActivity.getId());
					data.add(IProcessRemote.MODEL, sub.getModel());
					int i = 0;
					final StringBuilder mappings = new StringBuilder();
					for (final VariableMapping vMapping : sub.getMappingSet()) {
						if (i++ > 0) {
							mappings.append(";");
						}
						data.add(vMapping.mapping, pService.getVariable(mProcess, vMapping.variable));
						mappings.append(vMapping.mapping);
					}
					if (mappings.length() > 0) {
						data.add(IProcessRemote.VAR_MAPPINGS, mappings.toString());
					}

					// 创建远程子流程实例
					final Map<String, Object> r = context.getRemoteService().call(sub.getUrl(),
							"startProcess", data);
					final Object processId = r.get(IProcessRemote.SUB_PROCESSID);
					if (processId != null) {
						if (sub.isSync()) {
							nActivity.setStatus(EActivityStatus.waiting);
							// 保存子流程id
							nActivity.getProperties().setProperty(IProcessRemote.SUB_PROCESSID,
									String.valueOf(processId));
							getEntityManager(ActivityBean.class).update(
									new String[] { "status", "properties" }, nActivity);
						} else {
							new ActivityComplete(nActivity).complete();
						}
						taskExecutor.removeScheduledTask(this);
					}
				} else if (status == EActivityStatus.waiting) {
					// 如果发现环节处在等待状态，则发送一个远程检测请求来确认子流程是否完成
					final Properties properties = nActivity.getProperties();
					data.add(IProcessRemote.SUB_PROCESSID, properties.get(IProcessRemote.SUB_PROCESSID));
					try {
						final Map<String, Object> r = context.getRemoteService().call(sub.getUrl(),
								"checkProcess", data);
						final Boolean success = (Boolean) r.get("success");
						if (success != null && success.booleanValue()) {
							taskExecutor.removeScheduledTask(this);
						}
					} catch (final IOException e) {
						// 忽略。启动时调用，不抛出异常
						log.warn(e);
					}
				}
			}
		});
	}

	void _abort(final ActivityBean activity) {
		_abort(activity, EActivityAbortPolicy.normal);
	}

	void _abort(final ActivityBean activity, final EActivityAbortPolicy policy) {
		_abort(activity, policy, false);
	}

	void _abort(final ActivityBean activity, final EActivityAbortPolicy policy,
			final boolean fallback) {
		for (final WorkitemBean workitem : wService.getWorkitemList(activity)) {
			if (!wService.isFinalStatus(workitem)) {
				workitem.setStatus(EWorkitemStatus.abort);
				wService.update(new String[] { "status" }, workitem);
			}
		}

		if (policy == EActivityAbortPolicy.nextActivities) {
			for (final ActivityBean nextActivity : getNextActivities(activity)) {
				_abort(nextActivity, policy);
			}
		}

		if (!isFinalStatus(activity)) {
			// fallback可以理解为abort
			activity.setStatus(fallback ? EActivityStatus.fallback : EActivityStatus.abort);
			update(new String[] { "status" }, activity);
		}
	}

	@Override
	public void abort(final ActivityBean activity, final EActivityAbortPolicy policy) {
		if (isFinalStatus(activity)) {
			throw WorkflowStatusException.of($m("ActivityService.3", activity.getStatus()));
		}
		_abort(activity, policy);
	}

	@Override
	public void abort(final ActivityBean activity) {
		abort(activity, EActivityAbortPolicy.normal);
	}

	@Override
	public void suspend(final ActivityBean activity) {
		assertStatus(activity, EActivityStatus.running);
		activity.setStatus(EActivityStatus.suspended);
		update(new String[] { "status" }, activity);
	}

	@Override
	public void resume(final ActivityBean activity) {
		assertStatus(activity, EActivityStatus.suspended);
		activity.setStatus(EActivityStatus.running);
		update(new String[] { "status" }, activity);
	}

	@Override
	public List<ActivityBean> getActivities(final ProcessBean processBean,
			final EActivityStatus... status) {
		if (processBean == null) {
			return CollectionUtils.EMPTY_LIST();
		}
		final StringBuilder sql = new StringBuilder();
		final ArrayList<Object> params = new ArrayList<Object>();
		sql.append("processId=?");
		params.add(processBean.getId());
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final EActivityStatus s : status) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
		sql.append(" order by createDate asc");
		return DataQueryUtils.toList(query(sql.toString(), params.toArray()));
	}

	@Override
	public void jump(final ActivityBean activity, final String tasknode) {
		assertStatus(activity, EActivityStatus.running);

		// final ProcessNode processNode = (ProcessNode)
		// activity.taskNode().parent();
		//
		// processNode.getNodeByName(tasknodeName);
	}

	@Override
	public void fallback(final ActivityBean activity, final String tasknode) {
		assertStatus(activity, EActivityStatus.running);

		// 验证是否存在已完成的工作
		if (wService.getWorkitemList(activity, EWorkitemStatus.complete).size() > 0) {
			throw WorkflowException.of($m("ActivityService.0"));
		}

		// 退回前一指定任务
		final ActivityBean preActivity = getPreActivity(activity, tasknode);
		AbstractTaskNode to;
		if (preActivity == null || !((to = getTaskNode(preActivity)) instanceof UserNode)) {
			throw WorkflowException.of($m("ActivityService.1"));
		}

		final ActivityBean nActivity = createActivity(to, getBean(preActivity.getPreviousId()));

		final List<WorkitemBean> workitems = wService.getWorkitemList(preActivity,
				EWorkitemStatus.complete);
		if (ParticipantUtils.isSequential(to)) {
			// 先按日期排序，之后创建第一个工作项
			Collections.sort(workitems, new Comparator<WorkitemBean>() {
				@Override
				public int compare(final WorkitemBean item1, final WorkitemBean item2) {
					return item1.getCreateDate().compareTo(item2.getCreateDate());
				}
			});
			final ArrayList<Participant> al = new ArrayList<Participant>();
			for (final WorkitemBean item : workitems) {
				al.add(new Participant(item.getUserId(), item.getRoleId()));
			}
			final Iterator<Participant> it = al.iterator();
			final Participant first = it.next();
			PropSequential.set(nActivity, it);
			insert(nActivity);
			wService.insert(wService.createWorkitem(nActivity, first));
		} else {
			insert(nActivity);
			for (final WorkitemBean workitem : workitems) {
				final Participant p = new Participant(workitem.getUserId(), workitem.getRoleId());
				wService.insert(wService.createWorkitem(nActivity, p));
			}
		}

		// 放弃后续
		for (final ActivityBean _activity : getNextActivities(preActivity)) {
			_abort(_activity, EActivityAbortPolicy.nextActivities,
					_activity.getId().equals(activity.getId()));
		}
	}

	@Override
	public void fallback(final ActivityBean activity) {
		fallback(activity, null);
	}

	@Override
	public ActivityBean getStartActivity(final ProcessBean processBean) {
		return getBean("processId=? and previousId is null", processBean.getId());
	}

	@Override
	public List<ActivityBean> getNextActivities(final ActivityBean preActivity) {
		if (preActivity == null) {
			return CollectionUtils.EMPTY_LIST();
		}
		final List<ActivityBean> list = DataQueryUtils.toList(query("previousId=?",
				preActivity.getId()));
		// 查找合并节点
		final IDataQuery<ActivityBean> dq = query("processId=? and tasknodeType=?",
				preActivity.getProcessId(), AbstractTaskNode.TT_MERGE);
		ActivityBean activity;
		while ((activity = dq.next()) != null) {
			if (isPreviousOfMergeActivity(activity, preActivity) && !list.contains(activity)) {
				list.add(activity);
			}
		}
		return list;
	}

	@Override
	public ActivityBean getPreActivity(final ActivityBean activity) {
		return activity != null ? getBean(activity.getPreviousId()) : null;
	}

	@Override
	public ActivityBean getPreActivity(final ActivityBean activity, final String tasknode) {
		ActivityBean preActivity = getPreActivity(activity);
		while (preActivity != null && tasknode != null) {
			if (tasknode.equals(preActivity.getTasknodeId())
					|| tasknode.equals(getTaskNode(preActivity).getName())) {
				break;
			}
			preActivity = getPreActivity(preActivity);
		}
		return preActivity;
	}

	@Override
	public ProcessBean getProcessBean(final ActivityBean activity) {
		return pService.getBean(activity.getProcessId());
	}

	@Override
	public AbstractTaskNode getTaskNode(final ActivityBean activity) {
		return (AbstractTaskNode) pService.getProcessNode(getProcessBean(activity)).getNodeById(
				activity.getTasknodeId());
	}

	@Override
	public Map<String, Object> createVariables(final ActivityBean activity) {
		final Map<String, Object> variables = pService.createVariables(getProcessBean(activity));
		variables.put("activity", activity);
		for (final String variable : getVariableNames(activity)) {
			variables.put(variable, getVariable(activity, variable));
		}
		return variables;
	}

	@Override
	public Object getVariable(final ActivityBean activity, final String name) {
		final VariableNode variableNode = getTaskNode(activity).getVariableNodeByName(name);
		return vService.getVariableValue(activity, variableNode);
	}

	@Override
	public void setVariable(final ActivityBean activity, final String name, final Object value) {
		setVariable(activity, new String[] { name }, new Object[] { value });
	}

	@Override
	public void setVariable(final ActivityBean activity, final String[] names, final Object[] values) {
		vService.setVariableValue(activity, names, values);
	}

	@Override
	public Collection<String> getVariableNames(final ActivityBean activity) {
		return getTaskNode(activity).variables().keySet();
	}

	@Override
	public IWorkflowForm getWorkflowForm(final ActivityBean activity) {
		String formClass = null;
		final AbstractTaskNode tasknode = getTaskNode(activity);
		if (tasknode instanceof UserNode) {
			formClass = ((UserNode) tasknode).getFormClass();
		}
		if (formClass == null) {
			formClass = ((ProcessNode) tasknode.getParent()).getFormClass();
		}
		return (IWorkflowForm) (formClass != null ? singleton(formClass) : null);
	}

	@Override
	public boolean isFinalStatus(final ActivityBean t) {
		return t.getStatus().ordinal() >= EActivityStatus.complete.ordinal();
	}

	@Override
	public Set<PermissionUser> getParticipants(final ActivityBean activity, final boolean all) {
		final Set<PermissionUser> set = new LinkedHashSet<PermissionUser>();
		for (final WorkitemBean workitem : wService.getWorkitemList(activity)) {
			if (!all && workitem.getStatus().ordinal() > EWorkitemStatus.complete.ordinal()) {
				continue;
			}
			set.add(permission.getUser(workitem.getUserId()));
		}
		return set;
	}

	@Override
	public Set<PermissionUser> getParticipants2(final ActivityBean activity) {
		final Set<PermissionUser> set = new LinkedHashSet<PermissionUser>();
		for (final WorkitemBean workitem : wService.getWorkitemList(activity,
				EWorkitemStatus.complete)) {
			set.add(permission.getUser(workitem.getUserId2()));
		}
		return set;
	}

	ActivityBean createActivity(final AbstractTaskNode tasknode, final ActivityBean preActivity) {
		return createActivity(null, tasknode, preActivity);
	}

	ActivityBean createActivity(ProcessBean process, final AbstractTaskNode tasknode,
			final ActivityBean preActivity) {
		if (process == null && preActivity != null) {
			process = getProcessBean(preActivity);
		}
		final ActivityBean activity = createBean();
		activity.setProcessId(process.getId());
		if (preActivity != null) {
			activity.setPreviousId(preActivity.getId());
		}
		activity.setTasknodeId(tasknode.getId());
		activity.setTasknodeType(tasknode.getTasknodeType());
		activity.setCreateDate(new Date());
		return activity;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 启动子流程监控
		final IDataQuery<?> qs = query("tasknodeType=? and (status=? or status=?)",
				AbstractTaskNode.TT_SUB, EActivityStatus.running, EActivityStatus.waiting)
				.setFetchSize(0);
		ActivityBean activity;
		while ((activity = (ActivityBean) qs.next()) != null) {
			doRemoteSubTask(activity);
		}

		// 添加监听器
		addListener(new DbEntityAdapterEx() {
			@Override
			public void onBeforeDelete(final IDbEntityManager<?> manager,
					final IParamsValue paramsValue) {
				super.onBeforeDelete(manager, paramsValue);
				for (final ActivityBean activity : coll(paramsValue)) {
					final Object id = activity.getId();
					// 删除任务环节
					wService.deleteWith("activityId=?", id);
					// 删除环节变量
					vService.deleteVariables(EVariableSource.activity, id);
				}
			}

			@Override
			public void onAfterUpdate(final IDbEntityManager<?> manager, final String[] columns,
					final Object[] beans) {
				super.onAfterUpdate(manager, columns, beans);

				// 事件
				if (ArrayUtils.contains(columns, "status")) {
					for (final Object bean : beans) {
						final ActivityBean activity = (ActivityBean) bean;
						for (final IWorkflowEventListener listener : getEventListeners(activity)) {
							((IActivityEventListener) listener).onStatusChange(activity);
						}
					}
				}
			}
		});
	}
}
