package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.ado.trans.TransactionVoidCallback;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.CollectionUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.task.ExecutorRunnableEx;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IActivityService;
import net.simpleframework.workflow.engine.IMappingVal;
import net.simpleframework.workflow.engine.IWorkflowForm;
import net.simpleframework.workflow.engine.PropSequential;
import net.simpleframework.workflow.engine.TasknodeUtils;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.event.IActivityListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.remote.IProcessRemoteHandler;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.EVariableMode;
import net.simpleframework.workflow.schema.EndNode;
import net.simpleframework.workflow.schema.MergeNode;
import net.simpleframework.workflow.schema.Node;
import net.simpleframework.workflow.schema.ProcessNode;
import net.simpleframework.workflow.schema.SubNode;
import net.simpleframework.workflow.schema.SubNode.VariableMapping;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ActivityService extends AbstractWorkflowService<ActivityBean>
		implements IActivityService {

	@Override
	public ProcessBean getProcessBean(final ActivityBean activity) {
		return activity == null ? null : wfpService.getBean(activity.getProcessId());
	}

	@Override
	public AbstractTaskNode getTaskNode(final ActivityBean activity) {
		return activity == null ? null
				: (AbstractTaskNode) wfpService.getProcessNode(getProcessBean(activity))
						.getNodeById(activity.getTasknodeId());
	}

	@Override
	public void doComplete(final ActivityComplete activityComplete) {
		_doComplete(activityComplete);
	}
	
	protected boolean isCreateNext(final ProcessBean process){
		//单提出来，在业务中覆盖，因为有的业务流程结束后活动的节点还需要发下一节点
		// 如果流程处在最终状态，则不创建后续环节
		return !wfpService.isFinalStatus(process);
	}

	private void _doComplete(final ActivityComplete activityComplete) {
		final ActivityBean activity = activityComplete.getActivity();

		final ProcessBean process = getProcessBean(activity);
		if (process.getStatus() == EProcessStatus.suspended) {
			throw WorkflowStatusException.of($m("WorkitemService.2"));
		}
		_assert(wfpService.getProcessModel(process), EProcessModelStatus.deploy);

		final boolean bcomplete = activityComplete.isBcomplete();
		if (bcomplete && isFinalStatus(activity)) {
			throw WorkflowStatusException.of(activity + " - " + $m("ActivityService.2"));
		}

		// 创建前事件
		for (final IWorkflowListener listener : getEventListeners(activity)) {
			((IActivityListener) listener).onBeforeComplete(activityComplete);
		}

		final Date createDate = new Date();
		ActivityBean endActivity = null;
		// 如果流程处在最终状态，则不创建后续环节
		if (isCreateNext(process)) {
			// 如果存在定义的后续环节，则按此创建
			final ActivityBean nextActivity = getFallbackNextActivity(activity);
			if (nextActivity != null) {
				// 克隆所有
				_clone(nextActivity, activity, true);
			} else {
				for (final TransitionNode transition : activityComplete.getTransitions()) {
					final AbstractTaskNode to = transition.to();
					if (to instanceof UserNode) {
						final UserNode _to = (UserNode) to;
						_doUserNode(activity, _to, activityComplete.getParticipants(transition),
								createDate);
						// 空节点直接完成
						if (_to.isEmpty()) {
							for (final ActivityBean next : getNextActivities(activity)) {
								if (next.getTasknodeId().equals(_to.getId())&&!isFinalStatus(next)) {
									//加上&&!isFinalStatus(next)因为在发送时会发送多次,而以前发的有的已经完成了
									new ActivityComplete(next).complete();
								}
							}
						}
					} else if (to instanceof MergeNode) {
						_doMergeNode(activity, (MergeNode) to, createDate);
					} else if (to instanceof SubNode) {
						_doSubNode(activity, (SubNode) to, createDate);
					} else if (to instanceof EndNode) {
						endActivity = _create(process, to, activity, createDate);
						endActivity.setStatus(EActivityStatus.complete);
						endActivity.setCompleteDate(new Date());
						insert(endActivity);
					}
				}
			}
		}

		if (!bcomplete) {
			return;
		}

		activity.setStatus(EActivityStatus.complete);
		activity.setCompleteDate(new Date());
		update(new String[] { "completeDate", "status" }, activity);

		final AbstractTaskNode tasknode = getTaskNode(activity);
		if (tasknode instanceof UserNode) {
			final WorkitemService wfwServiceImpl = (WorkitemService) wfwService;
			// 放弃未完成的工作项
			for (final WorkitemBean workitem : wfwServiceImpl.getWorkitems(activity)) {
				if (!wfwServiceImpl.isFinalStatus(workitem)) {
					wfwServiceImpl._abort(workitem);
				}
			}

			if (!TasknodeUtils.isInstanceShared(tasknode) && !TasknodeUtils.isSequential(tasknode)) {
				// 多实例并行，处理响应数
				final ArrayList<ActivityBean> al = new ArrayList<ActivityBean>();
				int completes = 0;
				for (final ActivityBean activity2 : getNextActivities(getPreActivity(activity))) {
					if (activity2.getTasknodeId().equals(tasknode.getId())) {
						al.add(activity2);
						if (activity2.getStatus() == EActivityStatus.complete) {
							completes++;
						}
					}
				}
				if (completes >= TasknodeUtils.getResponseValue(tasknode, al.size())) {
					for (int i = 0; i < al.size(); i++) {
						_abort(al.get(i));
					}
				}
			}
		}

		if (endActivity != null) {
			// 检测状态
			_assert(process, EProcessStatus.running, EProcessStatus.timeout);

			process.setStatus(EProcessStatus.complete);
			process.setCompleteDate(endActivity.getCompleteDate());
			wfpService.update(new String[] { "completeDate", "status" }, process);

			_backToProcess(process);
		}

		// 创建后事件
		for (final IWorkflowListener listener : getEventListeners(activity)) {
			((IActivityListener) listener).onCompleted(activityComplete);
		}
	}

	/* 空节点的执行者 */
	private static final String EMPTY_PARTICIPANTS = "empty_participants";

	private void _doUserNode(final ActivityBean preActivity, final UserNode to,
			final List<Participant> _participants, final Date createDate) {
		final ProcessBean process = getProcessBean(preActivity);
		final boolean instanceShared = TasknodeUtils.isInstanceShared(to);
		if (to.isEmpty()) {
			// 空节点不创建任务项，仅把定义参与者保存在属性里
			if (_participants != null) {
				if (instanceShared) {
					final ActivityBean nActivity = _create(process, to, preActivity, createDate);
					nActivity.getProperties().setProperty(EMPTY_PARTICIPANTS,
							StringUtils.join(_participants, ";"));
					insert(nActivity);
				} else {
					for (final Participant participant : _participants) {
						final ActivityBean nActivity = _create(process, to, preActivity, createDate);
						nActivity.getProperties().setProperty(EMPTY_PARTICIPANTS, participant.toString());
						insert(nActivity);
					}
				}
			}
		} else {
			final ArrayList<Participant> participants = new ArrayList<Participant>();
			final boolean sequential = TasknodeUtils.isSequential(to);
			if (_participants != null && _participants.size() > 0) {
				if (sequential) {
					participants.add(_participants.remove(0));
				} else {
					participants.addAll(_participants);
				}
			}

			if (participants.size() == 0) {
				throw WorkflowException.of($m("ActivityService.5"));
			}

			ActivityBean nActivity = null;
			for (final Participant participant : participants) {
				if (!instanceShared || nActivity == null) {
					nActivity = _create(process, to, preActivity, createDate);
					// 设置后续参与者
					if (sequential) {
						PropSequential.set(nActivity, _participants);
					}
					insert(nActivity);
				}

				// 设置过期
				final int timoutHours = Convert.toInt(eval(nActivity, to.getTimoutHours()));
				if (timoutHours > 0) {
					doUpdateTimeoutDate(nActivity, timoutHours);
				}

				final WorkitemService wfwServiceImpl = (WorkitemService) wfwService;
				wfwServiceImpl.insert(wfwServiceImpl._create(nActivity, participant, createDate));
			}
		}
	}

	@Override
	public List<Participant> getEmptyParticipants(final ActivityBean activity) {
		final List<Participant> participants = new ArrayList<Participant>();
		for (final String participant : StringUtils
				.split(activity.getProperties().getProperty(EMPTY_PARTICIPANTS), ";")) {
			participants.add(Participant.of(participant));
		}
		return participants;
	}

	private static final String MERGE_PRE_ACTIVITIES = "merge_pre_activities";

	private void _doMergeNode(final ActivityBean preActivity, final MergeNode to,
			final Date createDate) {
		// 当前的合并环节，单实例
		ActivityBean nActivity = null;
		IDataQuery<ActivityBean> dq = query("processId=? and tasknodeId=?",
				preActivity.getProcessId(), to.getId());
		if (dq.getCount() == 0) {
			insert(nActivity = _create(to, preActivity, createDate));
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
				insert(nActivity = _create(to, preActivity, createDate));
			}
		}

		// 更新每次的preActivity
		final ID preId = preActivity.getId();
		if (!preId.equals(nActivity.getPreviousId())) {
			_setMergePreActivities(nActivity, new String[] {
					nActivity.getProperties().getProperty(MERGE_PRE_ACTIVITIES), preId.toString() });
			update(new String[] { "properties" }, nActivity);
		}

		List<ActivityBean> aborts = null;

		boolean complete = false;
		Object _condition = eval(nActivity, to.getCondition());
		if (_condition == null) {
			_condition = 0;
		}
		if (_condition instanceof Boolean) {
			complete = ((Boolean) _condition).booleanValue();
		} else if (_condition instanceof Number) {
			final int count = ((Number) _condition).intValue();

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

			dq = query(ev.addExpression(")"));
			ActivityBean pre;

			if (count <= 0 || count >= dq.getCount()) {
				complete = true;
				// 查找是否存在有未完成的任务环节
				while ((pre = dq.next()) != null) {
					if (!isFinalStatus(pre) && !pre.equals(preActivity)) {
						complete = false;
						break;
					}
				}
			} else {
				aborts = new ArrayList<ActivityBean>();

				complete = false;
				int completes = 0;
				while ((pre = dq.next()) != null) {
					// 如果合并环节含有前一环节的记录，则认为是完成的
					if (_isPreviousOfMergeActivity(nActivity, pre)) {
						completes++;
					} else {
						aborts.add(pre);
					}
					if (completes >= count) {
						complete = true;
					}
				}
			}
		}

		if (complete) {
			new ActivityComplete(nActivity).complete();
			if (aborts != null && aborts.size() > 0) {
				// 放弃未完成的
				for (final ActivityBean activity : aborts) {
					_abort(activity);
				}
			}
		}
	}

	void _setMergePreActivities(final ActivityBean mActivity, final Object[] preActivities) {
		mActivity.getProperties().setProperty(MERGE_PRE_ACTIVITIES,
				StringUtils.join(preActivities, ";"));
	}

	List<String> _getMergePreActivities(final ActivityBean mActivity) {
		return ArrayUtils.asList(
				StringUtils.split(mActivity.getProperties().getProperty(MERGE_PRE_ACTIVITIES), ";"));
	}

	boolean _isPreviousOfMergeActivity(final ActivityBean mActivity,
			final ActivityBean preActivity) {
		return preActivity.getId().equals(mActivity.getPreviousId())
				|| _getMergePreActivities(mActivity).contains(preActivity.getId().toString());
	}

	void _backToProcess(final ProcessBean sProcess) {
		final Properties properties = sProcess.getProperties();
		final String serverUrl = properties.getProperty(IProcessRemoteHandler.SERVERURL);
		if (StringUtils.hasText(serverUrl)) {
			wfpService.doBackToRemote(sProcess);
		} else {
			final ActivityBean nActivity = getBean(
					properties.getProperty(IProcessRemoteHandler.SUB_ACTIVITYID));
			doSubComplete(nActivity, new IMappingVal() {
				@Override
				public Object val(final String mapping) {
					return wfpService.getVariable(sProcess, mapping);
				}
			});
		}
	}

	@Override
	public void doSubComplete(final ActivityBean activity, final IMappingVal mappingVal) {
		if (activity == null) {
			return;
		}
		final SubNode sub = (SubNode) getTaskNode(activity);
		if (sub.isSync()) {
			// 设置返回的变量，仅在同步方式
			final ProcessBean mProcess = getProcessBean(activity);
			final ProcessNode processNode = wfpService.getProcessNode(mProcess);
			for (final VariableMapping vm : sub.getMappingSet()) {
				final VariableNode vNode = processNode.getVariableNodeByName(vm.variable);
				if (vNode == null) {
					continue;
				}
				if (vNode.getMode() == EVariableMode.inout) {
					wfpService.setVariable(mProcess, vm.variable, mappingVal.val(vm.mapping));
				}
			}
		}
		new ActivityComplete(activity).complete();
	}

	private void _doSubNode(final ActivityBean preActivity, final SubNode to,
			final Date createDate) {
		final ActivityBean nActivity = _create(to, preActivity, createDate);
		insert(nActivity);

		final String url = to.getUrl();
		// 如果是远程，则通过循环任务方式，如果本地，则进行强验证，即抛出异常
		if (!StringUtils.hasText(url)) {
			final ProcessBean process = getProcessBean(preActivity);
			final KVMap variables = new KVMap();
			for (final VariableMapping vMapping : to.getMappingSet()) {
				final Object value = wfpService.getVariable(process, vMapping.variable);
				variables.add(vMapping.mapping, value);
			}
			final boolean sync = to.isSync();
			Properties properties = null;
			if (sync) {
				properties = new Properties();
				properties.setProperty(IProcessRemoteHandler.SUB_ACTIVITYID,
						String.valueOf(nActivity.getId()));
			}

			wfpService.doStartProcess(wfpmService.getProcessModel(to.getModel()), null, variables,
					properties, null);
			if (sync) {
				_status(nActivity, EActivityStatus.waiting);
			} else {
				new ActivityComplete(nActivity).complete();
			}
		} else {
			try {
				_doRemoteSubActivity(nActivity);
			} catch (final IOException e) {
				throw WorkflowException.of(e);
			}
		}
	}

	void _doRemoteSubActivity(final ActivityBean nActivity) throws IOException {
		if (nActivity == null) {
			return;
		}

		final ProcessBean mProcess = getProcessBean(nActivity);
		final SubNode sub = (SubNode) getTaskNode(nActivity);
		final KVMap data = new KVMap(); // 提交的参数

		final IProcessRemoteHandler rHandler = workflowContext.getProcessRemoteHandler();

		final EActivityStatus status = nActivity.getStatus();
		if (status == EActivityStatus.running) {
			// 模型名称、主流程的地址及实例id
			data.add(IProcessRemoteHandler.SERVERURL, wfSettings.getServerUrl());
			data.add(IProcessRemoteHandler.SUB_ACTIVITYID, nActivity.getId());
			data.add(IProcessRemoteHandler.MODEL, sub.getModel());
			int i = 0;
			final StringBuilder mappings = new StringBuilder();
			for (final VariableMapping vMapping : sub.getMappingSet()) {
				if (i++ > 0) {
					mappings.append(";");
				}
				data.add(vMapping.mapping, wfpService.getVariable(mProcess, vMapping.variable));
				mappings.append(vMapping.mapping);
			}
			if (mappings.length() > 0) {
				data.add(IProcessRemoteHandler.VAR_MAPPINGS, mappings.toString());
			}

			// 创建远程子流程实例
			final Map<String, Object> r = rHandler.call(sub.getUrl(), "startProcess", data);
			final Object processId = r.get(IProcessRemoteHandler.SUB_PROCESSID);
			if (processId != null) {
				if (sub.isSync()) {
					nActivity.setStatus(EActivityStatus.waiting);
					// 保存子流程id
					nActivity.getProperties().setProperty(IProcessRemoteHandler.SUB_PROCESSID,
							String.valueOf(processId));
					update(new String[] { "status", "properties" }, nActivity);
				} else {
					new ActivityComplete(nActivity).complete();
				}
			}
		} else if (status == EActivityStatus.waiting) {
			// 如果发现环节处在等待状态，则发送一个远程检测请求来确认子流程是否完成
			final Properties properties = nActivity.getProperties();
			data.add(IProcessRemoteHandler.SUB_PROCESSID,
					properties.get(IProcessRemoteHandler.SUB_PROCESSID));

			final Map<String, Object> r = rHandler.call(sub.getUrl(), "checkProcess", data);
			final Boolean success = (Boolean) r.get("success");
			if (success != null && success.booleanValue()) {
				// oprintln("");
			}
		}
	}

	void _abort(final ActivityBean activity) {
		_abort(activity, EActivityAbortPolicy.normal);
	}

	void _abort(final ActivityBean activity, final EActivityAbortPolicy policy) {
		_abort(activity, policy, EActivityStatus.abort);
	}

	void _abort(final ActivityBean activity, final EActivityAbortPolicy policy,
			final EActivityStatus status) {
		final WorkitemService wfwServiceImpl = (WorkitemService) wfwService;
		for (final WorkitemBean workitem : wfwServiceImpl.getWorkitems(activity)) {
			if (!wfwServiceImpl.isFinalStatus(workitem)) {
				wfwServiceImpl._abort(workitem);
			}
		}

		if (policy == EActivityAbortPolicy.nextActivities) {
			for (final ActivityBean nextActivity : getNextActivities(activity)) {
				_abort(nextActivity, policy);
			}
		}

		if (!isFinalStatus(activity)) {
			activity.setStatus(status);
			activity.setCompleteDate(new Date());
			update(new String[] { "status", "completeDate" }, activity);
		}
	}

	@Override
	public void doAbort(final ActivityBean activity, final EActivityAbortPolicy policy) {
		if (isFinalStatus(activity)) {
			throw WorkflowStatusException.of($m("ActivityService.3", activity.getStatus()));
		}
		_abort(activity, policy);
	}

	@Override
	public void doAbort(final ActivityBean activity) {
		doAbort(activity, EActivityAbortPolicy.normal);
	}

	@Override
	public void doSuspend(final ActivityBean activity) {
		_assert(activity, EActivityStatus.running);
		_status(activity, EActivityStatus.suspended);
	}

	@Override
	public void doResume(final ActivityBean activity) {
		_assert(activity, EActivityStatus.suspended);
		_status(activity, EActivityStatus.running);
	}
	
	@Override
	public ActivityBean doResumeFromAbort(final ActivityBean activity){
		return _clone(activity, null, true);
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
		buildStatusSQL(sql, params, status);
		sql.append(" order by createDate asc");
		return DataQueryUtils.toList(query(sql, params.toArray()));
	}

	@Override
	public List<ActivityBean> getActivities(final ProcessBean processBean, final Object tasknodeId) {
		return DataQueryUtils.toList(query("processId=? and tasknodeId=?", processBean.getId(),
				tasknodeId instanceof AbstractTaskNode ? ((AbstractTaskNode) tasknodeId).getId()
						: tasknodeId));
	}

	@Override
	public void doJump(final ActivityBean activity, final String taskname) {
		doJump(activity, taskname, true);
	}

	@Override
	public void doJump(final ActivityBean activity, final String taskname, final boolean bComplete) {
		_assert(activity, EActivityStatus.running);

		if (taskname == null) {
			_doComplete(new ActivityComplete(activity).setBcomplete(bComplete));
		} else {
			final AbstractTaskNode from = getTaskNode(activity);
			final ProcessNode processNode = (ProcessNode) from.getParent();
			Node to = processNode.getNodeById(taskname);
			if (to == null) {
				to = processNode.getNodeByName(taskname);
			}

			if (to instanceof UserNode || to instanceof SubNode) {
				final List<TransitionNode> transitions = new ArrayList<TransitionNode>();
				transitions.add(new TransitionNode(null, null).setFrom(from.getId()).setTo(to.getId()));
				_doComplete(new ActivityComplete(activity, transitions).setBcomplete(bComplete));
			} else {
				throw WorkflowException.of($m("ActivityService.4"));
			}
		}
	}

	/* 如果存在FALLBACK_NEXTACTIVITY，则根据FALLBACK_NEXTACTIVITY克隆后续环节，而不是路由规则 */
	private static final String FALLBACK_NEXTACTIVITY = "fallback_nextactivity";

	@Override
	public ActivityBean getFallbackNextActivity(final ActivityBean activity) {
		return getBean(activity.getProperties().getProperty(FALLBACK_NEXTACTIVITY));
	}

	@Override
	public void doFallback(final ActivityBean activity, final String taskname,
			final boolean isNextActivity) {
		if (activity.getTasknodeType() != AbstractTaskNode.TT_END) {
			_assert(activity, EActivityStatus.running);
		}
		// 验证是否存在已完成的工作
		if (wfwService.getWorkitems(activity, EWorkitemStatus.complete).size() > 0) {
			throw WorkflowException.of($m("ActivityService.0"));
		}

		// 退回前一指定任务，有环节的是直退，没有环节的是退回，
		// 要保证退回环节的状态是已完成
		final boolean fallback2 = StringUtils.hasText(taskname);
		ActivityBean preActivity = null;
		if (fallback2) {
			preActivity = getPreActivity(activity, taskname);
			while (preActivity != null && preActivity.getStatus() != EActivityStatus.complete) {
				preActivity = getPreActivity(getPreActivity(preActivity), taskname);
			}
		} else {
			preActivity = getPreActivity(activity);
			while (preActivity != null && preActivity.getStatus() != EActivityStatus.complete) {
				// 查找上一个activity节点
				final ActivityBean activity2 = getPreActivity(preActivity, activity.getTasknodeId());
				preActivity = getPreActivity(activity2);
			}
		}

		if (preActivity == null) {
			throw WorkflowException.of($m("ActivityService.8"));
		}

		// 放弃当前操作环节
		_abort(activity, EActivityAbortPolicy.nextActivities,
				fallback2 ? EActivityStatus.fallback2 : EActivityStatus.fallback);
		// 放弃所有后续
		for (final ActivityBean _activity : getNextActivities(preActivity)) {
			_abort(_activity, EActivityAbortPolicy.nextActivities);
		}

		final AbstractTaskNode to = getTaskNode(preActivity);
		if (to instanceof UserNode) {
			final ActivityBean nActivity = _clone(preActivity, activity);
			if (isNextActivity) {
				// 保存后续环节
				nActivity.getProperties().setProperty(FALLBACK_NEXTACTIVITY,
						String.valueOf(activity.getId()));
				update(new String[] { "properties" }, nActivity);
			}
		} else if (to instanceof MergeNode) {
			_clone(getBean(preActivity.getPreviousId()), activity);
			for (final String id : _getMergePreActivities(preActivity)) {
				_clone(getBean(id), activity);
			}
		} else {
			throw WorkflowException.of($m("ActivityService.1"));
		}
	}

	@Override
	public void doFallback(final ActivityBean activity) {
		doFallback(activity, null, false);
	}

	private ActivityBean _clone(final ActivityBean oActivity, final ActivityBean preActivity) {
		return _clone(oActivity, preActivity, false);
	}

	private ActivityBean _clone(final ActivityBean oActivity, ActivityBean preActivity,
			final boolean allWorkitems) {
		// 复制某一个环节实例
		final AbstractTaskNode to = getTaskNode(oActivity);
		if (preActivity == null) {
			preActivity = getBean(oActivity.getPreviousId());
		}
		// 新建一个克隆节点
		final ActivityBean nActivity = _create(to, preActivity, new Date());
		insert(nActivity);
		if (to instanceof UserNode) {
			final WorkitemService wfwServiceImpl = (WorkitemService) wfwService;
			// 克隆工作项
			final List<WorkitemBean> workitems = allWorkitems ? wfwServiceImpl.getWorkitems(oActivity)
					: wfwServiceImpl.getWorkitems(oActivity, EWorkitemStatus.complete);
			if (workitems.size() > 0) {
				if (TasknodeUtils.isSequential(to)) {
					// 先按日期排序，之后创建第一个工作项
					Collections.sort(workitems, new Comparator<WorkitemBean>() {
						@Override
						public int compare(final WorkitemBean item1, final WorkitemBean item2) {
							return item1.getCreateDate().compareTo(item2.getCreateDate());
						}
					});
					// 仅创建第一个工作项
					wfwServiceImpl._clone(nActivity, workitems.remove(0));
					// 设置后续参与者
					PropSequential.set(nActivity, workitems);
					update(new String[] { "properties" }, nActivity);
				} else {
					for (final WorkitemBean workitem : workitems) {
						wfwServiceImpl._clone(nActivity, workitem);
					}
				}
			} else {
				throw WorkflowException.of($m("ActivityService.7", to));
			}
		}
		return nActivity;
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
		final List<ActivityBean> list = DataQueryUtils
				.toList(query("previousId=?", preActivity.getId()));
		// 查找合并节点
		final IDataQuery<ActivityBean> dq = query("processId=? and tasknodeType=?",
				preActivity.getProcessId(), AbstractTaskNode.TT_MERGE);
		ActivityBean activity;
		while ((activity = dq.next()) != null) {
			if (_isPreviousOfMergeActivity(activity, preActivity) && !list.contains(activity)) {
				list.add(activity);
			}
		}
		return list;
	}

	@Override
	public List<ActivityBean> getLastNextActivities(final ActivityBean preActivity) {
		final Map<Date, List<ActivityBean>> cache = new HashMap<Date, List<ActivityBean>>();
		Date last = null;
		for (final ActivityBean activity : getNextActivities(preActivity)) {
			final Date k = activity.getCreateDate();
			List<ActivityBean> l = cache.get(k);
			if (l == null) {
				cache.put(k, l = new ArrayList<ActivityBean>());
			}
			l.add(activity);
			if (last == null || last.before(k)) {
				last = k;
			}
		}
		return cache.get(last);
	}

	@Override
	public List<ActivityBean> getNextAllActivities(final ActivityBean preActivity) {
		final List<ActivityBean> l = new ArrayList<ActivityBean>();
		for (final ActivityBean next : getNextActivities(preActivity)) {
			l.add(next);
			l.addAll(getNextActivities(next));
		}
		return l;
	}

	@Override
	public ActivityBean getPreActivity(final ActivityBean activity) {
		return activity != null ? getBean(activity.getPreviousId()) : null;
	}

	@Override
	public ActivityBean getPreActivity(final ActivityBean activity, final String taskname) {
		ActivityBean preActivity = activity;
		if (StringUtils.hasText(taskname)) {
			while (preActivity != null) {
				if (taskname.equals(preActivity.getTasknodeId())
						|| taskname.equals(getTaskNode(preActivity).getName())) {
					break;
				}
				// 优先查找合并节点
				final List<String> list = _getMergePreActivities(preActivity);
				for (int i = list.size() - 1; i >= 0; i--) {
					// 先从最新的开始查找
					final ActivityBean preActivity2 = getPreActivity(getBean(list.get(i)), taskname);
					if (preActivity2 != null) {
						return preActivity2;
					}
				}
				preActivity = getPreActivity(preActivity);
			}
		} else {
			preActivity = getPreActivity(preActivity);
		}
		return preActivity;
	}

	@Override
	public Map<String, Object> createVariables(final ActivityBean activity) {
		final Map<String, Object> variables = wfpService.createVariables(getProcessBean(activity));
		variables.put("activity", activity);
		for (final String variable : getVariableNames(activity)) {
			variables.put(variable, getVariable(activity, variable));
		}
		return variables;
	}

	@Override
	public Object getVariable(final ActivityBean activity, final String name) {
		final VariableNode variableNode = getTaskNode(activity).getVariableNodeByName(name);
		return vServiceImpl.getVariableValue(activity, variableNode);
	}

	@Override
	public void setVariable(final ActivityBean activity, final String name, final Object value) {
		setVariable(activity, new String[] { name }, new Object[] { value });
	}

	@Override
	public void setVariable(final ActivityBean activity, final String[] names,
			final Object[] values) {
		vServiceImpl.setVariableValue(activity, names, values);
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
			formClass = wfpService.getProcessNode(wfpService.getBean(activity.getProcessId()))
					.getFormClass();
		}
		return (IWorkflowForm) (formClass != null ? singleton(formClass) : null);
	}

	@Override
	public boolean isFinalStatus(final ActivityBean t) {
		return _isFinalStatus(t.getStatus());
	}

	private boolean _isFinalStatus(final EActivityStatus status) {
		return status.ordinal() >= EActivityStatus.complete.ordinal();
	}

	@Override
	public List<Participant> getParticipants(final ActivityBean activity, final boolean all) {
		final List<Participant> list = new ArrayList<Participant>();
		for (final WorkitemBean workitem : wfwService.getWorkitems(activity)) {
			if (!all && workitem.getStatus().ordinal() > EWorkitemStatus.complete.ordinal()) {
				continue;
			}
			list.add(new Participant(workitem));
		}
		return list;
	}

	@Override
	public List<Participant> getParticipants2(final ActivityBean activity) {
		final List<Participant> list = new ArrayList<Participant>();
		for (final WorkitemBean workitem : wfwService.getWorkitems(activity,
				EWorkitemStatus.complete)) {
			list.add(new Participant(workitem, true));
		}
		return list;
	}

	@Override
	public void doUpdateTimeoutDate(final ActivityBean activity, final Date timeoutDate) {
		activity.setTimeoutDate(timeoutDate);
		update(new String[] { "timeoutDate" }, activity);
	}

	@Override
	public void doUpdateTimeoutDate(final ActivityBean activity, final int hours) {
		doUpdateTimeoutDate(activity,
				getWorkCalendarListener(activity).getRealDate(activity, hours * 60));
	}

	void _doActivityTimeout() {
		final IDataQuery<ActivityBean> dq = query(
				"timeoutdate is not null and (status=? or status=?)", EActivityStatus.running,
				EActivityStatus.timeout).setFetchSize(0);
		ActivityBean activity;
		final Date n = new Date();
		while ((activity = dq.next()) != null) {
			if (wfpService.isFinalStatus(getProcessBean(activity))) {
				continue;
			}
			final ActivityBean nActivity = activity;
			doExecuteTransaction(new TransactionVoidCallback() {
				@Override
				protected void doTransactionVoidCallback() throws Throwable {
					final EActivityStatus status = nActivity.getStatus();
					if (status != EActivityStatus.timeout && n.after(nActivity.getTimeoutDate())) {
						// 设置过期状态
						nActivity.setStatus(EActivityStatus.timeout);
						update(new String[] { "status" }, nActivity);
					}

					// 触发超期检测事件，比如一些通知
					for (final IWorkflowListener listener : getEventListeners(nActivity)) {
						((IActivityListener) listener).onTimeoutCheck(nActivity);
					}
				}
			});
		}
	}

	ActivityBean _create(final AbstractTaskNode tasknode, final ActivityBean preActivity,
			final Date createDate) {
		return _create(null, tasknode, preActivity, createDate);
	}

	ActivityBean _create(ProcessBean process, final AbstractTaskNode tasknode,
			final ActivityBean preActivity, final Date createDate) {
		if (process == null && preActivity != null) {
			process = getProcessBean(preActivity);
		}
		final ActivityBean activity = createBean();
		activity.setProcessId(process.getId());
		if (preActivity != null) {
			activity.setPreviousId(preActivity.getId());
		}
		activity.setTasknodeId(tasknode.getId());
		activity.setTasknodeText(tasknode.toString());
		activity.setTasknodeType(tasknode.getTasknodeType());
		activity.setCreateDate(createDate);
		return activity;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 启动子流程监控
		final ITaskExecutor taskExecutor = workflowContext.getTaskExecutor();
		taskExecutor.addScheduledTask(new ExecutorRunnableEx("activity_subtask_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				final IDataQuery<?> qs = query("tasknodeType=? and (status=? or status=?)",
						AbstractTaskNode.TT_SUB, EActivityStatus.running, EActivityStatus.waiting)
								.setFetchSize(0);
				ActivityBean activity;
				while ((activity = (ActivityBean) qs.next()) != null) {
					final ActivityBean nActivity = activity;
					doExecuteTransaction(new TransactionVoidCallback() {
						@Override
						protected void doTransactionVoidCallback() throws Throwable {
							try {
								_doRemoteSubActivity(nActivity);
							} catch (final IOException e) {
								getLog().warn(e);
							}
						}
					});
				}
			}
		});

		// 启动过期监控
		taskExecutor.addScheduledTask(new ExecutorRunnableEx("activity_timeout_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				_doActivityTimeout();
			}
		});

		addListener(new DbEntityAdapterEx<ActivityBean>() {
			@Override
			public void onBeforeDelete(final IDbEntityManager<ActivityBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onBeforeDelete(manager, paramsValue);
				for (final ActivityBean activity : coll(manager, paramsValue)) {
					final Object id = activity.getId();
					// 删除任务环节
					wfwService.deleteWith("activityId=?", id);
					// 删除环节变量
					vServiceImpl.deleteVariables(EVariableSource.activity, id);
				}
			}

			@Override
			public void onBeforeUpdate(final IDbEntityManager<ActivityBean> manager,
					final String[] columns, final ActivityBean[] beans) throws Exception {
				super.onAfterUpdate(manager, columns, beans);

				// 事件
				if (ArrayUtils.isEmpty(columns) || ArrayUtils.contains(columns, "status", true)) {
					for (final ActivityBean activity : beans) {
						// 状态转换事件
						final EActivityStatus _status = Convert.toEnum(EActivityStatus.class,
								queryFor("status", "id=?", activity.getId()));
						if (_isFinalStatus(_status)) {
							throw WorkflowException.of($m("ActivityService.6"));
						}

						if (_status != activity.getStatus()) {
							for (final IWorkflowListener listener : getEventListeners(activity)) {
								((IActivityListener) listener).onStatusChange(activity, _status);
							}
						}
					}
				}
			}

			@Override
			public void onAfterInsert(final IDbEntityManager<ActivityBean> manager,
					final ActivityBean[] beans) throws Exception {
				super.onAfterInsert(manager, beans);

				for (final ActivityBean activity : beans) {
					// 触发创建事件
					for (final IWorkflowListener listener : getEventListeners(activity)) {
						((IActivityListener) listener).onCreated(activity);
					}
				}
			}
		});
	}
}
