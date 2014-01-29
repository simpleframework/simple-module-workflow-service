package net.simpleframework.workflow.engine.impl;

import java.util.Date;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.EDelegationStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DelegationService extends AbstractWorkflowService<DelegationBean> {

	public void doDelegateTask(final DelegationBean delegation) {
		final EDelegationStatus status = delegation.getStatus();
		if (status == EDelegationStatus.ready) {
			final Date startDate = delegation.getStartDate();
			final Date n = new Date();
			if (startDate == null || startDate.before(n)) {
				delegation.setStatus(EDelegationStatus.running);
				delegation.setRunningDate(n);
				update(new String[] { "status", "runningDate" }, delegation);
			}
		} else if (status == EDelegationStatus.running) {
			final Date endDate = delegation.getEndDate();
			final Date n = new Date();
			if (endDate != null && endDate.after(n)) {
				delegation.setStatus(EDelegationStatus.abort);
				delegation.setCompleteDate(n);
				update(new String[] { "status", "runningDate" }, delegation);
			}
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
}
