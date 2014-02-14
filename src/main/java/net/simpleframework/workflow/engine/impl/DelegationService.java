package net.simpleframework.workflow.engine.impl;

import java.util.Date;

import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.IDataQuery;
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
	public DelegationBean queryWorkitem(final WorkitemBean workitem) {
		return getBean("delegationsource=? and sourceid=?", EDelegationSource.workitem,
				workitem.getId());
	}

	@Override
	public IDataQuery<DelegationBean> queryWorkitems(final Object userId) {
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

	public void doDelegateTask(final DelegationBean delegation) {
		final EDelegationStatus status = delegation.getStatus();
		if (status == EDelegationStatus.ready) {
			final Date startDate = delegation.getStartDate();
			final Date n = new Date();
			if (startDate == null || startDate.before(n)) {
				delegation.setStatus(EDelegationStatus.running);
				delegation.setRunningDate(n);
				update(new String[] { "status", "runningDate" }, delegation);

				updateWorkitem(delegation, EWorkitemStatus.delegate);
			}
		} else if (status == EDelegationStatus.running) {
			final Date endDate = delegation.getEndDate();
			final Date n = new Date();
			if (endDate != null && endDate.after(n)) {
				delegation.setStatus(EDelegationStatus.abort);
				update(new String[] { "status" }, delegation);

				updateWorkitem(delegation, EWorkitemStatus.running);
			}
		}
	}

	private void updateWorkitem(final DelegationBean delegation, final EWorkitemStatus status) {
		// 更新Workitem
		WorkitemBean workitem;
		if (delegation.getDelegationSource() == EDelegationSource.workitem
				&& (workitem = wService.getBean(delegation.getSourceId())) != null) {
			workitem.setStatus(status);
			if (status == EWorkitemStatus.delegate) {
				workitem.setUserId2(delegation.getUserId());
			} else {
				workitem.setUserId2(workitem.getUserId());
			}
			wService.update(new String[] { "status", "userId2" }, workitem);
		}
	}

	public void doDelegateTask() {
		final IDataQuery<DelegationBean> dq = query("status<=?", EDelegationStatus.running)
				.setFetchSize(0);
		DelegationBean delegation;
		while ((delegation = dq.next()) != null) {
			doDelegateTask(delegation);
		}
	}

	@Override
	public void doAbort(final DelegationBean delegation) {
		assertStatus(delegation, EDelegationStatus.ready, EDelegationStatus.running);
		delegation.setStatus(EDelegationStatus.abort);
		dService.update(new String[] { "status" }, delegation);

		updateWorkitem(delegation, EWorkitemStatus.running);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		final ITaskExecutor taskExecutor = context.getTaskExecutor();
		taskExecutor.addScheduledTask(settings.getDelegatePeriod(), new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				doDelegateTask();
			}
		});
	}

	@Override
	public boolean isFinalStatus(final DelegationBean t) {
		return t.getStatus().ordinal() >= EDelegationStatus.complete.ordinal();
	}
}
