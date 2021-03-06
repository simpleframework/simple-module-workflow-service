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
import net.simpleframework.ctx.task.ExecutorRunnableEx;
import net.simpleframework.ctx.trans.Transaction;
import net.simpleframework.module.common.log.LogEntity;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IDelegationService;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.bean.DelegationBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DelegationService extends AbstractWorkflowService<DelegationBean>
		implements IDelegationService {

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
			sb.append("select d.*,w.processid from ").append(getTablename(DelegationBean.class))
					.append(" d left join ").append(getTablename(WorkitemBean.class)).append(
							" w on d.sourceid = w.id where w.userId=? and d.delegationsource=? order by d.createdate desc");
			return query(new SQLValue(sb, userId, source));
		} else {
			return query("delegationsource=? and sourceid=? order by createdate desc", source, userId);
		}
	}

	@Override
	public IDataQuery<DelegationBean> queryRevDelegations(final ID userId) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select d.*,w.processid from ").append(getTablename(DelegationBean.class))
				.append(" d left join ").append(getTablename(WorkitemBean.class)).append(
						" w on d.sourceid = w.id where d.userId=? and d.delegationsource=? order by d.createdate desc");
		return query(new SQLValue(sb, userId, EDelegationSource.workitem));
	}

	@Override
	public void doAccept(final DelegationBean delegation, final String description2) {
		_doAccept(delegation, description2, false);
	}

	@Override
	public void doRefuse(final DelegationBean delegation, final String description2) {
		_doAccept(delegation, description2, true);
	}

	private void _doAccept(final DelegationBean delegation, final String description2,
			final boolean refuse) {
		_assert(delegation, EDelegationStatus.receiving);
		if (refuse) {
			_status(delegation, EDelegationStatus.refuse);
			_updateWorkitem(delegation, EWorkitemStatus.running);
		} else {
			_status(delegation, EDelegationStatus.running);
		}
		delegation.setDescription2(description2);
		update(new String[] { "description2" }, delegation);
	}

	@Override
	public void doAbort(final DelegationBean delegation) {
		_assert(delegation, EDelegationStatus.ready, EDelegationStatus.receiving,
				EDelegationStatus.running);
		_abort(delegation);
		_updateWorkitem(delegation, EWorkitemStatus.running);
	}

	void _abort(final DelegationBean delegation) {
		delegation.setStatus(EDelegationStatus.abort);
		delegation.setCompleteDate(new Date());
		update(new String[] { "status", "completeDate" }, delegation);
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

		delegation = _create(EDelegationSource.user, sourceId, sourceId, userId, dStartDate,
				dCompleteDate, description);
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

				LogEntity.disable(delegation);
				update(new String[] { "status", "startDate" }, delegation);

				_updateWorkitem(delegation, EWorkitemStatus.delegate);
			}
		}
	}

	void _updateWorkitem(final DelegationBean delegation, final EWorkitemStatus status) {
		// 更新Workitem
		final WorkitemBean workitem = delegation.getDelegationSource() == EDelegationSource.workitem
				? wfwService.getBean(delegation.getSourceId()) : null;
		if (workitem != null) {
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
			wfwService.update(new String[] { "status", "userId2", "userText2", "deptId2" }, workitem);
		}
	}

	@Transaction(context = IWorkflowContext.class)
	public void doDelegation_inTran(final DelegationBean delegation) {
		// 保证每条数据在一个事务内
		final WorkitemBean workitem = delegation.getDelegationSource() == EDelegationSource.workitem
				? wfwService.getBean(delegation.getSourceId()) : null;
		// 如果任务已经完成，则放弃
		if (workitem != null && wfwService.isFinalStatus(workitem)) {
			_abort(delegation);
		}

		if (delegation.getStatus() == EDelegationStatus.ready) {
			_doDelegateTask(delegation, false);
		} else {
			final Date endDate = delegation.getDcompleteDate();
			if (endDate != null && endDate.before(new Date())) {
				_abort(delegation);
				_updateWorkitem(delegation, EWorkitemStatus.running);

				// 修改标记
				delegation.setTimeoutMark(true);
				update(new String[] { "timeoutMark" }, delegation);
			}
		}
	}

	DelegationBean _create(final EDelegationSource delegationSource, final ID sourceId,
			final ID ouserId, final ID userId, final Date dStartDate, final Date dCompleteDate,
			final String description) {
		final DelegationBean delegation = createBean();
		delegation.setDelegationSource(delegationSource);
		delegation.setSourceId(sourceId);
		delegation.setOuserId(ouserId);
		delegation.setOuserText(permission.getUser(ouserId).getText());
		delegation.setUserId(userId);
		delegation.setUserText(permission.getUser(userId).getText());
		delegation.setDstartDate(dStartDate);
		delegation.setDcompleteDate(dCompleteDate);
		delegation.setDescription(description);
		return delegation;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 检测任务
		getTaskExecutor().addScheduledTask(new ExecutorRunnableEx("delegation_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				final IDataQuery<DelegationBean> dq = query("status=? or status=?",
						EDelegationStatus.ready, EDelegationStatus.running).setFetchSize(0);
				DelegationBean delegation;
				while ((delegation = dq.next()) != null) {
					try {
						doDelegation_inTran(delegation);
					} catch (final Exception ex) {
						getLog().error(ex);
					}
				}
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
