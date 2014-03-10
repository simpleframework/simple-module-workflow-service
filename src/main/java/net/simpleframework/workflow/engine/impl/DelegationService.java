package net.simpleframework.workflow.engine.impl;

import java.util.Date;

import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IDelegationService;
import net.simpleframework.workflow.engine.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DelegationService extends AbstractWorkflowService<DelegationBean> implements
		IDelegationService {

	@Override
	public DelegationBean queryRunningDelegation(final WorkitemBean workitem) {
		/* 运行期的只有一个 */
		return query("delegationsource=? and sourceid=? and status<?", EDelegationSource.workitem,
				workitem.getId(), EDelegationStatus.complete).next();
	}

	@Override
	public IDataQuery<DelegationBean> queryDelegations(final Object userId) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select d.* from ")
				.append(getTablename(DelegationBean.class))
				.append(" d left join ")
				.append(getTablename(WorkitemBean.class))
				.append(
						" w on d.sourceid = w.id where w.userId=? and d.delegationsource=? order by createDate desc");
		return getEntityManager().queryBeans(
				new SQLValue(sb.toString(), userId, EDelegationSource.workitem));
	}

	@Override
	public void accept(DelegationBean delegation) {
		_assert(delegation, EDelegationStatus.receiving);
		_status(delegation, EDelegationStatus.running);
	}

	@Override
	public void abort(final DelegationBean delegation) {
		_assert(delegation, EDelegationStatus.ready, EDelegationStatus.receiving,
				EDelegationStatus.running);
		_abort(delegation);
		_updateWorkitem(delegation, EWorkitemStatus.running);
	}

	void _abort(final DelegationBean delegation) {
		_status(delegation, EDelegationStatus.abort);
	}

	@Override
	public boolean isFinalStatus(final DelegationBean t) {
		return t.getStatus().ordinal() >= EDelegationStatus.complete.ordinal();
	}

	void _doDelegateTask(final DelegationBean delegation, final boolean confirm) {
		final EDelegationStatus status = delegation.getStatus();
		if (status == EDelegationStatus.ready) {
			final Date startDate = delegation.getDstartDate();
			final Date n = new Date();
			if (startDate == null || startDate.before(n)) {
				delegation.setStatus(confirm ? EDelegationStatus.receiving : EDelegationStatus.running);
				delegation.setStartDate(n);
				update(new String[] { "status", "startDate" }, delegation);

				_updateWorkitem(delegation, EWorkitemStatus.delegate);
			}
		}
	}

	void _updateWorkitem(final DelegationBean delegation, final EWorkitemStatus status) {
		// 更新Workitem
		WorkitemBean workitem;
		if (delegation.getDelegationSource() == EDelegationSource.workitem
				&& (workitem = wService.getBean(delegation.getSourceId())) != null) {
			workitem.setStatus(status);
			final ID userId = workitem.getUserId();
			workitem.setUserId2(status == EWorkitemStatus.delegate ? delegation.getUserId() : userId);
			final ID userId2 = workitem.getUserId2();
			workitem.setUserText2(userId.equals(userId2) ? null : permission.getUser(userId2)
					.toString());
			wService.update(new String[] { "status", "userId2", "userText2" }, workitem);
		}
	}

	void _doTimeoutTask() {
		final IDataQuery<DelegationBean> dq = query("status=?", EDelegationStatus.running)
				.setFetchSize(0);
		DelegationBean delegation;
		while ((delegation = dq.next()) != null) {
			final Date endDate = delegation.getDcompleteDate();
			final Date n = new Date();
			if (endDate != null && endDate.after(n)) {
				_abort(delegation);
				_updateWorkitem(delegation, EWorkitemStatus.running);
			}
		}
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 检测是否过期
		final ITaskExecutor taskExecutor = context.getTaskExecutor();
		taskExecutor.addScheduledTask(settings.getDelegatePeriod(), new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				_doTimeoutTask();
			}
		});
	}

	DelegationBean _create(final WorkitemBean workitem, final ID userId, final Date dStartDate,
			final Date dCompleteDate, final String description) {
		final DelegationBean delegation = createBean();
		delegation.setDelegationSource(EDelegationSource.workitem);
		delegation.setSourceId(workitem.getId());
		delegation.setUserId(userId);
		delegation.setUserText(permission.getUser(userId).toString());
		delegation.setDstartDate(dStartDate);
		delegation.setDcompleteDate(dCompleteDate);
		delegation.setDescription(description);
		return delegation;
	}
}
