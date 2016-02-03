package net.simpleframework.workflow.engine.notice;

import static net.simpleframework.common.I18n.$m;

import java.util.Date;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.ado.trans.TransactionVoidCallback;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.ctx.task.ExecutorRunnableEx;
import net.simpleframework.workflow.WorkflowException;
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
	public WfNoticeBean addWfNotice(String sentId, final ProcessBean process, ID userId,
			final Date dsentDate, final String smessage, final int typeno) {
		return _addWfNotice(sentId, process.getId(), null, userId, dsentDate, smessage, typeno);
	}

	@Override
	public WfNoticeBean addWfNotice(String sentId, final WorkitemBean workitem,
			final Date dsentDate, final String smessage, final int typeno) {
		return _addWfNotice(sentId, workitem.getProcessId(), workitem.getId(), workitem.getUserId2(),
				dsentDate, smessage, typeno);
	}

	WfNoticeBean _addWfNotice(String sentId, final ID processId, final ID workitemId, ID userId,
			final Date dsentDate, final String smessage, final int typeno) {
		if (getWfNoticeTypeHandler(typeno) == null) {
			throw WorkflowException.of($m("WfNoticeService.0"));
		}
		final WfNoticeBean notice = createBean();
		notice.setSentId(sentId);
		notice.setProcessId(processId);
		notice.setTypeNo(typeno);
		notice.setWorkitemId(workitemId);
		notice.setUserId(userId);
		notice.setDsentDate(dsentDate);
		notice.setSmessage(smessage);
		insert(notice);
		return notice;
	}

	@Override
	public WfNoticeBean getNoticeBeanBySentId(String sentId) {
		return getBean("sentid=?", sentId);
	}

	@Override
	public IWfNoticeTypeHandler getWfNoticeTypeHandler(final int no) {
		return AbstractWfNoticeTypeHandler.regists.get(no);
	}

	void _doCheck() {
		final IDataQuery<WfNoticeBean> dq = wfnService.query(
				"(status=? or status=?) and dsentdate<?", ENoticeStatus.ready, ENoticeStatus.fail,
				new Date());
		WfNoticeBean wfNotice;
		while ((wfNotice = dq.next()) != null) {
			final IWfNoticeTypeHandler handler = wfnService.getWfNoticeTypeHandler(wfNotice
					.getTypeNo());
			if (handler == null) {
				continue;
			}
			final WfNoticeBean _wfNotice = wfNotice;
			doExecuteTransaction(new TransactionVoidCallback() {
				@Override
				protected void doTransactionVoidCallback() throws Throwable {
					int sents = _wfNotice.getSents();
					try {
						// 修改状态
						if (handler.doSent(_wfNotice)) {
							_wfNotice.setStatus(ENoticeStatus.sent);
							_wfNotice.setSentDate(new Date());
							_wfNotice.setSents(sents + 1);
							wfnService.update(new String[] { "status", "sentdate", "sents" }, _wfNotice);
						} else {
							_wfNotice.setStatus(ENoticeStatus.unsent);
							wfnService.update(new String[] { "status" }, _wfNotice);
						}
					} catch (final Exception e) {
						_wfNotice.setStatus(ENoticeStatus.fail);
						_wfNotice.setSents(sents + 1);
						wfnService.update(new String[] { "status", "sents" }, _wfNotice);
						getLog().warn(e);
					}
				}
			});
		}
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 通知消息检测
		getTaskExecutor().addScheduledTask(new ExecutorRunnableEx("wfnotice_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				_doCheck();
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
