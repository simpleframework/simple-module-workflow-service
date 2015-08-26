package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.Date;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.ctx.task.ExecutorRunnable;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IDelegationService;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

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
		return _queryRunningDelegation(EDelegationSource.workitem, workitem.getId());
	}

	@Override
	public DelegationBean queryRunningDelegation(final ID userId) {
		return _queryRunningDelegation(EDelegationSource.user, userId);
	}

	private DelegationBean _queryRunningDelegation(final EDelegationSource delegationSource,
			final ID sourceId) {
		return getBean("delegationsource=? and sourceid=? and status<?", delegationSource, sourceId,
				EDelegationStatus.complete);
	}

	@Override
	public IDataQuery<DelegationBean> queryDelegations(final ID userId,
			final EDelegationSource source) {
		final StringBuilder sb = new StringBuilder();
		if (source == EDelegationSource.workitem) {
			sb.append("select d.*,w.processid from ")
					.append(getTablename(DelegationBean.class))
					.append(" d left join ")
					.append(getTablename(WorkitemBean.class))
					.append(
							" w on d.sourceid = w.id where w.userId=? and d.delegationsource=? order by d.createdate desc");
			return query(new SQLValue(sb, userId, source));
		} else {
			return query("delegationsource=? and sourceid=? order by createdate desc", source, userId);
		}
	}

	@Override
	public void doAccept(final DelegationBean delegation) {
		_assert(delegation, EDelegationStatus.receiving);
		_status(delegation, EDelegationStatus.running);
	}

	@Override
	public void doAbort(final DelegationBean delegation) {
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

	@Override
	public void doUserDelegation(final ID sourceId, final ID userId, final Date dStartDate,
			final Date dCompleteDate, final String description) {
		DelegationBean delegation = queryRunningDelegation(sourceId);
		if (delegation != null) {
			throw WorkflowException.of($m("DelegationService.0"));
		}

		delegation = _create(EDelegationSource.user, sourceId, userId, dStartDate, dCompleteDate,
				description);
		insert(delegation);

		_doDelegateTask(delegation, false);
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
			if (userId.equals(userId2)) {
				workitem.setUserText2(null);
				workitem.setDeptId2(null);
			} else {
				final PermissionUser user2 = permission.getUser(userId2);
				workitem.setUserText2(user2.getText());
				workitem.setDeptId2(user2.getDept().getId());
			}
			wService.update(new String[] { "status", "userId2", "userText2", "deptId2" }, workitem);
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

	DelegationBean _create(final EDelegationSource delegationSource, final ID sourceId,
			final ID userId, final Date dStartDate, final Date dCompleteDate, final String description) {
		final DelegationBean delegation = createBean();
		delegation.setDelegationSource(delegationSource);
		delegation.setSourceId(sourceId);
		delegation.setUserId(userId);
		delegation.setUserText(permission.getUser(userId).toString());
		delegation.setDstartDate(dStartDate);
		delegation.setDcompleteDate(dCompleteDate);
		delegation.setDescription(description);
		return delegation;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 检测是否过期
		final ITaskExecutor taskExecutor = workflowContext.getTaskExecutor();
		taskExecutor.addScheduledTask(new ExecutorRunnable() {

			@Override
			public int getPeriod() {
				return wfSettings.getDelegatePeriod();
			}

			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				_doTimeoutTask();
			}
		});

		addListener(new DbEntityAdapterEx<DelegationBean>() {
			@Override
			public void onBeforeDelete(final IDbEntityManager<DelegationBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onBeforeDelete(manager, paramsValue);
				for (final DelegationBean delegation : coll(manager, paramsValue)) {
					if (delegation.getStatus().ordinal() < EDelegationStatus.complete.ordinal()) {
						throw WorkflowException.of($m("DelegationService.1"));
					}
				}
			}
		});
	}
}
