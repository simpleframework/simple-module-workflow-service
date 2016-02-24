package net.simpleframework.workflow.engine.notice;

import static net.simpleframework.common.I18n.$m;

import java.util.Date;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.ctx.task.ExecutorRunnableEx;
import net.simpleframework.ctx.trans.Transaction;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.notice.WfNoticeBean.ENoticeStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfNoticeService extends AbstractDbBeanService<WfNoticeBean> implements
		IWfNoticeService {
	@Override
	public WfNoticeBean addWfNotice(final String sentKey, final ProcessBean process,
			final ID userId, final Date dsentDate, final String smessage, final int typeno) {
		return _addWfNotice(sentKey, process.getId(), null, userId, dsentDate, smessage, typeno);
	}

	@Override
	public WfNoticeBean addWfNotice(final String sentKey, final WorkitemBean workitem,
			final Date dsentDate, final String smessage, final int typeno) {
		return _addWfNotice(sentKey, workitem.getProcessId(), workitem.getId(),
				workitem.getUserId2(), dsentDate, smessage, typeno);
	}

	WfNoticeBean _addWfNotice(final String sentKey, final ID processId, final ID workitemId,
			final ID userId, final Date dsentDate, final String smessage, final int typeno) {
		if (getWfNoticeTypeHandler(typeno) == null) {
			throw WorkflowException.of($m("WfNoticeService.0"));
		}
		final WfNoticeBean wfNotice = createBean();
		wfNotice.setSentKey(sentKey);
		wfNotice.setProcessId(processId);
		wfNotice.setTypeNo(typeno);
		wfNotice.setWorkitemId(workitemId);
		wfNotice.setUserId(userId);
		wfNotice.setDsentDate(dsentDate);
		wfNotice.setSmessage(smessage);
		insert(wfNotice);
		// 发送任务
		_doWfNoticeTask(wfNotice);
		return wfNotice;
	}

	@Override
	public IDataQuery<WfNoticeBean> queryWfNotices(final String sentKey) {
		return query("sentkey=?", sentKey);
	}

	@Override
	public WfNoticeBean getWfNotice(final ID userId, final String sentKey) {
		return getBean("userid=? and sentkey=?", userId, sentKey);
	}

	@Override
	public IWfNoticeTypeHandler getWfNoticeTypeHandler(final int no) {
		return AbstractWfNoticeTypeHandler.regists.get(no);
	}

	void _doWfNoticeTask(final WfNoticeBean wfNotice) {
		final ENoticeStatus status = wfNotice.getStatus();
		if (status == ENoticeStatus.ready) {
			final Date dsentDate = wfNotice.getDsentDate();
			if (dsentDate == null || dsentDate.before(new Date())) {
				_doSent(wfNotice);
			}
		}
	}

	void _doSent(final WfNoticeBean wfNotice) {
		final IWfNoticeTypeHandler handler = wfnService.getWfNoticeTypeHandler(wfNotice.getTypeNo());
		if (handler == null) {
			return;
		}

		final int sents = wfNotice.getSents();
		try {
			// 修改状态
			if (handler.doSent(wfNotice)) {
				wfNotice.setStatus(ENoticeStatus.sent);
				wfNotice.setSentDate(new Date());
				wfNotice.setSents(sents + 1);
				wfnService.update(new String[] { "status", "sentdate", "sents" }, wfNotice);
			} else {
				wfNotice.setStatus(ENoticeStatus.unsent);
				wfnService.update(new String[] { "status" }, wfNotice);
			}
		} catch (final Exception e) {
			wfNotice.setStatus(ENoticeStatus.fail);
			wfNotice.setSents(sents + 1);
			wfnService.update(new String[] { "status", "sents" }, wfNotice);
			getLog().warn(e);
		}
	}

	@Transaction(context = IWorkflowContext.class)
	public void doWfNotice_inTran(final WfNoticeBean wfNotice) {
		if (wfNotice.getStatus() == ENoticeStatus.ready) {
			_doWfNoticeTask(wfNotice);
		} else {
			_doSent(wfNotice);
		}
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 通知消息检测
		getTaskExecutor().addScheduledTask(new ExecutorRunnableEx("wfnotice_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				final IDataQuery<WfNoticeBean> dq = wfnService.query("(status=? or status=?)",
						ENoticeStatus.ready, ENoticeStatus.fail);
				WfNoticeBean wfNotice;
				while ((wfNotice = dq.next()) != null) {
					doWfNotice_inTran(wfNotice);
				}
			}
		});

		// 流程被删除后执行
		wfpService.addListener(new DbEntityAdapterEx<ProcessBean>() {
			@Override
			public void onAfterDelete(final IDbEntityManager<ProcessBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onAfterDelete(manager, paramsValue);
				for (final ProcessBean process : coll(manager, paramsValue)) {
					deleteWith("processid=?", process.getId());
				}
			}
		});
	}
}
